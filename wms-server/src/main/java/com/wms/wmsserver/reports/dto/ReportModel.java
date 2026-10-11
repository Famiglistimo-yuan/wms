package com.wms.wmsserver.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 报表渲染模型（ADR-009：数据组装与渲染分离）：服务端只组装一份行数据，
 * Excel/Word 两个渲染器共用——「报表头 + 边框 + 排版」由此统一保证（FR-5-5）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportModel {

    /** 报表标题（合并居中行） */
    private String title;

    /** 报表头元信息行：制表人、生成时间、物料摘要等 */
    private List<String> metaLines;

    /** 表头列名 */
    private List<String> headers;

    /** 各列宽（字符数，Excel 列宽 / Word 比例参考） */
    private List<Integer> widths;

    /** 各列对齐：left / center / right（数量列右对齐，排版合理） */
    private List<String> aligns;

    /** 数据行（字符串形式，空报表传空列表） */
    private List<String[]> rows;

    /** Excel 工作表名（可空，缺省「报表」） */
    private String sheetName;
}
