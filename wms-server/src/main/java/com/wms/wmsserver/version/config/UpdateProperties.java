package com.wms.wmsserver.version.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自动升级发布信息（FR-6）：绑定 application.yaml 的 wms-update 块，
 * 发版时改配置 + 换 static/download/ 下的升级包即可，无需改代码。
 */
@Getter
@Setter
@Slf4j
@Component
@ConfigurationProperties(prefix = "wms-update")
public class UpdateProperties {

    /** 最新版本号（三段数字） */
    private String version;

    /** 升级包下载地址（相对路径，指向 static/ 下文件） */
    private String downloadUrl;

    /** 升级包 MD5 指纹 */
    private String md5;

    /** 发布配置自检：字段不全时告警留痕——客户端会安全跳过升级，但服务端应有线索可查 */
    @PostConstruct
    void warnIfIncomplete() {
        if (version == null || version.isBlank()
                || downloadUrl == null || downloadUrl.isBlank()
                || md5 == null || md5.isBlank()) {
            log.warn("wms-update 发布配置不完整（version/download-url/md5 有空值），客户端将跳过升级检查");
        }
    }
}
