package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户授权回显：直授 allow 集 + 禁止 deny 集，两端共享。
 * 有效权限 = allowPermIds − denyPermIds。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionVO {
    private List<Long> allowPermIds;
    private List<Long> denyPermIds;
}
