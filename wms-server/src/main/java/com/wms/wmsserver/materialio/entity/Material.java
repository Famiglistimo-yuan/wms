package com.wms.wmsserver.materialio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 子系统2 —— 简单物料进仓/出仓信息管理
 * 物料基础实体，映射表 {@code material}。
 * <p>
 * 与旧子系统1（{@code com.wms.wmsserver.entity.Material}，表名带前缀）为平行模块，
 * 子系统2 使用干净表名、轻量字段，专注物料库存与出入库记录核心链路。
 */
@Data
@TableName("material")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 单位
     */
    private String unit;

    /**
     * 库位（未设置可空）
     */
    private String location;

    /**
     * 安全库存，低于该值前端标记预警（未设置可空）
     */
    private BigDecimal safeStockNum;

    /**
     * 当前库存数量
     */
    private BigDecimal stockNum;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0未删 1已删
     */
    @TableLogic
    private Integer deleted;
}
