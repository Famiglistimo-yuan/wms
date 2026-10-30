package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.WmsConstants;
import com.wms.common.dto.PersonOptionVO;
import com.wms.common.dto.PermissionVO;
import com.wms.wmsserver.auth.dto.UserCreateDTO;
import com.wms.wmsserver.auth.dto.UserPermissionGrantDTO;
import com.wms.common.dto.UserPermissionVO;
import com.wms.wmsserver.auth.dto.UserUpdateDTO;
import com.wms.common.dto.UserVO;
import com.wms.wmsserver.entity.Permission;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.entity.UserPermission;
import com.wms.wmsserver.mapper.PermissionMapper;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.UserMapper;
import com.wms.wmsserver.mapper.UserPermissionMapper;
import com.wms.wmsserver.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理：CRUD + 直接授权。
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserPermissionMapper userPermissionMapper;
    private final PermissionMapper permissionMapper;

    private final PasswordEncoder encoder;

    /** 按权限码查询权限记录（自救防护用；权限表为种子数据，运行期不变） */
    private Permission resolvePerm(String permCode) {
        Permission p = permissionMapper.selectOne(
                Wrappers.<Permission>lambdaQuery().eq(Permission::getPermCode, permCode));
        if (p == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "未知权限码：" + permCode);
        return p;
    }

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

    /** 用户列表（联表人员档案；keyword≤50 字符且已转义 LIKE 通配符，模糊匹配用户名/人员编号/真实姓名，空白视为全量） */
    public List<UserVO> listUsers(String keyword) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (kw != null) {
            if (kw.length() > 50) {
                kw = kw.substring(0, 50);
            }
            // LIKE 通配符转义：% _ \ 前加反斜杠（MySQL LIKE 默认以反斜杠为转义符）
            kw = kw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        }
        return userMapper.selectUserVOs(kw);
    }

    /** 权限资源列表（菜单项，按 sort_order 排序）；新增权限 → 权限表插行 → 授权界面自动出现（FR-4-7） */
    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(
                        Wrappers.<Permission>lambdaQuery().orderByAsc(Permission::getSortOrder))
                .stream().map(p -> PermissionVO.builder()
                        .id(p.getId())
                        .permCode(p.getPermCode())
                        .permName(p.getPermName())
                        .sortOrder(p.getSortOrder())
                        .build()).collect(Collectors.toList());
    }

    /** 新增用户 */
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateDTO dto) {
        // admin 为系统管理员保留用户名：禁止创建同名账号，
        // 防止 admin 账号在数据层意外丢失后，新账号静默继承超管身份（提权防护）
        if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(dto.getUsername())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "admin 为系统管理员保留用户名，不可用于创建新账号");
        }

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

    /** 修改用户（密码 / 状态）；admin 仅口令可改、状态恒启用；停用时禁止停用当前登录账号、禁止停到无启用账号、禁止停掉唯一授权持有者 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateDTO dto, Long currentUserId) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            u.setPassword(encoder.encode(dto.getPassword()));
        }
        if (dto.getStatus() != null) {
            // 系统管理员状态锁定：恒启用，不接受任何状态修改
            if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(u.getUsername())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "系统管理员账号状态不可修改");
            }
            if (dto.getStatus() == 0) {
                if (id.equals(currentUserId)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "不能停用当前登录的账号");
                }
                // 至少保留一个启用账号，否则无人可登录、界面无法自救
                Long enabled = userMapper.selectCount(
                        Wrappers.<User>lambdaQuery().eq(User::getStatus, 1).ne(User::getId, id));
                if (enabled == 0) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "至少保留一个启用账号");
                }
                // 禁止停用掉唯一持有 menu.auth.grant 或 menu.auth.user 的启用账号——授权/用户管理界面死锁
                assertNotLastAuthHolder(id);
            }
            u.setStatus(dto.getStatus());
        }
        userMapper.updateById(u);
    }

    /** 删除用户：同事务解除其角色关系与直接权限再删账号；禁止删除当前登录账号、系统管理员、唯一授权持有者 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除当前登录的账号");
        }
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        // 系统管理员账号不可删除
        if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(u.getUsername())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "系统管理员账号不可删除");
        }
        assertNotLastAuthHolder(id);
        userRoleMapper.deleteByUserId(id);
        userPermissionMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    /**
     * 检查目标用户是否为最后一个持有 menu.auth.grant 或 menu.auth.user 的启用账号。
     * 若删除/停用该用户会导致无人可进入授权或用户管理界面，则拒绝。
     */
    private void assertNotLastAuthHolder(Long targetUserId) {
        assertNotLastAuthHolderForPerm(targetUserId, "menu.auth.grant", "用户授权");
        assertNotLastAuthHolderForPerm(targetUserId, "menu.auth.user", "用户管理");
    }

    private void assertNotLastAuthHolderForPerm(Long targetUserId, String permCode, String permLabel) {
        Permission perm = resolvePerm(permCode);
        long totalEnabledHolders = userPermissionMapper.countEnabledAllowUsersByPermCode(permCode);
        boolean targetHolds = userPermissionMapper
                .selectPermIdsByUserIdAndEffect(targetUserId, "allow").contains(perm.getId());
        // 删/停后剩余 = total - (targetHolds ? 1 : 0)
        if (totalEnabledHolders - (targetHolds ? 1 : 0) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "系统至少保留一个拥有「" + permLabel + "」权限的启用账号");
        }
    }

    /** 用户授权回显：直授 allow 集 + 禁止 deny 集（系统管理员恒为全部允许） */
    public UserPermissionVO getUserPermissions(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        // 系统管理员：回显恒为全部权限 allow、deny 空（与其 user_permission 行无关）
        if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(u.getUsername())) {
            List<Long> allIds = permissionMapper.selectList(
                            Wrappers.<Permission>lambdaQuery().orderByAsc(Permission::getSortOrder))
                    .stream().map(Permission::getId).collect(Collectors.toList());
            return UserPermissionVO.builder()
                    .allowPermIds(allIds)
                    .denyPermIds(List.of())
                    .build();
        }
        return UserPermissionVO.builder()
                .allowPermIds(userPermissionMapper.selectPermIdsByUserIdAndEffect(userId, "allow"))
                .denyPermIds(userPermissionMapper.selectPermIdsByUserIdAndEffect(userId, "deny"))
                .build();
    }

    /** 用户直接授权（全量覆盖 allow/deny；只动该用户的行，不影响其他用户；系统管理员不可修改） */
    @Transactional(rollbackFor = Exception.class)
    public void grantPermissions(Long userId, UserPermissionGrantDTO dto) {
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        // 系统管理员权限恒为全部允许，不接受任何授权修改
        if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(target.getUsername())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "系统管理员账号的权限不可修改");
        }

        // P4 入参校验：allow/deny 无交集
        List<Long> allow = dto.getAllow() == null ? List.of() : dto.getAllow();
        List<Long> deny = dto.getDeny() == null ? List.of() : dto.getDeny();
        Set<Long> overlap = new HashSet<>(allow);
        overlap.retainAll(new HashSet<>(deny));
        if (!overlap.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "allow/deny 列表不应包含同一权限项，重复 id：" + overlap);
        }

        // P4 入参校验：permId 必须都存在于 permission 表
        Set<Long> allIds = new HashSet<>();
        allIds.addAll(allow);
        allIds.addAll(deny);
        if (!allIds.isEmpty() && permissionMapper.selectBatchIds(allIds).size() != allIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在无效的权限 id");
        }

        userPermissionMapper.deleteByUserId(userId);
        insertUserPerms(userId, allow, "allow");
        insertUserPerms(userId, deny, "deny");

        // P3 保底检查：menu.auth.grant 与 menu.auth.user 均需至少保留一个启用持有者
        assertAtLeastOneEnabledHolder("menu.auth.grant", "用户授权");
        assertAtLeastOneEnabledHolder("menu.auth.user", "用户管理");
    }

    private void assertAtLeastOneEnabledHolder(String permCode, String permLabel) {
        if (userPermissionMapper.countEnabledAllowUsersByPermCode(permCode) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "系统至少保留一个拥有「" + permLabel + "」权限的启用账号");
        }
    }

    private void insertUserPerms(Long userId, List<Long> permIds, String effect) {
        for (Long permId : permIds) {
            UserPermission up = new UserPermission();
            up.setUserId(userId);
            up.setPermId(permId);
            up.setEffect(effect);
            userPermissionMapper.insert(up);
        }
    }
}
