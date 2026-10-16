package com.wms.wmsserver.person.dto;

import com.wms.common.RegexPatterns;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改人员请求：null 字段表示不改（人员代码不允许改，改代码应删除重建）。
 */
@Data
public class PersonUpdateDTO {

    private String name;

    @Pattern(regexp = "^$|^(男|女)$", message = "性别只能为男或女")
    private String gender;

    private LocalDate birthDate;

    @Pattern(regexp = "^$|" + RegexPatterns.ID_CARD, message = "身份证号须为 18 位，最后一位为数字或 X")
    private String idCard;

    private String hometown;
    private String address;
    private String phone;
    private String remark;
}
