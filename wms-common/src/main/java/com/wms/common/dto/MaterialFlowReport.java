package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 物料流量统计报告（FR-5-1）：明细列表 + 服务端分析的流动量最小物料。
 * items 按流动量（进+出）降序；minFlowMaterials 为并列最小的完整集合（无物料时为空列表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialFlowReport {

    private List<MaterialFlowStat> items;

    /** 流动量最小的物料集合（含并列——多个零流动物料都算呆滞，不能只报一个） */
    private List<MaterialFlowStat> minFlowMaterials;
}
