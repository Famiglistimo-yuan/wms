package com.wms.wmsserver.stock.service;

import com.wms.common.PageResult;
import com.wms.common.dto.OrderDetailVO;
import com.wms.common.dto.OrderVO;
import com.wms.common.dto.PersonOptionVO;
import com.wms.wmsserver.stock.dto.OrderCreateDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 进出仓单服务（子系统2：录入 + 单据查询；修改/删除属子系统3）。
 */
public interface StockOrderService {

    /**
     * 提交进出仓单：明细序列化为 JSON 后调存储过程 rg2402_11_12_13_order_create，
     * 事务/乐观锁/校验均在其内；SP 出口码 409xx 映射为错误码 409，message 原样透传客户端。
     *
     * @param dto    请求体（orderType IN/OUT、明细列表）
     * @param userId 当前登录用户 id（JWT subject，操作人取其关联人员）
     * @return 生成的单号
     */
    String create(OrderCreateDTO dto, Long userId);

    /**
     * 单据组合查询（技术方案 §6.2）：dateFrom/dateTo/material/operator/remark 可选，
     * 另支持 orderType；分页 page 从 1 起、size 默认 20 上限 100。
     */
    PageResult<OrderVO> query(LocalDate dateFrom, LocalDate dateTo, String material,
                              String operator, String remark, String orderType,
                              long page, int size);

    /** 单据详情：主表行 + 明细列表（含物料档案）；不存在时抛 400 */
    OrderDetailVO detail(Long orderId);

    /** 经手人下拉选项（人员档案，进出仓面板共用） */
    List<PersonOptionVO> personOptions();
}
