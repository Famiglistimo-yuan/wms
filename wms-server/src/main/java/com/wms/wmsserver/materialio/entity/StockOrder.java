package com.wms.wmsserver.materialio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进出仓单据主表：入库单 / 出库单 共用，order_type 区分。
 */
@Data
@TableName("stock_order")
public class StockOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单据号 自动生成 如 RK20261030001 / CK20261030001 */
    private String orderNo;

    /** 1入库 2出库 */
    private Integer orderType;

    /** 入库来源(采购/调拨等) / 出库去向  共用一个字段，orderType 区分语义 */
    private String sourceTarget;

    /** 经手人（业务字段，区别于 operator 操作员） */
    private String handler;

    /** 操作人 后端从登录态取 */
    private String operator;

    /** 整单备注 */
    private String remark;

    /** 明细条数 */
    private Integer totalCount;

    /** 明细数量合计 */
    private BigDecimal totalNum;

    private LocalDateTime createTime;
}
