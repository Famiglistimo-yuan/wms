package com.wms.updater;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;

/**
 * 文件工具（FR-6）：MD5 验货 + 带滚动备份的原子替换。
 */
public final class FileUtil {

    private FileUtil() {
    }

    /** 计算文件 MD5（流式，不整读进内存），十六进制小写 */
    static String md5(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (InputStream in = new DigestInputStream(
                new BufferedInputStream(Files.newInputStream(file)), digest)) {
            byte[] buf = new byte[8192];
            while (in.read(buf) != -1) {
                // 边读边喂摘要器
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** 下载文件指纹与服务器宣称值比对（忽略大小写） */
    static boolean md5Matches(Path file, String expected) throws Exception {
        return expected != null && !expected.isBlank() && md5(file).equalsIgnoreCase(expected.trim());
    }

    /**
     * 替换四部曲（目标文件此刻未被占用——主程序已退出）：
     * ① 清掉同目录 target.bak（滚动保留：任何时刻只留上一个版本；只清同名备份，
     *    不通配 *.jar.bak——app/ 平铺的其他依赖 jar 若也有 .bak 不应被误删）
     * ② downloaded 先落地为 target.new（与 target 同目录，若下载在系统 temp 跨卷，
     *    此步会退化为 copy+delete，但 target 仍在原位，失败不影响主程序启动）
     * ③ target → target.bak（同目录改名，原子）
     * ④ target.new → target（同目录 ATOMIC_MOVE，绝不会出现「无主 jar」的中间态）
     * ③/④ 抛异常时把 target.bak 还原回 target 做补偿，宁可升级失败也不留破损现场。
     */
    static void replaceWithBackup(Path target, Path downloaded) throws IOException {
        Path dir = target.getParent();
        Path backup = dir.resolve(target.getFileName() + ".bak");
        Path staging = dir.resolve(target.getFileName() + ".new");

        Files.deleteIfExists(backup);
        Files.deleteIfExists(staging);   // 上一次异常残留
        try {
            Files.move(downloaded, staging);   // 跨卷时退化为 copy+delete，target 未动
            Files.move(target, backup);        // 同目录改名，原子
            try {
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                // 极少数文件系统不支持 ATOMIC_MOVE，退回 REPLACE_EXISTING（同目录仍是 rename 语义）
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // 补偿：只要 target 缺失且 backup 存在，就把旧版还原，保证主程序还能启动
            try {
                if (!Files.exists(target) && Files.exists(backup)) {
                    Files.move(backup, target);
                }
                Files.deleteIfExists(staging);
            } catch (IOException ignored) {
                // 补偿失败也无路可退，把原始异常抛给上层弹窗
            }
            throw e;
        }
    }
}
