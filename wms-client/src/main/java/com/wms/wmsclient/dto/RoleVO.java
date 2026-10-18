package com.wms.wmsclient.dto;

/** 角色 VO */
public class RoleVO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String remark;
    private java.util.List<String> permCodes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public java.util.List<String> getPermCodes() { return permCodes; }
    public void setPermCodes(java.util.List<String> permCodes) { this.permCodes = permCodes; }

    @Override
    public String toString() { return roleName + " (" + roleCode + ")"; }
}
