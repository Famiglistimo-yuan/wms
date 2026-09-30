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
import java.util.stream.Stream;

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
     * 替换三部曲（目标文件此刻未被占用——主程序已退出）：
     * ① 清掉同目录旧 *.jar.bak（滚动保留：任何时刻只留上一个版本）
     * ② 现 jar 改名为 .bak（回滚 = 手动改回原名）
     * ③ 新文件原子 move 就位（同分区原子，不会出现半替换状态）
     */
    static void replaceWithBackup(Path target, Path downloaded) throws IOException {
        Path dir = target.getParent();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path old : files.filter(f -> f.getFileName().toString().endsWith(".jar.bak")).toList()) {
                Files.deleteIfExists(old);
            }
        }
        Files.move(target, dir.resolve(target.getFileName() + ".bak"));
        Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
