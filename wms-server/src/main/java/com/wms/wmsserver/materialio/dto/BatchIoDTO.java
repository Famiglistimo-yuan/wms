package com.wms.wmsserver.materialio.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量进仓 / 出仓请求 DTO。
 * <p>
 * 用法：
 * <pre>
 * POST /api/material/in/batch
 * {
 *   "items": [
 *     {"materialId": 1, "ioNum": 100},
 *     {"materialId": 2, "ioNum": 50}
 *   ],
 *   "remark": "2026-10-19 补货"
 * }
 * </pre>
 * <p>
 * 后端对整批操作开单事务：任意一条物料失败，整批回滚，保证原子性。
 */
@Data
public class BatchIoDTO {

    /** 本批要操作的物料条目列表（至少 1 条） */
    private List<Item> items;

    /** 整单备注（可为空，会原样写入每条流水的 remark 字段） */
    private String remark;

    @Data
    public static class Item {
        /** 物料表主键 */
        private Long materialId;
        /** 进/出数量（正数） */
        private BigDecimal ioNum;
    }
}
