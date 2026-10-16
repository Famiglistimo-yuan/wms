package com.wms.wmsserver.material.dto;

import lombok.Data;

/**
 * 修改物料请求：null 字段表示不改。物料代码不允许改。
 * stock 不在此 DTO 中——库存变更仅经进出仓存储过程（FR-1-3-4）。
 */
@Data
public class MaterialUpdateDTO {

    private String name;
    private String spec;
    private String unit;
    private String remark;
}
