package com.wms.common;

/**
 * HTTP 层错误码（技术方案 §6.1，不得私造新码，见 CONTRIBUTING §6.5）。
 * 存储过程业务出口码 409xx 见 docs/sql/schema.sql SP 契约，Service 层映射为 CONFLICT。
 *
 */
public final class ErrorCode {

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int CONFLICT = 409;
    public static final int SERVER_ERROR = 500;

    private ErrorCode() {
    }
}
