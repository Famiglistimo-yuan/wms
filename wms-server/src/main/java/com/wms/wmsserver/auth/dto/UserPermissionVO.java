package com.wms.wmsserver.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户授权回显：角色继承的权限 id 集 + 直授 allow 集 + 禁止 deny 集。
 * 有效权限 = (rolePermIds ∪ allowPermIds) − denyPermIds。
 */
@Data
@Builder
public class UserPermissionVO {
    private List<Long> rolePermIds;
    private List<Long> allowPermIds;
    private List<Long> denyPermIds;
}
