package com.wms.wmsclient.http;

/**
 * ApiClient 业务异常：承载服务端返回的 Result.code + message。
 * 继承 RuntimeException，调用方按需 catch。
 */
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 401 → 未登录/令牌过期 */
    public boolean isUnauthorized() {
        return code == 401;
    }
}
