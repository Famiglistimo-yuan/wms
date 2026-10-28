package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度进出仓单预览行（FR-5-2 导出前页面预览，两端共享契约）。
 * 各字段均为展示串，与导出文件的单元格内容一致（类型转「进仓/出仓」、数量 strip），保证所见即所导。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyOrderPreviewRow {

    private String orderNo;
    private String typeLabel;
    private String orderDate;
    private String operatorName;
    private String handlerName;
    private String materialCode;
    private String materialName;
    private String unit;
    private String quantity;
    private String remark;
}
