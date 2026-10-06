package com.wms.wmsserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 静态资源映射（FR-6）：升级包目录外置到文件系统，发客户端新版只换文件、
 * 服务端 jar 不含升级包无需重建。不用 spring.web.resources.static-locations
 * 属性的原因：相对 file: URL 的解析依赖进程 CWD（IDEA/mvn/脚本启动各不同），
 * 代码方式用 toAbsolutePath 落定并在日志暴露真实位置，可观察。
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of("downloads").toAbsolutePath();
        log.info("FR-6 升级包目录（/download/** → file:{}）", dir);
        registry.addResourceHandler("/download/**")
                .addResourceLocations("file:" + dir + "/")
                .setCachePeriod(0);
    }
}
