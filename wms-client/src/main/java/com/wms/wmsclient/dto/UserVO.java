package com.wms.wmsclient.dto;

/** 用户管理列表行 VO */
public class UserVO {
    private Long id;
    private String username;
    private Long personId;
    private String personCode;
    private String realName;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public String getPersonCode() { return personCode; }
    public void setPersonCode(String personCode) { this.personCode = personCode; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
