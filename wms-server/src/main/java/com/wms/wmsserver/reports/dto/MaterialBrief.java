package com.wms.wmsserver.reports.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料摘要（FR-5-3 账本报表头用）：物料代码、名称、规格、计量单位（PPT 要求打印）+ 当前库存（期初反推用）。
 */
@Data
public class MaterialBrief {

    private Long id;
    private String materialCode;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal stock;
}
