package com.wms.wmsserver.material.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.wmsserver.entity.Material;
import com.wms.wmsserver.mapper.MaterialMapper;
import com.wms.wmsserver.material.dto.MaterialCreateDTO;
import com.wms.wmsserver.material.dto.MaterialUpdateDTO;
import com.wms.wmsserver.material.dto.MaterialVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物料档案 CRUD + 模糊查询（FR-1-3-5）：keyword 同时匹配 material_code / name / spec。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;

    public List<MaterialVO> listAll() {
        return materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                        .orderByAsc(Material::getMaterialCode))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /** FR-1-3-5：keyword 对代码/名称/规格做 OR LIKE '%x%' */
    public List<MaterialVO> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) return listAll();
        return materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                        .and(w -> w.like(Material::getMaterialCode, keyword)
                                .or().like(Material::getName, keyword)
                                .or().like(Material::getSpec, keyword))
                        .orderByAsc(Material::getMaterialCode))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public MaterialVO create(MaterialCreateDTO dto) {
        Long dup = materialMapper.selectCount(Wrappers.<Material>lambdaQuery()
                .eq(Material::getMaterialCode, dto.getMaterialCode()));
        if (dup > 0) throw new BusinessException(ErrorCode.CONFLICT, "物料代码已存在");

        Material m = new Material();
        m.setMaterialCode(dto.getMaterialCode());
        m.setName(dto.getName());
        m.setSpec(dto.getSpec());
        m.setUnit(dto.getUnit());
        m.setStock(dto.getStock() != null ? dto.getStock() : BigDecimal.ZERO);
        m.setRemark(dto.getRemark());
        try {
            materialMapper.insert(m);
        } catch (DataIntegrityViolationException e) {
            log.warn("新增物料代码冲突：{}", dto.getMaterialCode(), e);
            throw new BusinessException(ErrorCode.CONFLICT, "物料代码已存在");
        }
        return toVO(m);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialUpdateDTO dto) {
        Material m = materialMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "物料不存在");
        if (dto.getName() != null) m.setName(dto.getName());
        if (dto.getSpec() != null) m.setSpec(dto.getSpec());
        if (dto.getUnit() != null) m.setUnit(dto.getUnit());
        if (dto.getRemark() != null) m.setRemark(dto.getRemark());
        materialMapper.updateById(m);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Material m = materialMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "物料不存在");
        try {
            materialMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            log.warn("删除物料 {} 失败：存在进出仓单引用", id, e);
            throw new BusinessException(ErrorCode.CONFLICT, "该物料已被进出仓单引用，无法删除");
        }
    }

    private MaterialVO toVO(Material m) {
        return MaterialVO.builder()
                .id(m.getId())
                .materialCode(m.getMaterialCode())
                .name(m.getName())
                .spec(m.getSpec())
                .unit(m.getUnit())
                .stock(m.getStock())
                .remark(m.getRemark())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
