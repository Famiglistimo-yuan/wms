package com.wms.wmsserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员档案实体（@TableName 直书完整表名；MP table-prefix 对显式 @TableName 不生效）。
 */
@NoArgsConstructor
@TableName("rg2402_11_12_13_person")
public class Person {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 人员代码（唯一，FR-1-2-2 非空） */
    private String personCode;
    /** 姓名（FR-1-2-7 按单字模糊查询） */
    private String name;
    /** 性别（'男' / '女'，FR-1-2-3 界面单选不自由输入） */
    private String gender;
    /** 出生日期（FR-1-2-4 日期控件） */
    private LocalDate birthDate;
    /** 身份证号（FR-1-2-5 正则校验：17 位数字 + 1 位数字或 X/x） */
    private String idCard;
    private String hometown;
    private String address;
    private String phone;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPersonCode() { return personCode; }
    public void setPersonCode(String personCode) { this.personCode = personCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getHometown() { return hometown; }
    public void setHometown(String hometown) { this.hometown = hometown; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
