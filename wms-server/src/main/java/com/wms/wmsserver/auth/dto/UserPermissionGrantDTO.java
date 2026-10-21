package com.wms.wmsserver.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户直接授权请求（全量覆盖）：allow=授予列表，deny=禁止列表。
 * 同一 permId 不应同时出现在两者，否则后主键冲突按异常处理。
 */
@Data
public class UserPermissionGrantDTO {
    private List<Long> allow;
    private List<Long> deny;
}
