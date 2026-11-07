package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 进出仓单明细行 VO（单据详情弹窗用），两端共享契约。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemVO {

    private String materialCode;

    private String materialName;

    private String spec;

    private String unit;

    /** 本次进/出数量 */
    private BigDecimal quantity;
}
