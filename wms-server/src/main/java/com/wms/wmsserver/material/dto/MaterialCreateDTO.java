package com.wms.wmsserver.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增物料请求。FR-1-3-2 物料代码非空唯一；
 * FR-1-3-3 单位可选+可输入（非空即可，不校验枚举）；FR-1-3-4 库存默认 0。
 */
@Data
public class MaterialCreateDTO {

    @NotBlank(message = "物料代码不能为空")
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    private String name;

    private String spec;

    @NotBlank(message = "计量单位不能为空")
    private String unit;

    /** 默认 0；null 时 Service 层兜底 */
    private BigDecimal stock;

    private String remark;
}
