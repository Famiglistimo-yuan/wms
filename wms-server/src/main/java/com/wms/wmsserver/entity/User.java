package com.wms.wmsserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录用户实体（@TableName 直书完整表名，含 rg2402_11_12_13_ 前缀；
 * MP 的 table-prefix 对显式 @TableName 不生效，项目不配置该参数）。
 */
@NoArgsConstructor
@TableName("rg2402_11_12_13_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long personId;
    private String username;
    private String password;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
