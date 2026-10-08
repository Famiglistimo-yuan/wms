package com.wms.wmsserver.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 给用户分配角色：userId + 要分配的角色 id 列表（全量覆盖，不是增量）。
 */
@Data
public class AssignRolesDTO {

    @NotNull(message = "userId 不能为空")
    private Long userId;

    /** 该用户应拥有的全部角色 id（null 或空列表表示清除所有角色） */
    private List<Long> roleIds;
}
