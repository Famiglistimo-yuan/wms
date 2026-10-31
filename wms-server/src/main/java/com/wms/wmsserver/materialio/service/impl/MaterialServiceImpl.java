package com.wms.wmsserver.materialio.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.wmsserver.materialio.dto.BatchIoDTO;
import com.wms.wmsserver.materialio.entity.Material;
import com.wms.wmsserver.materialio.entity.MaterialIoRecord;
import com.wms.wmsserver.materialio.mapper.MaterialIoMapper;
import com.wms.wmsserver.materialio.mapper.MaterialIoRecordMapper;
import com.wms.wmsserver.materialio.service.MaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物料业务实现：单条 / 批量入出库，一个 @Transactional 保证库存变更与流水落库原子执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialIoMapper materialMapper;
    private final MaterialIoRecordMapper materialIoRecordMapper;

    @Override
    public List<Material> listAll() {
        return materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                .orderByAsc(Material::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockIn(Long materialId, BigDecimal ioNum, String remark) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "物料不存在（id=" + materialId + "）");
        }
        BigDecimal beforeStock = material.getStockNum();
        BigDecimal afterStock = beforeStock.add(ioNum);
        material.setStockNum(afterStock);
        materialMapper.updateById(material);

        MaterialIoRecord record = new MaterialIoRecord();
        record.setMaterialId(materialId);
        record.setIoType(1);
        record.setIoNum(ioNum);
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setRemark(remark);
        materialIoRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockOut(Long materialId, BigDecimal ioNum, String remark) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "物料不存在（id=" + materialId + "）");
        }
        BigDecimal beforeStock = material.getStockNum();
        if (beforeStock.compareTo(ioNum) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "库存不足：当前 " + beforeStock.stripTrailingZeros().toPlainString()
                            + "，申请出库 " + ioNum.stripTrailingZeros().toPlainString());
        }
        BigDecimal afterStock = beforeStock.subtract(ioNum);
        material.setStockNum(afterStock);
        materialMapper.updateById(material);

        MaterialIoRecord record = new MaterialIoRecord();
        record.setMaterialId(materialId);
        record.setIoType(2);
        record.setIoNum(ioNum);
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setRemark(remark);
        materialIoRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchIn(BatchIoDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "批量入库条目不能为空");
        }
        for (BatchIoDTO.Item item : dto.getItems()) {
            if (item.getMaterialId() == null || item.getIoNum() == null
                    || item.getIoNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "存在非法条目（materialId / ioNum 无效）");
            }
            stockIn(item.getMaterialId(), item.getIoNum(), dto.getRemark());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchOut(BatchIoDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "批量出库条目不能为空");
        }
        for (BatchIoDTO.Item item : dto.getItems()) {
            if (item.getMaterialId() == null || item.getIoNum() == null
                    || item.getIoNum().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "存在非法条目（materialId / ioNum 无效）");
            }
            stockOut(item.getMaterialId(), item.getIoNum(), dto.getRemark());
        }
    }

    @Override
    public void addMaterial(Material material) {
        if (material.getStockNum() == null) material.setStockNum(BigDecimal.ZERO);
        materialMapper.insert(material);
    }

    @Override
    public void updateMaterial(Material material) {
        if (material.getId() == null || materialMapper.selectById(material.getId()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "物料不存在");
        }
        materialMapper.updateById(material);
    }
}
