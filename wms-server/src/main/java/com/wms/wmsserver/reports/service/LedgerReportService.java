package com.wms.wmsserver.reports.service;

import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.dto.LedgerPreview;
import com.wms.common.dto.LedgerPreviewRow;
import com.wms.wmsserver.mapper.ReportMapper;
import com.wms.wmsserver.reports.dto.LedgerRow;
import com.wms.wmsserver.reports.dto.MaterialBrief;
import com.wms.wmsserver.reports.dto.ReportModel;
import com.wms.wmsserver.reports.render.ReportRenderers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 仓库账本报表（FR-5-3）：给定年份与物料，按日期列出进仓、出仓与结存。
 * 结存口径为期初反推：期初 = 当前库存 −（查询年 1 月 1 日至今的净变动），
 * 结存 = 期初 + 累计进 − 累计出，与实物库存对得上（口径讨论进报告）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerReportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportMapper reportMapper;

    public byte[] build(String materialCode, int year, String format, String username) {
        MaterialBrief brief = reportMapper.selectMaterialBrief(materialCode);
        if (brief == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "物料不存在：" + materialCode);
        }
        LocalDate from = LocalDate.of(year, 1, 1);
        List<LedgerRow> rows = reportMapper.selectLedgerRows(brief.getId(), from, from.plusYears(1));
        BigDecimal opening = brief.getStock().subtract(reportMapper.selectNetFlowSince(brief.getId(), from));
        List<String[]> display = accumulate(opening, rows).stream()
                .map(r -> new String[]{r.getOrderDate(), r.getInQty(), r.getOutQty(), r.getBalance()})
                .toList();

        ReportModel model = ReportModel.builder()
                .title("仓库账本（" + year + "年）")
                .metaLines(List.of(
                        "物料代码：" + brief.getMaterialCode() + "    物料名称：" + brief.getName(),
                        "规格型号：" + (brief.getSpec() == null ? "—" : brief.getSpec())
                                + "    计量单位：" + brief.getUnit(),
                        "期初结存：" + ReportRenderers.strip(opening) + " " + brief.getUnit(),
                        "制表人：" + username + "    生成时间：" + LocalDateTime.now().format(TIME_FORMAT),
                        rows.isEmpty() ? "本年度该物料无进出仓流水" : "共 " + rows.size() + " 日流水"))
                .headers(List.of("日期", "进仓数量", "出仓数量", "结存（" + brief.getUnit() + "）"))
                .widths(List.of(14, 14, 14, 16))
                .aligns(List.of("center", "right", "right", "right"))
                .rows(display)
                .sheetName("仓库账本" + year)
                .build();

        log.info("仓库账本导出 {} {} 年：{} 日流水，期初 {}，格式 {}",
                materialCode, year, rows.size(), opening, format);
        return ReportRenderers.renderOrThrow(model, format);
    }

    /** 导出前预览：与 build() 相同的期初反推与逐日累计，随后按页切片（全年 ≤366 行，无需 SQL 分页） */
    public LedgerPreview preview(String materialCode, int year, long page, int size) {
        MaterialBrief brief = reportMapper.selectMaterialBrief(materialCode);
        if (brief == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "物料不存在：" + materialCode);
        }
        LocalDate from = LocalDate.of(year, 1, 1);
        List<LedgerRow> rows = reportMapper.selectLedgerRows(brief.getId(), from, from.plusYears(1));
        BigDecimal opening = brief.getStock().subtract(reportMapper.selectNetFlowSince(brief.getId(), from));

        List<LedgerPreviewRow> all = accumulate(opening, rows);
        int fromIdx = (int) Math.min((page - 1) * size, all.size());
        int toIdx = (int) Math.min(fromIdx + (long) size, all.size());
        return LedgerPreview.builder()
                .materialCode(brief.getMaterialCode())
                .materialName(brief.getName())
                .spec(brief.getSpec() == null ? "—" : brief.getSpec())
                .unit(brief.getUnit())
                .openingBalance(ReportRenderers.strip(opening))
                .records(all.subList(fromIdx, toIdx))
                .total(all.size())
                .page(page)
                .size(size)
                .build();
    }

    /** 期初反推后的逐日累计展示行：build 与 preview 共用（口径单源） */
    private static List<LedgerPreviewRow> accumulate(BigDecimal opening, List<LedgerRow> rows) {
        List<LedgerPreviewRow> all = new ArrayList<>(rows.size());
        BigDecimal balance = opening;
        for (LedgerRow r : rows) {
            balance = balance.add(r.getInQty()).subtract(r.getOutQty());
            all.add(LedgerPreviewRow.builder()
                    .orderDate(r.getOrderDate().toString())
                    .inQty(ReportRenderers.strip(r.getInQty()))
                    .outQty(ReportRenderers.strip(r.getOutQty()))
                    .balance(ReportRenderers.strip(balance))
                    .build());
        }
        return all;
    }
}
