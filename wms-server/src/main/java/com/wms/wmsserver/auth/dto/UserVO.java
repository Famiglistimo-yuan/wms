package com.wms.wmsserver.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** 该用户已分配的角色列表（仅管理界面用） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private java.util.List<RoleLite> roles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleLite {
        private Long id;
        private String roleCode;
        private String roleName;
    }
}
