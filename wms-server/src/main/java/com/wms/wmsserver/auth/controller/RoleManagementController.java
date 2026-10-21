package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.dto.PermissionVO;
import com.wms.wmsserver.auth.service.RoleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限资源列表（授权界面用）。角色相关接口已随直授模型移除，角色仅作展示标签。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleManagementController {

    private final RoleManagementService service;

    /** 权限资源列表（菜单项） */
    @GetMapping("/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<List<PermissionVO>> listPermissions() {
        return Result.success(service.listPermissions());
    }
}
