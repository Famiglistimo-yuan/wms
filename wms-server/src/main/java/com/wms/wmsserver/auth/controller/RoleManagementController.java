package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.dto.PermissionVO;
import com.wms.wmsserver.auth.dto.RoleVO;
import com.wms.wmsserver.auth.service.RoleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色 & 权限资源列表（授权界面用）。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleManagementController {

    private final RoleManagementService service;

    /** 角色列表 */
    @GetMapping
    @RequirePermission("menu.auth.grant")
    public Result<List<RoleVO>> list() {
        return Result.success(service.listRoles());
    }

    /** 权限资源列表（菜单项） */
    @GetMapping("/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(service.listPermissions());
    }

    /** 查询某角色已分配的权限 id */
    @GetMapping("/{roleId}/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<List<Long>> listPermIds(@PathVariable Long roleId) {
        return Result.success(service.listPermIds(roleId));
    }

    /** 给角色分配权限（全量覆盖） */
    @PostMapping("/{roleId}/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<Void> assignPermissions(@PathVariable Long roleId,
                                          @RequestBody List<Long> permIds) {
        service.assignPermissions(roleId, permIds);
        return Result.success();
    }
}
