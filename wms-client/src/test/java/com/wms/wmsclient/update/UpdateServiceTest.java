package com.wms.wmsclient.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 版本比较边界测试（FR-6）：三段数字语义 + 非法格式安全降级。
 */
class UpdateServiceTest {

    @Test
    void newerVersionDetected() {
        assertTrue(UpdateService.isNewer("1.0.1", "1.0.0"));
        assertTrue(UpdateService.isNewer("1.1.0", "1.0.9"));
        assertTrue(UpdateService.isNewer("2.0.0", "1.9.9"));
    }

    @Test
    void numericComparisonNotLexicographic() {
        // 字符串比较会得出 1.0.10 < 1.0.9（'1'<'9'）——数字语义必须翻对
        assertTrue(UpdateService.isNewer("1.0.10", "1.0.9"));
    }

    @Test
    void sameOrOlderIsNotNewer() {
        assertFalse(UpdateService.isNewer("1.0.0", "1.0.0"));
        assertFalse(UpdateService.isNewer("1.0.0", "1.0.1"));
        assertFalse(UpdateService.isNewer("1.0.9", "1.0.10"));
    }

    @Test
    void illegalFormatNeverUpgrades() {
        // 开发期 0.1.0-SNAPSHOT 非三段纯数字 → 解析失败一律不升级（含 null 安全）
        assertFalse(UpdateService.isNewer("1.0.1", "0.1.0-SNAPSHOT"));
        assertFalse(UpdateService.isNewer("0.1.0-SNAPSHOT", "0.1.0"));
        assertFalse(UpdateService.isNewer("1.0", "0.9.9"));
        assertFalse(UpdateService.isNewer(null, "1.0.0"));
        assertFalse(UpdateService.isNewer("1.0.1", null));
    }
}
