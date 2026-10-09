package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应（两端共享契约，CONTRIBUTING §4.4）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT token，客户端后续请求放 Authorization: Bearer <token> */
    private String token;

    /** 登录用户名 */
    private String username;

    /** 人员代码（关联人员档案） */
    private String personCode;

    /** 真实姓名 */
    private String realName;

    /** 当前用户权限码集合，客户端据此显隐菜单项 */
    private List<String> permissions;
}
