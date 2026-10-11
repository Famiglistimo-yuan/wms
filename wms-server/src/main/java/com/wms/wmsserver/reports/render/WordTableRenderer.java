package com.wms.wmsserver.reports.render;

import com.wms.wmsserver.reports.dto.ReportModel;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Word(.docx) 渲染器：报表头（标题/元信息段落）+ 全边框表格（FR-5-5）。
 * 边框走 XWPFTable 公开 API（setTopBorder 等），不碰底层 CT 对象。
 */
final class WordTableRenderer {

    private WordTableRenderer() {
    }

    static byte[] render(ReportModel model) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 报表头：标题段落（居中加粗）+ 元信息段落
            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleP.createRun();
            titleRun.setText(model.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            for (String line : model.getMetaLines()) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line);
                run.setFontSize(9);
            }
            doc.createParagraph(); // 空段落分隔报表头与表格

            int cols = model.getHeaders().size();
            XWPFTable table = doc.createTable(1 + model.getRows().size(), cols);
            table.setWidthType(TableWidthType.PCT);
            table.setWidth("100");
            applyBorders(table);

            // 表头行：居中加粗
            XWPFTableRow headerRow = table.getRow(0);
            for (int c = 0; c < cols; c++) {
                fillCell(headerRow.getCell(c), model.getHeaders().get(c), ParagraphAlignment.CENTER, true);
            }
            // 数据行
            for (int i = 0; i < model.getRows().size(); i++) {
                XWPFTableRow row = table.getRow(i + 1);
                String[] vals = model.getRows().get(i);
                for (int c = 0; c < cols; c++) {
                    fillCell(row.getCell(c), c < vals.length ? vals[c] : "",
                            alignOf(model, c), false);
                }
            }

            doc.write(out);
            return out.toByteArray();
        }
    }

    private static void applyBorders(XWPFTable table) {
        XWPFTable.XWPFBorderType single = XWPFTable.XWPFBorderType.SINGLE;
        table.setTopBorder(single, 4, 0, "000000");
        table.setBottomBorder(single, 4, 0, "000000");
        table.setLeftBorder(single, 4, 0, "000000");
        table.setRightBorder(single, 4, 0, "000000");
        table.setInsideHBorder(single, 4, 0, "000000");
        table.setInsideVBorder(single, 4, 0, "000000");
    }

    private static void fillCell(XWPFTableCell cell, String text, ParagraphAlignment align, boolean bold) {
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setAlignment(align);
        XWPFRun run = p.createRun();
        run.setText(text == null ? "" : text);
        run.setBold(bold);
        run.setFontSize(10);
    }

    private static ParagraphAlignment alignOf(ReportModel model, int col) {
        String align = col < model.getAligns().size() ? model.getAligns().get(col) : "left";
        return "right".equals(align) ? ParagraphAlignment.RIGHT
                : "center".equals(align) ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT;
    }
}
