package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.wmsserver.auth.dto.PersonOptionVO;
import com.wms.wmsserver.auth.dto.UserCreateDTO;
import com.wms.wmsserver.auth.dto.UserUpdateDTO;
import com.wms.wmsserver.auth.dto.UserVO;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.entity.UserRole;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.UserMapper;
import com.wms.wmsserver.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理：CRUD + 角色分配。
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder encoder;

    /** 人员下拉选项（新增用户时选关联人员） */
    public List<PersonOptionVO> listPersonOptions() {
        List<Person> persons = personMapper.selectList(
                Wrappers.<Person>lambdaQuery().orderByAsc(Person::getId));
        return persons.stream().map(p -> PersonOptionVO.builder()
                .id(p.getId())
                .personCode(p.getPersonCode())
                .realName(p.getName())
                .build()).collect(Collectors.toList());
    }

    /** 用户列表（联表人员档案） */
    public List<UserVO> listUsers() {
        return userMapper.selectUserVOs();
    }

    /** 新增用户 */
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateDTO dto) {
        // 用户名唯一
        Long dup = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        if (dup > 0) throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");

        // 人员档案必须存在
        Person p = personMapper.selectById(dto.getPersonId());
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "关联的人员档案不存在");

        // 同一人员不能重复绑定账号（uk_user_person）
        Long bound = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getPersonId, dto.getPersonId()));
        if (bound > 0) throw new BusinessException(ErrorCode.CONFLICT, "该人员已绑定登录账号");

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPersonId(dto.getPersonId());
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setStatus(1);
        userMapper.insert(u);

        return UserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .personId(p.getId())
                .personCode(p.getPersonCode())
                .realName(p.getName())
                .status(1)
                .build();
    }

    /** 修改用户（密码 / 状态） */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateDTO dto) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            u.setPassword(encoder.encode(dto.getPassword()));
        }
        if (dto.getStatus() != null) {
            u.setStatus(dto.getStatus());
        }
        userMapper.updateById(u);
    }

    /** 删除用户：同事务先解除其角色关系再删账号；禁止删除当前登录账号 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除当前登录的账号");
        }
        if (userMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    /** 给用户分配角色（全量覆盖） */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    /** 查询用户已分配角色 id 列表 */
    public List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }
}
