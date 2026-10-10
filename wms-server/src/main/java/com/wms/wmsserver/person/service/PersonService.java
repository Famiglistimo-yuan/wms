package com.wms.wmsserver.person.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.person.dto.PersonCreateDTO;
import com.wms.wmsserver.person.dto.PersonUpdateDTO;
import com.wms.wmsserver.person.dto.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 人员档案 CRUD + 单字模糊查询（FR-1-2-7）。
 * 单字模糊：WHERE name LIKE '%{char}%'，索引覆盖只对前缀 LIKE 生效，
 * 报告中说明取舍：姓名一般 2-4 字，全表扫描可接受。
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonMapper personMapper;

    /** 全部列表（按 person_code 排序，方便下拉选择） */
    public List<PersonVO> listAll() {
        return personMapper.selectList(Wrappers.<Person>lambdaQuery()
                        .orderByAsc(Person::getPersonCode))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /** FR-1-2-7：输入姓名任何一个单字 → 含该字全部人员（LIKE '%x%'） */
    public List<PersonVO> searchBySingleChar(String ch) {
        if (ch == null || ch.length() != 1) {
            // 空或非单字 → 返回全部（客户端搜索框清空后的默认行为）
            return listAll();
        }
        return personMapper.selectList(Wrappers.<Person>lambdaQuery()
                        .like(Person::getName, ch)
                        .orderByAsc(Person::getPersonCode))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    public PersonVO get(Long id) {
        Person p = personMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "人员不存在");
        return toVO(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public PersonVO create(PersonCreateDTO dto) {
        Long dup = personMapper.selectCount(Wrappers.<Person>lambdaQuery()
                .eq(Person::getPersonCode, dto.getPersonCode()));
        if (dup > 0) throw new BusinessException(ErrorCode.CONFLICT, "人员代码已存在");

        Person p = new Person();
        p.setPersonCode(dto.getPersonCode());
        p.setName(dto.getName());
        p.setGender(dto.getGender());
        p.setBirthDate(dto.getBirthDate());
        p.setIdCard(dto.getIdCard());
        p.setHometown(dto.getHometown());
        p.setAddress(dto.getAddress());
        p.setPhone(dto.getPhone());
        p.setRemark(dto.getRemark());
        personMapper.insert(p);
        return toVO(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PersonUpdateDTO dto) {
        Person p = personMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "人员不存在");

        if (dto.getName() != null) p.setName(dto.getName());
        if (dto.getGender() != null) p.setGender(dto.getGender());
        if (dto.getBirthDate() != null) p.setBirthDate(dto.getBirthDate());
        if (dto.getIdCard() != null) p.setIdCard(dto.getIdCard());
        if (dto.getHometown() != null) p.setHometown(dto.getHometown());
        if (dto.getAddress() != null) p.setAddress(dto.getAddress());
        if (dto.getPhone() != null) p.setPhone(dto.getPhone());
        if (dto.getRemark() != null) p.setRemark(dto.getRemark());
        personMapper.updateById(p);
    }

    /** FR-1-2-6：删除前必须定位；若关联登录用户则拒绝删除 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Person p = personMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "人员不存在");
        Long refs = personMapper.selectCount(Wrappers.<Person>lambdaQuery().eq(Person::getId, id));
        if (refs <= 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "人员不存在");
        // User 表外键在删除时数据库会拒绝（FK 约束），此处友好提示
        try {
            personMapper.deleteById(id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CONFLICT, "该人员已关联登录账号，无法删除");
        }
    }

    private PersonVO toVO(Person p) {
        return PersonVO.builder()
                .id(p.getId())
                .personCode(p.getPersonCode())
                .name(p.getName())
                .gender(p.getGender())
                .birthDate(p.getBirthDate())
                .idCard(p.getIdCard())
                .hometown(p.getHometown())
                .address(p.getAddress())
                .phone(p.getPhone())
                .remark(p.getRemark())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
