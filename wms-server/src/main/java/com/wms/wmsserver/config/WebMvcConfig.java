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
        // toUri() 生成合法的 file:/// 绝对 URL（正斜杠，空格/中文自动转义），
        // 直接拼 "file:" + dir 在 Windows 得到 file:C:\...\ 反斜杠非法 URL。
        // 尾斜杠是 ResourceHandler 解析相对路径的目录边界，必须有；
        // 但 toUri() 对「已存在的目录」会自动补 /（不存在时不补），故按条件拼接避免 //
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        log.info("FR-6 升级包目录（/download/** → {}）", location);
        registry.addResourceHandler("/download/**")
                .addResourceLocations(location)
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
