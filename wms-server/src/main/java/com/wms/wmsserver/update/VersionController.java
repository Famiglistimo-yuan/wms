package com.wms.wmsserver.update;

import com.wms.common.Result;
import com.wms.common.VersionInfo;
import com.wms.wmsserver.update.config.UpdateProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 版本检测接口（FR-6，技术方案 §6.2）：登录前调用，公开接口——
 * 已在 JwtInterceptor 豁免清单登记（WebMvcConfig excludePathPatterns）。
 */
@RestController
@RequiredArgsConstructor
public class VersionController {

    private final UpdateProperties updateProperties;

    @GetMapping("/api/version")
    public Result<VersionInfo> version() {
        VersionInfo info = new VersionInfo();
        info.setVersion(updateProperties.getVersion());
        info.setDownloadUrl(updateProperties.getDownloadUrl());
        info.setMd5(updateProperties.getMd5());
        return Result.success(info);
    }
}
