package com.wms.updater;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FR-6 升级执行器（课题方法二）：主程序在退出前以 app-image 自带 runtime 的 java 启动本程序，
 * 传入 --url/--md5/--target/--pid/--launch 五参数（见 {@link Params}）。
 * 流程：等主程序退出 → 下载 → MD5 校验 → 备份替换 → 重启主程序 → 退出。
 * 任何一步失败都保持旧文件原样（宁可不升级，不升级坏文件）。
 */
public class Updater {

    public static void main(String[] args) {
        Params p;
        try {
            p = Params.parse(args);
        } catch (IllegalArgumentException e) {
            SwingProgress.errorExit("升级参数无效：" + e.getMessage());
            return;
        }
        // progress/downloaded 外提到 try 外：errorExit 内部 System.exit 不展开栈，
        // finally 不可靠，所有出口（成功/校验失败/异常）显式清理
        javax.swing.JDialog progress = SwingProgress.showProgress("正在升级，请稍候…");
        Path downloaded = null;
        try {
            waitForMainExit(p.pid());
            downloaded = download(p.url());
            if (!FileUtil.md5Matches(downloaded, p.md5())) {
                SwingProgress.hideProgress(progress);
                SwingProgress.errorExit("下载文件校验失败，已放弃升级（原程序未受影响）");
                return;
            }
            FileUtil.replaceWithBackup(p.target(), downloaded);
            downloaded = null;   // 已 move 进 target，标记已消费
            new ProcessBuilder(p.launch().toString()).start();
            SwingProgress.hideProgress(progress);
            System.exit(0);
        } catch (Exception e) {
            if (downloaded != null) {
                try {
                    Files.deleteIfExists(downloaded);   // 未消费的下载残留，及时清理
                } catch (java.io.IOException ignored) {
                    // 清理失败无害，temp 目录最终由 OS 回收
                }
            }
            SwingProgress.hideProgress(progress);
            SwingProgress.errorExit("升级失败，已放弃：" + e.getMessage());
        }
    }

    /** 轮询等待主程序进程消失（每 300ms 一次，30 秒超时则中止——主程序卡死时不动文件） */
    private static void waitForMainExit(long pid) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (ProcessHandle.of(pid).isEmpty()) {
                return;
            }
            Thread.sleep(300);
        }
        SwingProgress.errorExit("等待主程序退出超时，升级中止");
    }

    /**
     * 下载升级包到系统临时目录（不落在目标位置——验货前不碰现有文件）。
     * 显式设置连接与请求超时：默认无超时时，服务端半连接会让 Updater 静默挂死，
     * 用户看到的是「主程序已退出、新的没起来、也没弹窗」。
     * 非 200（如升级包未发布时的 404）直接抛错——否则错误页会被写进文件，浪费到 MD5 校验才失败。
     */
    private static Path download(String url) throws Exception {
        Path temp = Files.createTempFile("wms-update-", ".jar");
        java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(java.time.Duration.ofMinutes(5))
                .GET()
                .build();
        java.net.http.HttpResponse<Path> response = http.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofFile(temp));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(temp);
            throw new java.io.IOException("服务端返回 HTTP " + response.statusCode());
        }
        return response.body();
    }
}
