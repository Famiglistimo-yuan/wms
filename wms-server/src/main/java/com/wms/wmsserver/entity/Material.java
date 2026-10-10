package com.wms.wmsserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料档案实体（@TableName 直书完整表名）。库存 DECIMAL(12,3) 非负约束在 schema CHECK + 应用层双重兜底。
 */
@NoArgsConstructor
@TableName("rg2402_11_12_13_material")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 物料代码（FR-1-3-2 非空唯一，可自动生成或手输） */
    private String materialCode;
    private String name;
    private String spec;
    /** 计量单位（FR-1-3-3 可选+可输入，预设 件/套/公斤/吨/升/米/毫米/个） */
    private String unit;
    /** 库存（FR-1-3-4 默认 0，schema CHECK stock >= 0） */
    private BigDecimal stock;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
