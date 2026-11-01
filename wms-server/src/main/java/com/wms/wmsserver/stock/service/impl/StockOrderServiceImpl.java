package com.wms.wmsserver.stock.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.PageResult;
import com.wms.common.dto.OrderDetailVO;
import com.wms.common.dto.OrderItemVO;
import com.wms.common.dto.OrderVO;
import com.wms.common.dto.PersonOptionVO;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.StockOrderMapper;
import com.wms.wmsserver.mapper.UserMapper;
import com.wms.wmsserver.stock.dto.OrderCreateDTO;
import com.wms.wmsserver.stock.service.StockOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进出仓单实现（FR-2 / FR-3）。
 * <p>
 * 写路径硬性要求走存储过程（docs/sql/schema.sql SP 契约节）：校验、条件更新扣库存、
 * 单号流水（行锁自增）与事务提交/回滚均在 SP 内，锁与事务不分离；应用层不做任何
 * 库存读写。SP 出口码 409xx 统一映射为错误码 409（HTTP 恒 200），message 原样透传。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockOrderServiceImpl implements StockOrderService {

    /** 分页上限（技术方案 §6.3） */
    private static final int MAX_PAGE_SIZE = 100;

    private final StockOrderMapper stockOrderMapper;
    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String create(OrderCreateDTO dto, Long userId) {
        // 操作人取当前登录用户关联人员（stock_order.operator_id → person.id）
        User user = userMapper.selectById(userId);
        if (user == null || user.getPersonId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "操作人未关联人员档案");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("orderType", dto.getOrderType());
        params.put("orderDate", LocalDate.parse(dto.getOrderDate()));
        params.put("operatorId", user.getPersonId());
        params.put("handlerId", dto.getHandlerId());
        params.put("remark", dto.getRemark());
        params.put("items", objectMapper.writeValueAsString(dto.getItems()));
        // OUT 位占位（CALLABLE 执行后写回本 Map）
        params.put("orderNo", null);
        params.put("code", null);
        params.put("message", null);

        stockOrderMapper.callOrderCreate(params);

        int code = params.get("code") instanceof Number n ? n.intValue() : -1;
        String message = String.valueOf(params.get("message"));
        if (code != 0) {
            // 409xx（物料不存在/库存不足/人员不存在/参数无效）→ 业务冲突 409，message 透传弹窗
            log.warn("进出仓单提交被 SP 驳回：code={} message={}", code, message);
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
        String orderNo = (String) params.get("orderNo");
        log.info("进出仓单 {} 创建成功：{}，{} 条明细", dto.getOrderType(), orderNo, dto.getItems().size());
        return orderNo;
    }

    @Override
    public PageResult<OrderVO> query(LocalDate dateFrom, LocalDate dateTo, String material,
                                     String operator, String remark, String orderType,
                                     long page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        long offset = (page - 1) * size;

        long total = stockOrderMapper.countOrders(dateFrom, dateTo, material, operator, remark, orderType);
        List<OrderVO> records = List.of();
        if (offset < total) { // 页越界（数据刚被删）返回空页而非报错
            records = stockOrderMapper.selectOrdersPage(dateFrom, dateTo, material, operator,
                    remark, orderType, offset, size);
        }
        return new PageResult<>(records, total, page, size);
    }

    @Override
    public OrderDetailVO detail(Long orderId) {
        OrderVO order = stockOrderMapper.selectOrder(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单据不存在");
        }
        List<OrderItemVO> items = stockOrderMapper.selectOrderItems(orderId);
        return OrderDetailVO.builder().order(order).items(items).build();
    }

    @Override
    public List<PersonOptionVO> personOptions() {
        return personMapper.selectList(Wrappers.<Person>lambdaQuery()
                        .orderByAsc(Person::getPersonCode))
                .stream()
                .map(p -> PersonOptionVO.builder()
                        .id(p.getId())
                        .personCode(p.getPersonCode())
                        .realName(p.getName())
                        .build())
                .toList();
    }
}
