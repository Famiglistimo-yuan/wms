package com.wms.wmsserver.reports.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 月度进出仓单数据行（FR-5-2）：一条明细一行，按单号顺序排列。
 */
@Data
public class MonthlyOrderRow {

    private String orderNo;
    private String orderType;
    private LocalDate orderDate;
    private String operatorName;
    private String handlerName;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal quantity;
    private String remark;
}
