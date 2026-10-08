package com.wms.wmsserver.config;

import com.wms.wmsserver.auth.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 静态资源映射（FR-6）+ JWT 拦截器注册（FR-4）。
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** FR-6：升级包目录外置到文件系统 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of("downloads").toAbsolutePath();
        log.info("FR-6 升级包目录（/download/** → file:{}）", dir);
        registry.addResourceHandler("/download/**")
                .addResourceLocations("file:" + dir + "/")
                .setCachePeriod(0);
    }

    /** FR-4：JWT 拦截器；放行登录/注销接口 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/version");
    }
}
