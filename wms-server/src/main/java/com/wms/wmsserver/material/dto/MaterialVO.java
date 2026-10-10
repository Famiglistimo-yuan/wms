package com.wms.wmsserver.material.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialVO {

    private Long id;
    private String materialCode;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal stock;
    private String remark;
    private LocalDateTime createdAt;
}
