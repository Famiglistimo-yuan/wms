package com.wms.wmsserver.material.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改物料请求：null 字段表示不改。物料代码不允许改。
 */
@Data
public class MaterialUpdateDTO {

    private String name;
    private String spec;
    private String unit;

    /** 改库存时服务端校验非负 */
    private BigDecimal stock;

    private String remark;
}
