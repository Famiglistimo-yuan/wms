package com.wms.common;

/**
 * 接口业务错误码（技术方案 §6.1，不得私造新码，见 CONTRIBUTING §6.5）。
 * 存储过程业务出口码 409xx 见 docs/sql/schema.sql SP 契约，Service 层映射为 CONFLICT。
 */
public final class ErrorCode {

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int CONFLICT = 409;
    /** 登录口令解密失败（服务端已换钥；客户端应刷新公钥后自动重试，ADR-011） */
    public static final int DECRYPT_FAILED = 422;
    public static final int SERVER_ERROR = 500;

    private ErrorCode() {
    }
}
