package com.wms.wmsserver.person.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.RegexPatterns;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.UserMapper;
import com.wms.wmsserver.person.dto.PersonCreateDTO;
import com.wms.wmsserver.person.dto.PersonUpdateDTO;
import com.wms.wmsserver.person.dto.PersonVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@Slf4j
@RequiredArgsConstructor
public class PersonService {

    private final PersonMapper personMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;

    /** 全部列表（按 person_code 排序，方便下拉选择） */
    public List<PersonVO> listAll() {
        return personMapper.selectList(Wrappers.<Person>lambdaQuery()
                        .orderByAsc(Person::getPersonCode))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    /** FR-1-2-7：按姓名模糊查询（LIKE '%keyword%'，天然兼容单字） */
    public List<PersonVO> searchBySingleChar(String ch) {
        if (ch == null || ch.isBlank()) {
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
        p.setIdCard(emptyToNull(dto.getIdCard()));
        p.setHometown(dto.getHometown());
        p.setAddress(dto.getAddress());
        p.setPhone(dto.getPhone());
        p.setRemark(dto.getRemark());
        try {
            personMapper.insert(p);
        } catch (DataIntegrityViolationException e) {
            log.warn("新增人员代码冲突：{}", dto.getPersonCode(), e);
            throw new BusinessException(ErrorCode.CONFLICT, "人员代码已存在");
        }
        // 勾选时同事务创建关联登录账号，失败整体回滚，不留"有档案无账号"半成品
        if (Boolean.TRUE.equals(dto.getCreateUser())) {
            createLinkedUser(p.getId(), dto.getUsername(), dto.getPassword());
        }
        return toVO(p);
    }

    /** 创建与人员关联的登录账号（FR-4-1）；规则与 UserManagementService.create 对齐 */
    private void createLinkedUser(Long personId, String username, String password) {
        if (username == null || !username.matches(RegexPatterns.USERNAME)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名须字母开头，4-30 位字母/数字/下划线");
        }
        if (password == null || !password.matches(RegexPatterns.PASSWORD)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "口令须为 6-20 位字母/数字");
        }
        Long dup = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
        if (dup > 0) throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");

        User u = new User();
        u.setUsername(username);
        u.setPersonId(personId);
        u.setPassword(encoder.encode(password));
        u.setStatus(1);
        try {
            userMapper.insert(u);
        } catch (DataIntegrityViolationException e) {
            log.warn("新增人员联动创建账号冲突：{}", username, e);
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PersonUpdateDTO dto) {
        Person p = personMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "人员不存在");

        if (dto.getName() != null) p.setName(dto.getName());
        if (dto.getGender() != null) p.setGender(dto.getGender());
        if (dto.getBirthDate() != null) p.setBirthDate(dto.getBirthDate());
        if (dto.getIdCard() != null) p.setIdCard(emptyToNull(dto.getIdCard()));
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
        // User 表外键在删除时数据库会拒绝（FK 约束），此处友好提示
        try {
            personMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            log.warn("删除人员 {} 失败：存在关联登录账号", id, e);
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

    /** 空串转 null：数据库 CHECK 约束（IS NULL OR 正则）不接受空串 */
    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
