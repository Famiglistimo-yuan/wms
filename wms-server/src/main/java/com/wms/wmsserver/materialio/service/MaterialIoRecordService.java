package com.wms.wmsserver.materialio.service;

import com.wms.wmsserver.materialio.dto.IoRecordQuery;
import com.wms.wmsserver.materialio.entity.MaterialIoRecord;

import java.util.List;
import java.util.Map;

/**
 * 子系统2 —— 出入库流水查询接口。
 */
public interface MaterialIoRecordService {

    /** 查询全部流水，按创建时间倒序（最新的在前） */
    List<MaterialIoRecord> listAll();

    /**
     * 按物料 ID 查流水，按时间倒序排列。
     */
    List<MaterialIoRecord> listByMaterialId(Long materialId);

    /**
     * 按类型筛选：1=入库，2=出库，null=全部；按创建时间倒序。
     */
    List<MaterialIoRecord> listByIoType(Integer ioType);

    /**
     * 分页 + 条件筛选（物料 / 时间范围 / 单据类型）。
     * 返回结构：{total: N, records: [...] }
     */
    Map<String, Object> pageQuery(IoRecordQuery query);
}
