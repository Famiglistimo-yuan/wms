package com.wms.wmsclient.dto;

/** 人员下拉选项 VO */
public class PersonOptionVO {
    private Long id;
    private String personCode;
    private String realName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPersonCode() { return personCode; }
    public void setPersonCode(String personCode) { this.personCode = personCode; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    @Override
    public String toString() {
        return personCode + " - " + realName;
    }
}
