package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物料下拉选项（FR-5 账本面板选物料用；不与 FR-1 的 /api/materials 重复）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialOption {

    private String materialCode;
    private String name;
}
