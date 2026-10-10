package com.wms.wmsserver.person.dto;

import com.wms.common.RegexPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新增人员请求。FR-1-2-2 人员代码非空；FR-1-2-3 性别男/女；
 * FR-1-2-4 出生日期由客户端 DatePicker 传入；FR-1-2-5 身份证正则校验。
 */
@Data
public class PersonCreateDTO {

    @NotBlank(message = "人员代码不能为空")
    private String personCode;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "性别必须选择")
    @Pattern(regexp = "^(男|女)$", message = "性别只能为男或女")
    private String gender;

    /** 由客户端 DatePicker 控制（FR-1-2-4） */
    private LocalDate birthDate;

    /** 空表示不填；有值时正则校验（FR-1-2-5） */
    @Pattern(regexp = "^$|" + RegexPatterns.ID_CARD, message = "身份证号须为 18 位，最后一位为数字或 X")
    private String idCard;

    private String hometown;
    private String address;
    private String phone;
    private String remark;
}
