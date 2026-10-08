package com.wms.wmsserver.auth.interceptor;

import tools.jackson.databind.ObjectMapper;
import com.wms.common.ErrorCode;
import com.wms.common.Result;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * JWT 拦截器：
 *   1) 提取 Authorization: Bearer <token>，解析得到 userId + permissions
 *   2) 未标注 @RequirePermission 的方法：只要 token 合法即通过
 *   3) 标注了 @RequirePermission("xxx") 的：额外校验权限集中是否含 "xxx"
 * 失败直接写 JSON 响应，HTTP 状态恒 200，业务码走 401/403（技术方案 §6.1）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    /** request attribute key，供 Controller 层取当前用户信息 */
    public static final String ATTR_USER_ID = "wms.userId";
    public static final String ATTR_USERNAME = "wms.username";
    public static final String ATTR_PERMISSIONS = "wms.permissions";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true; // 静态资源等，放行
        }

        // 1) 取 token
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeFail(response, ErrorCode.UNAUTHORIZED, "未登录");
            return false;
        }
        String token = auth.substring(7).trim();
        if (token.isEmpty()) {
            writeFail(response, ErrorCode.UNAUTHORIZED, "token 为空");
            return false;
        }

        // 2) 解析
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException e) {
            writeFail(response, ErrorCode.UNAUTHORIZED, "token 无效或已过期");
            return false;
        }

        // 3) 写 request attribute，供下游使用
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);
        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_USERNAME, username);
        request.setAttribute(ATTR_PERMISSIONS, permissions == null ? List.of() : permissions);

        // 4) 权限校验
        RequirePermission rp = hm.getMethodAnnotation(RequirePermission.class);
        if (rp != null) {
            if (permissions == null || !permissions.contains(rp.value())) {
                writeFail(response, ErrorCode.FORBIDDEN, "无权限：" + rp.value());
                return false;
            }
        }

        return true;
    }

    private void writeFail(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }
}
