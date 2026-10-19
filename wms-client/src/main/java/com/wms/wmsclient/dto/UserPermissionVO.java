package com.wms.wmsclient.dto;

/** 用户授权回显 VO：直授 allow 集 + 禁止 deny 集 */
public class UserPermissionVO {
    private java.util.List<Long> allowPermIds;
    private java.util.List<Long> denyPermIds;

    public java.util.List<Long> getAllowPermIds() { return allowPermIds; }
    public void setAllowPermIds(java.util.List<Long> allowPermIds) { this.allowPermIds = allowPermIds; }
    public java.util.List<Long> getDenyPermIds() { return denyPermIds; }
    public void setDenyPermIds(java.util.List<Long> denyPermIds) { this.denyPermIds = denyPermIds; }
}
