package com.wms.wmsclient.http;

/**
 * ApiClient 业务异常：承载服务端返回的错误 message（业务码与 HTTP 状态信息已在文案内）。
 * 继承 RuntimeException，调用方按需 catch。
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
