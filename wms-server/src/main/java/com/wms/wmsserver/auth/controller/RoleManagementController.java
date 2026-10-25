package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.dto.PermissionVO;
import com.wms.wmsserver.auth.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限资源列表（授权界面用）。角色相关接口已随直授模型移除，角色仅作展示标签，
 * 本控制器仅保留 /api/roles/permissions 路径（技术方案 §6.2 清单内），实现在 UserManagementService。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleManagementController {

    private final UserManagementService service;

    /** 权限资源列表（菜单项） */
    @GetMapping("/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(service.listPermissions());
    }
}
