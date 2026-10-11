package com.wms.wmsserver.reports.render;

import com.wms.wmsserver.reports.dto.ReportModel;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 渲染分发（ADR-009）：同一份 ReportModel 按 format 参数选渲染器。
 */
public final class ReportRenderers {

    private ReportRenderers() {
    }

    public static byte[] render(ReportModel model, String format) throws IOException {
        return "word".equalsIgnoreCase(format)
                ? WordTableRenderer.render(model)
                : ExcelTableRenderer.render(model);
    }

    /** 去掉 BigDecimal 多余的小数零（800.000 → 800），报表展示用 */
    public static String strip(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
