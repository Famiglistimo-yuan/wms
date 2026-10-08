package com.wms.wmsserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（wms.jwt.secret 存在 application-local.yaml，不入库）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wms.jwt")
public class JwtProperties {

    /** 签名密钥（base64 字符串，≥ 256 bit） */
    private String secret = "wms-default-secret-key-change-in-production-please-32b-min";

    /** token 有效期（小时） */
    private int expireHours = 24;
}
