package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.common.dto.PersonOptionVO;
import com.wms.common.dto.UserPermissionVO;
import com.wms.common.dto.UserVO;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.dto.UserCreateDTO;
import com.wms.wmsserver.auth.dto.UserPermissionGrantDTO;
import com.wms.wmsserver.auth.dto.UserUpdateDTO;
import com.wms.wmsserver.auth.interceptor.JwtInterceptor;
import com.wms.wmsserver.auth.service.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
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

    /** 人员下拉选项（新增用户时选关联人员） */
    @GetMapping("/person-options")
    @RequirePermission("menu.auth.user")
    public Result<List<PersonOptionVO>> personOptions() {
        return Result.success(service.listPersonOptions());
    }

    @PutMapping("/{id}")
    @RequirePermission("menu.auth.user")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateDTO dto,
                               HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        service.update(id, dto, currentUserId);
        return Result.success();
    }

    /** 删除用户（连带解除其角色关系；禁止删除当前登录账号） */
    @DeleteMapping("/{id}")
    @RequirePermission("menu.auth.user")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        service.delete(id, currentUserId);
        return Result.success();
    }

    /** 用户直接授权回显（allow/deny 集合） */
    @GetMapping("/{id}/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<UserPermissionVO> getPermissions(@PathVariable Long id) {
        return Result.success(service.getUserPermissions(id));
    }

    /** 用户直接授权：对单个用户授予/禁止某菜单执行权（全量覆盖，不影响其他用户） */
    @PostMapping("/{id}/permissions")
    @RequirePermission("menu.auth.grant")
    public Result<Void> grantPermissions(@PathVariable Long id, @RequestBody UserPermissionGrantDTO dto) {
        service.grantPermissions(id, dto);
        return Result.success();
    }
}
