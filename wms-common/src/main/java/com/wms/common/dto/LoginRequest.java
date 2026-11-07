package com.wms.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（两端共享契约，CONTRIBUTING §4.4）。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 口令明文（仅开发联调用，生产路径走 encryptedPassword；AuthService 校验两者至少一个非空） */
    private String password;

    /** 口令密文（RSA-2048/OAEP 加密后 Base64；生产路径必填） */
    private String encryptedPassword;
}
