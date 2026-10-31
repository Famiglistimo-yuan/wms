package com.wms.wmsserver.materialio.service;

import com.wms.wmsserver.materialio.dto.CreateOrderDTO;
import com.wms.wmsserver.materialio.entity.StockOrder;

/**
 * 进出仓单据服务：创建入库单 / 出库单、查询单据列表与明细。
 */
public interface StockOrderService {

    /**
     * 创建单据（入库 / 出库 共用）。
     * 事务内：出库先整体校验库存 → 全部通过才批量扣库存 → 写流水 → 写单据主/明细。
     * 任意环节异常整张单据回滚，库存不动，流水不写。
     *
     * @param dto      请求体
     * @param operator 操作人（从登录态取）
     * @return 生成的单据主表（含 orderNo）
     */
    StockOrder createOrder(CreateOrderDTO dto, String operator);

    /** 单据列表（按时间倒序，可按类型筛选） */
    java.util.List<StockOrder> listOrders(Integer orderType);

    /** 单据详情（主表 + 明细列表） */
    java.util.Map<String, Object> getOrderDetail(Long orderId);
}
