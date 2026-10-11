package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 物料流量统计行（FR-5-1，两端共享契约，CONTRIBUTING §4.4）。
 * inQty/outQty 为指定时间段内的进/出仓数量总和；totalFlow = 进 + 出（流动量）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialFlowStat {

    private String materialCode;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal inQty;
    private BigDecimal outQty;
    private BigDecimal totalFlow;
}
