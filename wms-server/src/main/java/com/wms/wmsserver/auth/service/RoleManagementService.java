package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.wmsserver.auth.dto.PermissionVO;
import com.wms.wmsserver.auth.dto.RoleVO;
import com.wms.wmsserver.entity.Permission;
import com.wms.wmsserver.entity.Role;
import com.wms.wmsserver.entity.RolePermission;
import com.wms.wmsserver.mapper.PermissionMapper;
import com.wms.wmsserver.mapper.RoleMapper;
import com.wms.wmsserver.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色 & 权限管理：角色 CRUD + 给角色分配权限码。
 * 新增权限 → 权限表插行 → 授权界面自动出现（FR-4-7 可扩展性）。
 */
@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /** 角色列表（含已分配权限码） */
    public List<RoleVO> listRoles() {
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId));
        return roles.stream().map(r -> RoleVO.builder()
                .id(r.getId())
                .roleCode(r.getRoleCode())
                .roleName(r.getRoleName())
                .remark(r.getRemark())
                .permCodes(rolePermissionMapper.selectPermCodesByRoleId(r.getId()))
                .build()).collect(Collectors.toList());
    }

    /** 权限资源列表（菜单项，按 sort_order 排序） */
    public List<PermissionVO> listPermissions() {
        List<Permission> perms = permissionMapper.selectList(
                Wrappers.<Permission>lambdaQuery().orderByAsc(Permission::getSortOrder));
        return perms.stream().map(p -> PermissionVO.builder()
                .id(p.getId())
                .permCode(p.getPermCode())
                .permName(p.getPermName())
                .sortOrder(p.getSortOrder())
                .build()).collect(Collectors.toList());
    }

    /** 给角色分配权限（全量覆盖） */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permIds) {
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permIds != null) {
            for (Long pid : permIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    /** 查询角色已分配的权限 id 列表 */
    public List<Long> listPermIds(Long roleId) {
        return rolePermissionMapper.selectPermIdsByRoleId(roleId);
    }
}
