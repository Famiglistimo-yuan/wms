package com.wms.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一响应工厂测试：code/message/data 语义按技术方案 §6.1（HTTP 恒 200，业务码在 body）。
 */
class ResultTest {

    @Test
    void successWithoutData() {
        Result<Void> r = Result.success();
        assertEquals(ErrorCode.SUCCESS, r.getCode());
        assertEquals("success", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void successCarriesData() {
        Result<String> r = Result.success("payload");
        assertEquals(ErrorCode.SUCCESS, r.getCode());
        assertEquals("payload", r.getData());
    }

    @Test
    void failCarriesCodeAndMessageWithoutData() {
        Result<Void> r = Result.fail(ErrorCode.CONFLICT, "库存不足");
        assertEquals(409, r.getCode());
        assertEquals("库存不足", r.getMessage());
        assertNull(r.getData());
    }
}
