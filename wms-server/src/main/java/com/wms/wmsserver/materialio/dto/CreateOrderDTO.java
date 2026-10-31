package com.wms.wmsserver.materialio.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建入/出库单请求体：
 * {
 *   "orderType": 1,        // 1入库 2出库
 *   "source": "采购",       // 仅入库单需要
 *   "remark": "2026-10-30 补货",
 *   "items": [{"materialId":1,"ioNum":100}, {"materialId":2,"ioNum":50}]
 * }
 */
@Data
public class CreateOrderDTO {

    /** 1入库 2出库 */
    private Integer orderType;

    /** 入库来源（采购/调拨等） 仅入库单有意义 */
    private String source;

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
