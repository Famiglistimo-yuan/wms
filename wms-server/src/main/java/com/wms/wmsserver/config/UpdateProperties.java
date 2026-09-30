package com.wms.wmsserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自动升级发布信息（FR-6）：绑定 application.yaml 的 wms-update 块，
 * 发版时改配置 + 换 static/download/ 下的升级包即可，无需改代码。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wms-update")
public class UpdateProperties {

    /** 最新版本号（三段数字） */
    private String version;

    /** 升级包下载地址（相对路径，指向 static/ 下文件） */
    private String downloadUrl;

    /** 升级包 MD5 指纹 */
    private String md5;
}
