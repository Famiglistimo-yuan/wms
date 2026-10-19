package com.wms.wmsserver.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户授权回显：直授 allow 集 + 禁止 deny 集。
 * 有效权限 = allowPermIds − denyPermIds。
 */
@Data
@Builder
public class UserPermissionVO {
    private List<Long> allowPermIds;
    private List<Long> denyPermIds;
}
