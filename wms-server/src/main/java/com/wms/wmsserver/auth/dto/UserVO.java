package com.wms.wmsserver.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户列表/详情 VO（不返回口令）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO {

    private Long id;
    private String username;
    private Long personId;
    private String personCode;
    private String realName;
    private Integer status; // 1=启用 0=停用
}
