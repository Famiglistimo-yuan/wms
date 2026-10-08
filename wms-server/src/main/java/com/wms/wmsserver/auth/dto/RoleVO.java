package com.wms.wmsserver.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleVO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String remark;
    /** 该角色已分配的权限码列表 */
    private java.util.List<String> permCodes;
}
