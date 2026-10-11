package com.wms.wmsserver.reports.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 仓库账本数据行（FR-5-3）：某物料某年按日期聚合的进/出数量。
 * 结存由 Service 期初反推后逐日累计（见 LedgerReportService）。
 */
@Data
public class LedgerRow {

    private LocalDate orderDate;
    private BigDecimal inQty;
    private BigDecimal outQty;
}
