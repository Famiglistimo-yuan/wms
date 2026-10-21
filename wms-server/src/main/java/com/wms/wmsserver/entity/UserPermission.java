package com.wms.wmsserver.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@TableName("rg2402_11_12_13_user_permission")
public class UserPermission {
    private Long userId;
    private Long permId;
    /** allow=授予，deny=禁止；有效权限 = allow − deny，不含角色继承 */
    private String effect;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPermId() { return permId; }
    public void setPermId(Long permId) { this.permId = permId; }
    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
}
