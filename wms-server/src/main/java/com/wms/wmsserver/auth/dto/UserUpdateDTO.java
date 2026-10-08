package com.wms.wmsserver.auth.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改用户请求（status 或 password，不填字段表示不改）。
 */
@Data
public class UserUpdateDTO {

    /** 空表示不改 */
    @Pattern(regexp = "^$|^[a-zA-Z0-9]{6,20}$", message = "口令须为 6-20 位字母/数字")
    private String password;

    /** null 表示不改 */
    private Integer status;
}
