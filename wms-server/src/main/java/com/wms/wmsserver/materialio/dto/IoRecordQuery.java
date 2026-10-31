package com.wms.wmsserver.materialio.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出入库流水分页查询参数（post body，所有条件可选）。
 * page 从 1 开始，pageSize 默认 20。
 */
@Data
public class IoRecordQuery {

    /** 页码 从1开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 物料ID筛选 */
    private Long materialId;

    /** 单据类型 1入库 2出库 */
    private Integer orderType;

    /** 起始时间（创建时间 >=） */
    private LocalDateTime startTime;

    /** 截止时间（创建时间 <=） */
    private LocalDateTime endTime;

    /** 关联单据号 模糊匹配（如 RK20261031） */
    private String orderNo;

    /** 操作人 精确匹配 */
    private String operator;
}
