package com.wms.wmsserver.materialio.service;

import com.wms.wmsserver.materialio.dto.BatchIoDTO;
import com.wms.wmsserver.materialio.entity.Material;

import java.math.BigDecimal;
import java.util.List;

/**
 * 子系统2 —— 物料业务接口。
 * <p>
 * 职责：物料新增、查询，以及核心的入库 / 出库业务。
 * 实现类 {@link com.wms.wmsserver.materialio.service.impl.MaterialServiceImpl}
 * 内所有方法均带 {@code @Transactional}（单条/批量各自一个事务），
 * 保证「更新 material 库存 + 写 material_io_record 流水」作为单事务原子执行。
 */
public interface MaterialService {

    /** 全量物料列表（按 id 升序） */
    List<Material> listAll();

    /**
     * 单条入库。事务内完成：校验 → 加库存 → 写流水。
     */
    void stockIn(Long materialId, BigDecimal ioNum, String remark);

    /**
     * 单条出库。事务内完成：校验 → 扣库存 → 写流水。
     */
    void stockOut(Long materialId, BigDecimal ioNum, String remark);

    /**
     * 批量入库。整批一个事务，任意一条失败全部回滚。
     */
    void batchIn(BatchIoDTO dto);

    /**
     * 批量出库。整批一个事务，任意一条失败全部回滚（包括库存不足）。
     */
    void batchOut(BatchIoDTO dto);

    /** 新增物料档案。若未指定初始库存，默认 0。 */
    void addMaterial(Material material);

    /**
     * 修改物料档案（编码、名称、规格、单位）。
     * 库存由进出仓业务维护，不走本方法。
     */
    void updateMaterial(Material material);
}
