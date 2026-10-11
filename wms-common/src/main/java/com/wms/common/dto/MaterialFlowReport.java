package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 物料流量统计报告（FR-5-1）：明细列表 + 服务端分析的流动量最小物料。
 * items 按流动量（进+出）降序；minFlowMaterial 即分析结论（无物料时为 null）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialFlowReport {

    private List<MaterialFlowStat> items;

    /** 流动量最小的物料（含零流动，提示呆滞物料） */
    private MaterialFlowStat minFlowMaterial;
}
