package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员下拉选项 VO：新增用户时选关联人员，只暴露编号+姓名，两端共享。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonOptionVO {
    private Long id;
    private String personCode;
    private String realName;
}
