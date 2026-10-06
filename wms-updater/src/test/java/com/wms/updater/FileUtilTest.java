package com.wms.updater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件工具测试（FR-6）：MD5 计算的确定性与验货匹配、滚动备份替换的最终状态。
 */
class FileUtilTest {

    @TempDir
    Path dir;

    @Test
    void md5IsDeterministicAndContentSensitive() throws Exception {
        Path a = Files.writeString(dir.resolve("a.txt"), "hello world");
        Path b = Files.writeString(dir.resolve("b.txt"), "hello world");
        Path c = Files.writeString(dir.resolve("c.txt"), "hello world!");
        assertEquals(FileUtil.md5(a), FileUtil.md5(b));            // 同内容同指纹
        assertFalse(FileUtil.md5(a).equals(FileUtil.md5(c)));      // 一字节之差指纹全变
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", FileUtil.md5(a)); // "hello world" 的已知 MD5
    }

    @Test
    void md5MatchesIgnoresCaseAndBlank() throws Exception {
        Path f = Files.writeString(dir.resolve("f.txt"), "hello world");
        assertTrue(FileUtil.md5Matches(f, "5EB63BBBE01EEED093CB22BB8F5ACDC3")); // 大写也匹配
        assertFalse(FileUtil.md5Matches(f, ""));
        assertFalse(FileUtil.md5Matches(f, null));
    }

    @Test
    void replaceRollsBackupAndSwapsFile() throws Exception {
        Path target = Files.writeString(dir.resolve("wms-client.jar"), "OLD");
        Path downloaded = Files.writeString(dir.resolve("new.jar"), "NEW");
        // 预置上次升级留下的同名 .bak，验证滚动清理
        Files.writeString(dir.resolve("wms-client.jar.bak"), "ANCIENT");
        // 无关的第三方 .jar.bak（例如 JavaFX 手动备份）不应被误删
        Path unrelated = Files.writeString(dir.resolve("javafx-controls.jar.bak"), "UNRELATED");

        FileUtil.replaceWithBackup(target, downloaded);

        assertEquals("NEW", Files.readString(target));                              // 新文件就位
        assertEquals("OLD", Files.readString(dir.resolve("wms-client.jar.bak")));    // 旧版已备份
        assertFalse(Files.exists(dir.resolve("new.jar")));                           // 下载件已消费
        assertFalse(Files.exists(dir.resolve("wms-client.jar.new")));                // 中转文件已消费
        assertEquals("UNRELATED", Files.readString(unrelated));                      // 只清同名 .bak，其他不动
    }
}
