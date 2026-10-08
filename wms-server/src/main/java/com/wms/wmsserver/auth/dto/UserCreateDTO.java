package com.wms.wmsserver.auth.dto;

import com.wms.common.RegexPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新增用户请求。
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = RegexPatterns.USERNAME, message = "用户名须字母开头，4-30 位字母/数字/下划线")
    private String username;

    @NotNull(message = "必须关联人员档案")
    private Long personId;

    @NotBlank(message = "初始口令不能为空")
    @Pattern(regexp = RegexPatterns.PASSWORD, message = "口令须为 6-20 位字母/数字")
    private String password;
}
