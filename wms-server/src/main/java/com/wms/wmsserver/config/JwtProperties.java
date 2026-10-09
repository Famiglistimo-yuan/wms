package com.wms.wmsserver.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 配置（wms.jwt.secret 存在 application-local.yaml，不入库）。
 * secret 无默认值：漏配时启动即失败（@Validated 校验），避免静默退化为弱密钥。
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "wms.jwt")
public class JwtProperties {

    /** 签名密钥（base64 字符串，≥ 256 bit；模板见 application-local.yaml.example） */
    @NotBlank(message = "wms.jwt.secret 未配置：请在 application-local.yaml 提供（模板见 application-local.yaml.example）")
    private String secret;

    /** token 有效期（小时） */
    private int expireHours = 24;
}
