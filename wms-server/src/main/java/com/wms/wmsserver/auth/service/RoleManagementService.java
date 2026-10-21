package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.wmsserver.auth.dto.PermissionVO;
import com.wms.wmsserver.entity.Permission;
import com.wms.wmsserver.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限资源管理。
 * 新增权限 → 权限表插行 → 授权界面自动出现（FR-4-7 可扩展性）。
 * 角色仅作展示标签（种子数据维护），不参与有效权限计算，无管理接口。
 */
@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final PermissionMapper permissionMapper;

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
}
