package com.wms.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验正则边界测试：客户端预校验与服务端复用同一份常量，
 * 两端行为由本测试共同保证（FR-4-5 / FR-1-2-5）。
 */
class RegexPatternsTest {

    // ---------- USERNAME：字母开头，4-30 位字母/数字/下划线 ----------

    @Test
    void usernameAcceptsSeedAccount() {
        assertTrue("admin".matches(RegexPatterns.USERNAME));
    }

    @Test
    void usernameAcceptsLengthBounds() {
        assertTrue("a123".matches(RegexPatterns.USERNAME));               // 4 位下界
        assertTrue(("a" + "b".repeat(29)).matches(RegexPatterns.USERNAME)); // 30 位上界
    }

    @Test
    void usernameRejectsInvalidForms() {
        assertFalse("abc".matches(RegexPatterns.USERNAME));                // 3 位太短
        assertFalse("1abcd".matches(RegexPatterns.USERNAME));              // 数字开头
        assertFalse("ab_cd-e".matches(RegexPatterns.USERNAME));            // 非法字符
        assertFalse(("a" + "b".repeat(30)).matches(RegexPatterns.USERNAME)); // 31 位超限
        assertFalse("".matches(RegexPatterns.USERNAME));                   // 空串
    }

    // ---------- PASSWORD：6-20 位字母/数字（不含下划线） ----------

    @Test
    void passwordAcceptsSeedPasswordAndBounds() {
        assertTrue("admin123".matches(RegexPatterns.PASSWORD));      // 种子账号明文口令
        assertTrue("abcdef".matches(RegexPatterns.PASSWORD));        // 6 位下界
        assertTrue("a".repeat(20).matches(RegexPatterns.PASSWORD));  // 20 位上界
    }

    @Test
    void passwordRejectsInvalidForms() {
        assertFalse("admin".matches(RegexPatterns.PASSWORD));           // 5 位太短
        assertFalse("a".repeat(21).matches(RegexPatterns.PASSWORD));    // 21 位超限
        assertFalse("abc_12".matches(RegexPatterns.PASSWORD));          // 下划线不收
    }

    // ---------- ID_CARD：17 位数字 + 数字/X/x ----------

    @Test
    void idCardAcceptsDigitAndXxTail() {
        assertTrue(("1".repeat(17) + "9").matches(RegexPatterns.ID_CARD)); // 末位数字
        assertTrue(("1".repeat(17) + "X").matches(RegexPatterns.ID_CARD)); // 末位大写 X
        assertTrue(("1".repeat(17) + "x").matches(RegexPatterns.ID_CARD)); // 末位小写 x（技术方案 §7.1）
    }

    @Test
    void idCardRejectsInvalidForms() {
        assertFalse("1".repeat(17).matches(RegexPatterns.ID_CARD));         // 少末位
        assertFalse("1".repeat(19).matches(RegexPatterns.ID_CARD));         // 超长
        assertFalse(("1".repeat(17) + "A").matches(RegexPatterns.ID_CARD)); // 末位非法字母
    }
}
