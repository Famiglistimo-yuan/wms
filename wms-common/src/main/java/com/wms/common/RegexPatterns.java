package com.wms.common;

/**
 * 校验正则常量（客户端与服务端双重校验用同一份，技术方案 §7.1）。
 * 用户名/口令规则为应用层口径（数据契约评审第 6 条定稿建议）。
 */
public final class RegexPatterns {

    /** 登录用户名：字母开头，字母/数字/下划线，4-30 字符 */
    public static final String USERNAME = "^[a-zA-Z][a-zA-Z0-9_]{3,29}$";

    /** 登录口令：字母/数字，6-20 字符 */
    public static final String PASSWORD = "^[a-zA-Z0-9]{6,20}$";

    /** 身份证号：17 位数字 + 数字或 X（FR-1-2-5 课题硬性要求） */
    public static final String ID_CARD = "^[0-9]{17}[0-9Xx]$";

    private RegexPatterns() {
    }
}
