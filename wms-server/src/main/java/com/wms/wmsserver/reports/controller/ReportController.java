package com.wms.wmsserver.reports.controller;

import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.common.dto.LedgerPreview;
import com.wms.common.dto.MonthlyOrderPreviewRow;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.interceptor.JwtInterceptor;
import com.wms.wmsserver.reports.service.LedgerReportService;
import com.wms.wmsserver.reports.service.MonthlyOrderReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 报表导出接口（FR-5-2 / FR-5-3 / FR-5-4，技术方案 §6.2）。返回文件流
 * （Content-Type: application/octet-stream，附件名 UTF-8）；失败走全局异常处理，
 * 与其他接口一致返回 Result JSON（HTTP 恒 200 契约，客户端按 Content-Type 分流）。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final MonthlyOrderReportService monthlyOrderReportService;
    private final LedgerReportService ledgerReportService;

    /** 月度进出仓单（FR-5-2）：给定月份，该月单据全部按单号顺序导出 */
    @GetMapping("/monthly-orders/{yyyyMM}")
    @RequirePermission("menu.stats.monthly")
    public ResponseEntity<byte[]> monthlyOrders(@PathVariable String yyyyMM,
                                               @RequestParam(defaultValue = "excel") String format,
                                               HttpServletRequest request) {
        int[] ym = parseYm(yyyyMM);
        String fmt = normalizeFormat(format);
        byte[] body = monthlyOrderReportService.build(ym[0], ym[1], fmt, usernameOf(request));
        return file(body, "月度进出仓单_" + yyyyMM + extOf(fmt));
    }

    /** 月度进出仓单预览（FR-5-2 导出前页面展示）：与导出同查询同排序，分页返回 Result JSON */
    @GetMapping("/monthly-orders/{yyyyMM}/preview")
    @RequirePermission("menu.stats.monthly")
    public Result<PageResult<MonthlyOrderPreviewRow>> monthlyOrdersPreview(@PathVariable String yyyyMM,
                                                                           @RequestParam(defaultValue = "1") long page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        int[] ym = parseYm(yyyyMM);
        return Result.success(monthlyOrderReportService.preview(ym[0], ym[1], Math.max(1, page), clampSize(size)));
    }

    /** 仓库账本（FR-5-3）：给定年份与物料，按日期列出进仓、出仓与结存 */
    @GetMapping("/ledger/{materialCode}/{yyyy}")
    @RequirePermission("menu.stats.ledger")
    public ResponseEntity<byte[]> ledger(@PathVariable String materialCode,
                                         @PathVariable String yyyy,
                                         @RequestParam(defaultValue = "excel") String format,
                                         HttpServletRequest request) {
        int year = parseYear(yyyy);
        String fmt = normalizeFormat(format);
        byte[] body = ledgerReportService.build(materialCode, year, fmt, usernameOf(request));
        return file(body, "仓库账本_" + materialCode + "_" + year + extOf(fmt));
    }

    /** 仓库账本预览（FR-5-3 导出前页面展示）：物料摘要 + 期初结存 + 分页行 */
    @GetMapping("/ledger/{materialCode}/{yyyy}/preview")
    @RequirePermission("menu.stats.ledger")
    public Result<LedgerPreview> ledgerPreview(@PathVariable String materialCode,
                                               @PathVariable String yyyy,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") int size) {
        int year = parseYear(yyyy);
        return Result.success(ledgerReportService.preview(materialCode, year, Math.max(1, page), clampSize(size)));
    }

    // ====== 以下为参数与响应辅助 ======

    private static int[] parseYm(String yyyyMM) {
        if (yyyyMM == null || !yyyyMM.matches("\\d{6}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "月份格式须为 yyyyMM（如 202609）");
        }
        int year = Integer.parseInt(yyyyMM.substring(0, 4));
        int month = Integer.parseInt(yyyyMM.substring(4));
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "月份须在 01-12 之间");
        }
        return new int[]{year, month};
    }

    private static int parseYear(String yyyy) {
        if (yyyy == null || !yyyy.matches("\\d{4}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "年份格式须为 yyyy（如 2026）");
        }
        return Integer.parseInt(yyyy);
    }

    private static String normalizeFormat(String format) {
        if (format == null) {
            return "excel";
        }
        String f = format.trim().toLowerCase();
        if (!"excel".equals(f) && !"word".equals(f)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "导出格式仅支持 excel 或 word");
        }
        return f;
    }

    /** 分页 size 钳制（技术方案 §6.3：默认 20，上限 100） */
    private static int clampSize(int size) {
        return Math.min(100, Math.max(1, size));
    }

    private static String usernameOf(HttpServletRequest request) {
        Object username = request.getAttribute(JwtInterceptor.ATTR_USERNAME);
        return username == null ? "unknown" : username.toString();
    }

    private static String extOf(String format) {
        return "word".equals(format) ? ".docx" : ".xlsx";
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename) {
        HttpHeaders headers = new HttpHeaders();
        // RFC 5987：中文附件名 UTF-8 编码，浏览器/客户端均可还原
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
