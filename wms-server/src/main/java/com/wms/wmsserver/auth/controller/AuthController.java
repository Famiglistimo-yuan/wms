package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.common.dto.LoginRequest;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsserver.auth.interceptor.JwtInterceptor;
import com.wms.wmsserver.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录/鉴权接口。login/logout 放行；/me 走拦截器校验 token 合法性。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录：POST /api/auth/login */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    /**
     * 当前用户信息（启动恢复校验 + 刷新权限集）：GET /api/auth/me。
     * 走 JwtInterceptor：token 过期/无效直接返回 401；
     * 合法时从数据库取最新权限集（覆盖 token 内可能陈旧的 permissions，
     * 管理员收回权限后旧会话自动同步）。
     */
    @GetMapping("/me")
    public Result<LoginResponse> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        // 客户端调用 me 时带的 Bearer token 原样回传（不重签发，续期由前端控制）
        String auth = request.getHeader("Authorization");
        String token = auth != null ? auth.substring(7).trim() : null;
        return Result.success(authService.me(userId, token));
    }

    /**
     * 注销：POST /api/auth/logout
     * 无状态 JWT 服务端不存黑名单，注销由客户端清 token；此接口保留为语义入口，
     * 可在后续接入短 token + 黑名单时扩展。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
