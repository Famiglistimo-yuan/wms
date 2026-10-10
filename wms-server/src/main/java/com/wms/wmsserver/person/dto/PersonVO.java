package com.wms.wmsserver.person.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员列表/详情 VO（对外展示，不返回口令类敏感字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonVO {

    private Long id;
    private String personCode;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String idCard;
    private String hometown;
    private String address;
    private String phone;
    private String remark;
    private LocalDateTime createdAt;
}
