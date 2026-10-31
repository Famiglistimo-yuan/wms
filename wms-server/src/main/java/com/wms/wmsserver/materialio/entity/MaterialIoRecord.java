package com.wms.wmsserver.materialio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 子系统2 —— 物料出入库流水记录，映射表 {@code material_io_record}。
 * <p>
 * 每次入库或出库操作都会写一条流水，记录操作前后的库存快照，
 * 方便后续追溯与库存核对。
 */
@Data
@TableName("material_io_record")
public class MaterialIoRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 物料ID，关联material表
     */
    private Long materialId;

    /**
     * 出入库类型：1入库，2出库
     */
    private Integer ioType;

    /**
     * 出入库数量
     */
    private BigDecimal ioNum;

    /**
     * 操作前库存
     */
    private BigDecimal beforeStock;

    /**
     * 操作后库存
     */
    private BigDecimal afterStock;

    /**
     * 备注
     */
    private String remark;

    /** 操作人 */
    private String operator;

    /** 关联单据号（新单据化流程写入，历史数据可空） */
    private String orderNo;

    /** 单据类型 1入库 2出库 */
    private Integer orderType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 逻辑删除 0未删 1已删
     */
    @TableLogic
    private Integer deleted;
}
