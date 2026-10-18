package com.wms.wmsserver.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 人员下拉选项 VO：新增用户时选关联人员，只暴露编号+姓名。
 */
@Getter
@Builder
public class PersonOptionVO {
    private Long id;
    private String personCode;
    private String realName;
}
