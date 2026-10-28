package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仓库账本预览行（FR-5-3 导出前页面预览）：结存为服务端期初反推后逐日累计，
 * 展示串与导出一致（见 LedgerReportService）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerPreviewRow {

    private String orderDate;
    private String inQty;
    private String outQty;
    private String balance;
}
