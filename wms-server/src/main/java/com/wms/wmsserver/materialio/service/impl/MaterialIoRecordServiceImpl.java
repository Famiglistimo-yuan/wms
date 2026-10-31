package com.wms.wmsserver.materialio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.wmsserver.materialio.dto.IoRecordQuery;
import com.wms.wmsserver.materialio.entity.MaterialIoRecord;
import com.wms.wmsserver.materialio.mapper.MaterialIoRecordMapper;
import com.wms.wmsserver.materialio.service.MaterialIoRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子系统2 —— 出入库流水查询实现。
 */
@Service
@RequiredArgsConstructor
public class MaterialIoRecordServiceImpl implements MaterialIoRecordService {

    private final MaterialIoRecordMapper materialIoRecordMapper;

    @Override
    public List<MaterialIoRecord> listAll() {
        return materialIoRecordMapper.selectList(baseWrapper());
    }

    @Override
    public List<MaterialIoRecord> listByMaterialId(Long materialId) {
        return materialIoRecordMapper.selectList(baseWrapper()
                .eq(MaterialIoRecord::getMaterialId, materialId));
    }

    @Override
    public List<MaterialIoRecord> listByIoType(Integer ioType) {
        // 旧逻辑按 io_type 筛选（MaterialIoRecord 原有字段）
        var w = baseWrapper();
        if (ioType != null) w.eq(MaterialIoRecord::getIoType, ioType);
        return materialIoRecordMapper.selectList(w);
    }

    @Override
    public Map<String, Object> pageQuery(IoRecordQuery query) {
        // 构建筛选条件（所有条件可选，动态 eq/like）
        var w = Wrappers.<MaterialIoRecord>lambdaQuery()
                .eq(query.getMaterialId() != null, MaterialIoRecord::getMaterialId, query.getMaterialId())
                .eq(query.getOrderType()  != null, MaterialIoRecord::getOrderType,   query.getOrderType())
                .ge(query.getStartTime()  != null, MaterialIoRecord::getCreateTime,   query.getStartTime())
                .le(query.getEndTime()    != null, MaterialIoRecord::getCreateTime,   query.getEndTime())
                .like(query.getOrderNo()  != null && !query.getOrderNo().isBlank(),
                        MaterialIoRecord::getOrderNo, query.getOrderNo())
                .eq(query.getOperator()   != null && !query.getOperator().isBlank(),
                        MaterialIoRecord::getOperator, query.getOperator())
                .orderByDesc(MaterialIoRecord::getCreateTime);

        Long total = materialIoRecordMapper.selectCount(w);

        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        int offset = (page - 1) * size;

        List<MaterialIoRecord> records = materialIoRecordMapper.selectList(w.last("LIMIT " + offset + "," + size));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", total);
        r.put("page", page);
        r.put("pageSize", size);
        r.put("records", records);
        return r;
    }

    /** 默认按 create_time 倒序的基础查询 */
    private LambdaQueryWrapper<MaterialIoRecord> baseWrapper() {
        return Wrappers.<MaterialIoRecord>lambdaQuery().orderByDesc(MaterialIoRecord::getCreateTime);
    }
}
