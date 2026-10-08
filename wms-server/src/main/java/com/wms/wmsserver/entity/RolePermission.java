package com.wms.wmsserver.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@TableName("rg2402_11_12_13_role_permission")
public class RolePermission {
    private Long roleId;
    private Long permId;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getPermId() { return permId; }
    public void setPermId(Long permId) { this.permId = permId; }
}
