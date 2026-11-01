package com.wms.wmsserver.materialio.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.wmsserver.materialio.dto.IoRecordQuery;
import com.wms.wmsserver.materialio.entity.Material;
import com.wms.wmsserver.materialio.entity.MaterialIoRecord;
import com.wms.wmsserver.materialio.entity.StockOrder;
import com.wms.wmsserver.materialio.mapper.MaterialIoMapper;
import com.wms.wmsserver.materialio.mapper.MaterialIoRecordMapper;
import com.wms.wmsserver.materialio.mapper.StockOrderMapper;
import com.wms.wmsserver.materialio.service.MaterialIoRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 子系统2 —— 出入库流水查询实现。
 */
@Service
@RequiredArgsConstructor
public class MaterialIoRecordServiceImpl implements MaterialIoRecordService {

    private final MaterialIoRecordMapper recordMapper;
    private final MaterialIoMapper materialMapper;
    private final StockOrderMapper stockOrderMapper;

    /** 分页上限保护 */
    private static final int MAX_PAGE_SIZE = 200;

    @Override
    public List<MaterialIoRecord> listAll() {
        return recordMapper.selectList(baseWrapper());
    }

    @Override
    public List<MaterialIoRecord> listByMaterialId(Long materialId) {
        return recordMapper.selectList(baseWrapper()
                .eq(MaterialIoRecord::getMaterialId, materialId));
    }

    @Override
    public List<MaterialIoRecord> listByIoType(Integer ioType) {
        var w = baseWrapper();
        if (ioType != null) w.eq(MaterialIoRecord::getIoType, ioType);
        return recordMapper.selectList(w);
    }

    @Override
    public Map<String, Object> pageQuery(IoRecordQuery query) {
        // ---------- 0. 分页钳位 ----------
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;

        // ---------- 1. handler 反查 stock_order → orderNo 集合 ----------
        List<String> orderNosFromHandler = null;
        if (query.getHandler() != null && !query.getHandler().isBlank()) {
            orderNosFromHandler = stockOrderMapper.selectList(
                    Wrappers.<StockOrder>lambdaQuery()
                            .eq(StockOrder::getHandler, query.getHandler())
                            .select(StockOrder::getOrderNo))
                    .stream().map(StockOrder::getOrderNo).collect(Collectors.toList());
            if (orderNosFromHandler.isEmpty()) {
                // 没有任何单据符合 handler，直接返回空
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("total", 0);
                empty.put("page", page);
                empty.put("pageSize", size);
                empty.put("records", Collections.emptyList());
                return empty;
            }
        }

        // ---------- 2. materialKeyword 反查 material → id 集合 ----------
        List<Long> materialIds = null;
        if (query.getMaterialKeyword() != null && !query.getMaterialKeyword().isBlank()) {
            materialIds = materialMapper.selectList(
                    Wrappers.<Material>lambdaQuery()
                            .and(w -> w.like(Material::getMaterialCode, query.getMaterialKeyword())
                                    .or().like(Material::getMaterialName, query.getMaterialKeyword()))
                            .select(Material::getId))
                    .stream().map(Material::getId).collect(Collectors.toList());
            if (materialIds.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("total", 0);
                empty.put("page", page);
                empty.put("pageSize", size);
                empty.put("records", Collections.emptyList());
                return empty;
            }
        }

        // ---------- 3. 构建主查询 ----------
        var w = Wrappers.<MaterialIoRecord>lambdaQuery()
                .eq(query.getMaterialId() != null, MaterialIoRecord::getMaterialId, query.getMaterialId())
                .eq(query.getOrderType()  != null, MaterialIoRecord::getOrderType,   query.getOrderType())
                .ge(query.getStartTime()  != null, MaterialIoRecord::getCreateTime,   query.getStartTime())
                .le(query.getEndTime()    != null, MaterialIoRecord::getCreateTime,   query.getEndTime())
                .like(query.getOrderNo()  != null && !query.getOrderNo().isBlank(),
                        MaterialIoRecord::getOrderNo, query.getOrderNo())
                .eq(query.getOperator()   != null && !query.getOperator().isBlank(),
                        MaterialIoRecord::getOperator, query.getOperator())
                .like(query.getSourceTarget() != null && !query.getSourceTarget().isBlank(),
                        MaterialIoRecord::getSourceTarget, query.getSourceTarget())
                .orderByDesc(MaterialIoRecord::getCreateTime);

        // materialKeyword 反查结果
        if (materialIds != null) w.in(MaterialIoRecord::getMaterialId, materialIds);
        // handler 反查结果
        if (orderNosFromHandler != null) w.in(MaterialIoRecord::getOrderNo, orderNosFromHandler);

        Long total = recordMapper.selectCount(w);
        int offset = (page - 1) * size;

        List<MaterialIoRecord> records = recordMapper.selectList(w.last("LIMIT " + offset + "," + size));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", total);
        r.put("page", page);
        r.put("pageSize", size);
        r.put("records", records);
        return r;
    }

    /** 默认按 create_time 倒序的基础查询 */
    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MaterialIoRecord> baseWrapper() {
        return Wrappers.<MaterialIoRecord>lambdaQuery().orderByDesc(MaterialIoRecord::getCreateTime);
    }
}
