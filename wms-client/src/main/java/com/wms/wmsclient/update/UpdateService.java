package com.wms.wmsclient.update;

import com.wms.common.ErrorCode;
import com.wms.common.Result;
import com.wms.common.VersionInfo;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.AppPaths;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 自动升级（FR-6，课题方法二）：启动后台检查 + 菜单手动检查。
 * 降级原则：检查链路任何失败（网络/解析/版本号非法/dev 无 app-image）静默跳过，绝不挡登录；
 * 仅在确有新版本且用户同意时才退出主程序、拉起 Updater（下载/校验/替换由其完成）。
 */
public final class UpdateService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private UpdateService() {
    }

    /**
     * 异步检查新版本（后台线程发请求，弹窗切回 FX 线程——CONTRIBUTING §3.4 第 1 条）。
     *
     * @param manual true = 菜单手动触发，「已是最新/检查失败」也给用户反馈；false = 启动自动检查，静默降级
     */
    public static void checkAsync(boolean manual) {
        new Thread(() -> {
            VersionInfo remote = fetchRemote();
            String local = localVersion();
            if (remote == null || local == null || !isNewer(remote.getVersion(), local)) {
                if (manual) {
                    String msg = remote == null || local == null
                            ? "升级检查未通过（服务端不可达、发布配置不完整或本地版本缺失），已跳过"
                            : "当前已是最新版本";
                    Platform.runLater(() -> info(msg));
                }
                return;
            }
            Platform.runLater(() -> askAndUpgrade(remote));
        }, "version-check").start();
    }

    /** 拉起 Updater 并退出：JavaFX 线程上调用（弹窗确认后） */
    private static void askAndUpgrade(VersionInfo remote) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "发现新版本 " + remote.getVersion() + "，是否升级？（升级将退出当前程序）",
                new ButtonType("升级", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || !choice.get().getButtonData().isDefaultButton()) {
            return;
        }
        launchUpdater(remote);
    }

    /** 启动 Updater（复用 app-image 自带 runtime 的 java），成功后主程序退出解锁主 jar */
    static void launchUpdater(VersionInfo remote) {
        Path appRoot = AppPaths.appRoot();
        if (appRoot == null) {
            info("开发模式（未打包运行）不支持升级，请以打包产物运行");
            return;
        }
        try {
            Path appDir = appRoot.resolve("app");
            String javaExe = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "runtime/bin/java.exe" : "runtime/bin/java";
            String launcher = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "WMS.exe" : "MacOS/WMS";

            new ProcessBuilder(
                    appRoot.resolve(javaExe).toString(),
                    "-jar", appDir.resolve("updater.jar").toString(),
                    "--url=" + ApiClient.BASE_URL + remote.getDownloadUrl(),
                    "--md5=" + remote.getMd5(),
                    "--target=" + appDir.resolve("wms-client.jar"),
                    "--pid=" + ProcessHandle.current().pid(),
                    "--launch=" + appRoot.resolve(launcher))
                    .start();
            Platform.exit();
            // Platform.exit() 只关 JavaFX；若存在其他非 daemon 用户线程，JVM 会残留导致
            // Updater 的 ProcessHandle.of(pid) 一直看到「主程序还活着」而 30s 超时中止升级。
            // 显式 exit(0) 确保进程消失，让 Updater 立刻进入下载阶段。
            System.exit(0);
        } catch (IOException e) {
            info("启动升级程序失败：" + e.getMessage());
        }
    }

    /** 读本地版本：打包时经 resources filtering 烧进 jar 的 version.txt（FR-6 版本单一来源） */
    static String localVersion() {
        try (InputStream in = UpdateService.class.getResourceAsStream("/version.txt")) {
            return in == null ? null : new String(in.readAllBytes()).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /** 查询服务端最新版本；任何失败或发布配置不完整（version/md5 空白）返回 null，调用方静默降级 */
    static VersionInfo fetchRemote() {
        try {
            String body = ApiClient.get("/api/version");
            // 泛型包装需显式构造参数化类型，否则 data 反序列化为 LinkedHashMap
            Result<VersionInfo> result = JSON.readValue(body,
                    JSON.getTypeFactory().constructParametricType(Result.class, VersionInfo.class));
            if (result.getCode() != ErrorCode.SUCCESS) {
                return null;
            }
            VersionInfo info = result.getData();
            // 服务端 application.yaml 默认 md5=""，若发版时忘了填就下发，会让客户端下载完 6MB
            // 主 jar 才在 MD5 校验时弹错。这里提前短路，视为「无有效发布」。
            if (info == null
                    || info.getVersion() == null || info.getVersion().isBlank()
                    || info.getMd5() == null || info.getMd5().isBlank()) {
                return null;
            }
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 三段数字版本比较：remote 是否比 local 新（1.0.10 > 1.0.9）。
     * 任一格式非法（如 0.1.0-SNAPSHOT）返回 false——解析不了就不升级。
     */
    static boolean isNewer(String remote, String local) {
        int[] a = parse(remote), b = parse(local);
        if (a == null || b == null) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i];
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        if (version == null) {
            return null;
        }
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new int[]{
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void info(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}
