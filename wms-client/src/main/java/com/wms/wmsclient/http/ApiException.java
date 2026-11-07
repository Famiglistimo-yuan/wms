package com.wms.wmsclient.http;

/**
 * ApiClient 业务异常：承载服务端返回的业务码 code 与错误 message
 * （HTTP 恒 200，业务码在 body；网络层异常无业务码，code 为 0）。
 * 继承 RuntimeException，调用方按需 catch。
 */
public class ApiException extends RuntimeException {

    /** 服务端业务码（技术方案 §6.1）；0 表示无业务码（纯网络/HTTP 层错误） */
    private final int code;

    public ApiException(String message) {
        this(0, message);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
