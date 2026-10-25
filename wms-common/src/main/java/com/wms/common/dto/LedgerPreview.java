package com.wms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仓库账本预览响应（FR-5-3）：物料摘要 + 期初结存 + 分页行（records/total/page/size，口径同 PageResult）。
 * 分页字段拍平在本对象而不嵌 PageResult，避免泛型嵌套在客户端 Jackson 反序列化时被擦除。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerPreview {

    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private String openingBalance;
    private List<LedgerPreviewRow> records;
    private long total;
    private long page;
    private long size;
}
