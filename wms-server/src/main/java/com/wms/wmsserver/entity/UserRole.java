package com.wms.wmsserver.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@TableName("rg2402_11_12_13_user_role")
public class UserRole {
    private Long userId;
    private Long roleId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
