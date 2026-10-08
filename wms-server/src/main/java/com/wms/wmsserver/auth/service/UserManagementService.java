package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.wmsserver.auth.dto.*;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.entity.UserRole;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.RoleMapper;
import com.wms.wmsserver.mapper.UserMapper;
import com.wms.wmsserver.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理：CRUD + 角色分配。
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final JdbcTemplate jdbc;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 用户列表（联表人员档案） */
    public List<UserVO> listUsers() {
        return jdbc.query("""
                SELECT u.id, u.username, u.status,
                       u.person_id, p.person_code, p.name AS real_name
                FROM user u LEFT JOIN person p ON p.id = u.person_id
                ORDER BY u.id
                """, (rs, i) -> UserVO.builder()
                .id(rs.getLong("id"))
                .username(rs.getString("username"))
                .personId(rs.getLong("person_id"))
                .personCode(rs.getString("person_code"))
                .realName(rs.getString("real_name"))
                .status(rs.getInt("status"))
                .build());
    }

    /** 新增用户 */
    @Transactional
    public UserVO create(UserCreateDTO dto) {
        // 用户名唯一
        Long dup = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        if (dup > 0) throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");

        // 人员档案必须存在
        Person p = personMapper.selectById(dto.getPersonId());
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "关联的人员档案不存在");

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
    @Transactional
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

    /** 给用户分配角色（全量覆盖） */
    @Transactional
    public void assignRoles(AssignRolesDTO dto) {
        userRoleMapper.deleteByUserId(dto.getUserId());
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                UserRole ur = new UserRole();
                ur.setUserId(dto.getUserId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    /** 查询用户已分配角色 id 列表 */
    public List<Long> listRoleIds(Long userId) {
        return jdbc.queryForList(
                "SELECT role_id FROM user_role WHERE user_id = ?",
                Long.class, userId);
    }
}
