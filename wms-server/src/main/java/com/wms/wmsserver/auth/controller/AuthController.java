package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.common.dto.LoginRequest;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsserver.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录/鉴权接口（auth 公开，不走 JwtInterceptor；拦截器在 WebMvcConfig 里放行 /api/auth/**）。
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
     * 注销：POST /api/auth/logout
     * 无状态 JWT 服务端不存黑名单，注销由客户端清 token；此接口保留为语义入口，
     * 可在后续接入短 token + 黑名单时扩展。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
