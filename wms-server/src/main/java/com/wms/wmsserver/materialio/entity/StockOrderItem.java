package com.wms.wmsserver.materialio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单据明细：每种物料一行。
 */
@Data
@TableName("stock_order_item")
public class StockOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 stock_order.id */
    private Long orderId;

    /** 关联 material.id */
    private Long materialId;

    /** 本次入库/出库数量 */
    private BigDecimal ioNum;

    /** 操作前库存快照 */
    private BigDecimal beforeStock;

    /** 操作后库存快照 */
    private BigDecimal afterStock;
}
