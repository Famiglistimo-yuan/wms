package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.dto.*;
import com.wms.wmsserver.auth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理（需 menu.auth.user 权限）。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService service;

    @GetMapping
    @RequirePermission("menu.auth.user")
    public Result<List<UserVO>> list() {
        return Result.success(service.listUsers());
    }

    @PostMapping
    @RequirePermission("menu.auth.user")
    public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    @RequirePermission("menu.auth.user")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateDTO dto) {
        service.update(id, dto);
        return Result.success();
    }

    /** 查询某用户已分配的角色 id（给授权界面用） */
    @GetMapping("/{id}/roles")
    @RequirePermission("menu.auth.user")
    public Result<List<Long>> listRoles(@PathVariable Long id) {
        return Result.success(service.listRoleIds(id));
    }

    /** 给用户分配角色（全量覆盖） */
    @PostMapping("/roles")
    @RequirePermission("menu.auth.grant")
    public Result<Void> assignRoles(@RequestBody AssignRolesDTO dto) {
        service.assignRoles(dto);
        return Result.success();
    }
}
