package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 进出仓单列表行 VO（子系统2 单据查询），两端共享契约。
 * 操作人/经手人已由服务端 JOIN 人员档案带出姓名，客户端无需二次拉取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private Long id;

    /** 单号：IN/OUT + yyyyMMdd + 4 位流水 */
    private String orderNo;

    /** IN=进仓 OUT=出仓 */
    private String orderType;

    /** 单据日期 yyyy-MM-dd */
    private String orderDate;

    /** 操作人姓名（登录用户关联人员） */
    private String operatorName;

    /** 经手人姓名 */
    private String handlerName;

    /** 明细条数 */
    private Long itemCount;

    /** 明细数量合计 */
    private BigDecimal totalQuantity;

    /** 整单备注 */
    private String remark;
}
