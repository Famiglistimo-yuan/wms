package com.wms.wmsserver.materialio.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.wmsserver.materialio.dto.CreateOrderDTO;
import com.wms.wmsserver.materialio.entity.*;
import com.wms.wmsserver.materialio.mapper.*;
import com.wms.wmsserver.materialio.service.StockOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 进出仓单据服务：入库 / 出库 共用，整单一个 @Transactional。
 * 流程：校验 → 出库整体校验库存 → 生成单据号 → 插主表 → 逐条改库存+写流水+插明细 → 全部成功提交。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockOrderServiceImpl implements StockOrderService {

    private final MaterialIoMapper materialMapper;
    private final MaterialIoRecordMapper materialIoRecordMapper;
    private final StockOrderMapper stockOrderMapper;
    private final StockOrderItemMapper stockOrderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockOrder createOrder(CreateOrderDTO dto, String operator) {
        // 1. 参数校验
        if (dto.getOrderType() == null || (dto.getOrderType() != 1 && dto.getOrderType() != 2)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "orderType 必须为 1(入库) 或 2(出库)");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单据条目不能为空");
        }

        // 物料存在性 + 数量合法性预校验
        List<Material> materials = new ArrayList<>();
        for (CreateOrderDTO.OrderItemDTO item : dto.getItems()) {
            if (item.getMaterialId() == null || item.getIoNum() == null
                    || item.getIoNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "存在非法条目");
            }
            Material m = materialMapper.selectById(item.getMaterialId());
            if (m == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "物料不存在(id=" + item.getMaterialId() + ")");
            }
            materials.add(m);
        }

        // 2. 出库整体校验库存：任意一条不足则整单驳回
        boolean isOutbound = dto.getOrderType() == 2;
        if (isOutbound) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dto.getItems().size(); i++) {
                Material m = materials.get(i);
                BigDecimal ioNum = dto.getItems().get(i).getIoNum();
                if (m.getStockNum().compareTo(ioNum) < 0) {
                    sb.append("物料[").append(m.getMaterialCode()).append(' ')
                            .append(m.getMaterialName()).append("] 当前库存 ")
                            .append(m.getStockNum().stripTrailingZeros().toPlainString())
                            .append(" 不足申请出库 ")
                            .append(ioNum.stripTrailingZeros().toPlainString())
                            .append("；\n");
                }
            }
            if (!sb.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "库存校验不通过：\n" + sb);
            }
        }

        // 3. 生成单据号 RK/CK + yyyyMMdd + 3位当日序号
        String prefix = isOutbound ? "CK" : "RK";
        String orderNo = genOrderNo(prefix, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        // 4. 先插单据主表（拿到 orderId，否则明细外键会报错）
        BigDecimal totalNum = BigDecimal.ZERO;
        for (CreateOrderDTO.OrderItemDTO item : dto.getItems()) totalNum = totalNum.add(item.getIoNum());
        StockOrder order = new StockOrder();
        order.setOrderNo(orderNo);
        order.setOrderType(dto.getOrderType());
        order.setSource(dto.getSource());
        order.setOperator(operator);
        order.setRemark(dto.getRemark());
        order.setTotalCount(dto.getItems().size());
        order.setTotalNum(totalNum);
        stockOrderMapper.insert(order);
        Long orderId = order.getId();

        // 5. 逐条改库存 + 写流水 + 插明细
        for (int i = 0; i < dto.getItems().size(); i++) {
            CreateOrderDTO.OrderItemDTO item = dto.getItems().get(i);
            Material m = materials.get(i);
            BigDecimal ioNum = item.getIoNum();
            BigDecimal beforeStock = m.getStockNum();
            BigDecimal afterStock = isOutbound ? beforeStock.subtract(ioNum) : beforeStock.add(ioNum);
            m.setStockNum(afterStock);
            materialMapper.updateById(m);

            MaterialIoRecord rec = new MaterialIoRecord();
            rec.setMaterialId(m.getId());
            rec.setIoType(dto.getOrderType());
            rec.setIoNum(ioNum);
            rec.setBeforeStock(beforeStock);
            rec.setAfterStock(afterStock);
            rec.setRemark(dto.getRemark());
            rec.setOperator(operator);
            rec.setOrderNo(orderNo);
            rec.setOrderType(dto.getOrderType());
            materialIoRecordMapper.insert(rec);

            StockOrderItem oi = new StockOrderItem();
            oi.setOrderId(orderId);
            oi.setMaterialId(m.getId());
            oi.setIoNum(ioNum);
            oi.setBeforeStock(beforeStock);
            oi.setAfterStock(afterStock);
            stockOrderItemMapper.insert(oi);
        }

        log.info("单据创建成功：{} {} 条 合计 {}", orderNo, order.getTotalCount(),
                totalNum.stripTrailingZeros().toPlainString());
        return order;
    }

    private String genOrderNo(String prefix, String dateStr) {
        String like = prefix + dateStr + "%";
        Long count = stockOrderMapper.selectCount(Wrappers.<StockOrder>lambdaQuery()
                .likeRight(StockOrder::getOrderNo, like));
        int seq = (int) (count == null ? 0 : count) + 1;
        return prefix + dateStr + String.format("%03d", seq);
    }

    @Override
    public List<StockOrder> listOrders(Integer orderType) {
        var w = Wrappers.<StockOrder>lambdaQuery().orderByDesc(StockOrder::getCreateTime);
        if (orderType != null) w.eq(StockOrder::getOrderType, orderType);
        return stockOrderMapper.selectList(w);
    }

    @Override
    public Map<String, Object> getOrderDetail(Long orderId) {
        StockOrder order = stockOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "单据不存在");
        List<StockOrderItem> items = stockOrderItemMapper.selectList(Wrappers.<StockOrderItem>lambdaQuery()
                .eq(StockOrderItem::getOrderId, orderId));
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (StockOrderItem it : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialId", it.getMaterialId());
            row.put("ioNum", it.getIoNum());
            row.put("beforeStock", it.getBeforeStock());
            row.put("afterStock", it.getAfterStock());
            Material m = materialMapper.selectById(it.getMaterialId());
            if (m != null) {
                row.put("materialCode", m.getMaterialCode());
                row.put("materialName", m.getMaterialName());
                row.put("spec", m.getSpec());
                row.put("unit", m.getUnit());
            }
            enriched.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", enriched);
        return result;
    }
}
