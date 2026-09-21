package com.wms.common;

import lombok.Getter;

/**
 * 业务异常：Service 层抛出，由服务端全局异常处理器统一转 Result（CONTRIBUTING §4.2）。
 *
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
