package com.wms.wmsserver.reports.service;

import com.wms.common.PageResult;
import com.wms.common.dto.MonthlyOrderPreviewRow;
import com.wms.wmsserver.mapper.ReportMapper;
import com.wms.wmsserver.reports.dto.MonthlyOrderRow;
import com.wms.wmsserver.reports.dto.ReportModel;
import com.wms.wmsserver.reports.render.ReportRenderers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 月度进出仓单报表（FR-5-2）：给定月份，该月单据全部按单号顺序导出（Excel/Word）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyOrderReportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportMapper reportMapper;

    public byte[] build(int year, int month, String format, String username) {
        LocalDate from = LocalDate.of(year, month, 1);
        List<MonthlyOrderRow> rows = reportMapper.selectMonthlyOrders(from, from.plusMonths(1));

        List<String[]> display = rows.stream().map(r -> new String[]{
                r.getOrderNo(),
                "IN".equals(r.getOrderType()) ? "进仓" : "出仓",
                r.getOrderDate().toString(),
                r.getOperatorName(),
                r.getHandlerName(),
                r.getMaterialCode(),
                r.getMaterialName(),
                r.getUnit(),
                ReportRenderers.strip(r.getQuantity()),
                r.getRemark() == null ? "" : r.getRemark()
        }).toList();

        ReportModel model = ReportModel.builder()
                .title(String.format("%d年%02d月 进出仓单", year, month))
                .metaLines(List.of(
                        "制表人：" + username,
                        "生成时间：" + LocalDateTime.now().format(TIME_FORMAT),
                        rows.isEmpty() ? "本月无进出仓单据" : "共 " + display.size() + " 条明细，按单号顺序排列"))
                .headers(List.of("单号", "类型", "日期", "操作人", "经手人",
                        "物料代码", "物料名称", "单位", "数量", "备注"))
                .widths(List.of(16, 7, 12, 10, 10, 11, 14, 7, 10, 22))
                .aligns(List.of("left", "center", "center", "left", "left",
                        "left", "left", "center", "right", "left"))
                .rows(display)
                .sheetName(String.format("%d%02d进出仓单", year, month))
                .build();

        log.info("月度进出仓单导出 {}-{}：{} 条明细，格式 {}", year, month, display.size(), format);
        return ReportRenderers.renderOrThrow(model, format);
    }

    /** 导出前预览：与 build() 同查询同排序同展示口径，按页返回（百万级明细不可全量拉取） */
    public PageResult<MonthlyOrderPreviewRow> preview(int year, int month, long page, int size) {
        LocalDate from = LocalDate.of(year, month, 1);
        long total = reportMapper.countMonthlyOrders(from, from.plusMonths(1));
        List<MonthlyOrderPreviewRow> records = List.of();
        if ((page - 1) * size < total) { // 页越界（如数据刚被删）返回空页而非报错
            records = reportMapper.selectMonthlyOrdersPage(from, from.plusMonths(1), (page - 1) * size, size)
                    .stream()
                    .map(r -> MonthlyOrderPreviewRow.builder()
                            .orderNo(r.getOrderNo())
                            .typeLabel("IN".equals(r.getOrderType()) ? "进仓" : "出仓")
                            .orderDate(r.getOrderDate().toString())
                            .operatorName(r.getOperatorName())
                            .handlerName(r.getHandlerName())
                            .materialCode(r.getMaterialCode())
                            .materialName(r.getMaterialName())
                            .unit(r.getUnit())
                            .quantity(ReportRenderers.strip(r.getQuantity()))
                            .remark(r.getRemark() == null ? "" : r.getRemark())
                            .build())
                    .toList();
        }
        return new PageResult<>(records, total, page, size);
    }
}
