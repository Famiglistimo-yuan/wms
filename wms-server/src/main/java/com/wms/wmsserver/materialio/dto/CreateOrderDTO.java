package com.wms.wmsserver.materialio.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建入/出库单请求体：
 * {
 *   "orderType": 1,            // 1入库 2出库
 *   "sourceTarget": "采购",     // 入库填来源 / 出库填去向
 *   "handler": "张三",          // 经手人（必填）
 *   "remark": "2026-10-30 补货",
 *   "items": [{"materialId":1,"ioNum":100}, {"materialId":2,"ioNum":50}]
 * }
 */
@Data
public class CreateOrderDTO {

    /** 1入库 2出库 */
    private Integer orderType;

    /** 入库来源 / 出库去向  共用字段，orderType 区分语义 */
    private String sourceTarget;

    /** 经手人（业务必填） */
    private String handler;

    /** 整单备注 */
    private String remark;

    /** 明细 */
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long materialId;
        private BigDecimal ioNum;
    }
}
