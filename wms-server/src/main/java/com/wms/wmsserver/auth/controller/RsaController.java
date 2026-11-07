package com.wms.wmsserver.auth.controller;

import com.wms.common.Result;
import com.wms.wmsserver.auth.config.RsaKeyHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RSA 公钥下发接口。公开端点（WebMvcConfig JWT 白名单放行）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RsaController {

    private final RsaKeyHolder rsaKeyHolder;

    /** GET /api/auth/public-key：返回 Base64 公钥供客户端加密口令 */
    @GetMapping("/public-key")
    public Result<Map<String, String>> publicKey() {
        return Result.success(Map.of(
                "publicKey", rsaKeyHolder.publicKeyBase64(),
                "algorithm", "RSA",
                "keySize", "2048"
        ));
    }
}
