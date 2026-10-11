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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Word(.docx) 渲染器：报表头（标题/元信息段落）+ 全边框表格（FR-5-5）。
 *
 * <p>列宽按<strong>实际内容宽度</strong>自适应（表头与数据逐列取最大值估算），
 * 不用 ReportModel.widths——那是 Excel 字符宽口径（1 字符 ≈ 256 dxa），直接压进
 * A4 页宽会把所有列挤到竖排换行（已踩坑）。内容总宽超过 A4 纵向可用宽度时整页
 * 自动转横向（月度单 10 列场景），保证排版合理（FR-5-5）。
 */
final class WordTableRenderer {

    /** A4 纵向可用宽度（dxa，1/20 磅）：页宽 11906 − 左右页边距 2×1440 */
    private static final int PORTRAIT_DXA = 9026;
    /** A4 横向可用宽度（dxa）：页宽 16838 − 左右页边距 2×1440 */
    private static final int LANDSCAPE_DXA = 13958;
    /** 单元格左右内边距 + 边框余量（dxa） */
    private static final int CELL_PADDING = 300;
    /** 10pt 字符宽近似（dxa）：CJK 全角 ≈ 210，ASCII 半角 ≈ 105 */
    private static final int CJK_CHAR = 210;
    private static final int ASCII_CHAR = 105;
    /** 压缩后的列宽下限，低于此值宁可转横向/换行 */
    private static final int MIN_COL_DXA = 500;

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

            // 列宽：内容自适应，宽表自动转横向页面
            int[] desired = desiredWidths(model);
            int need = Arrays.stream(desired).sum();
            int pageDxa = need > PORTRAIT_DXA ? LANDSCAPE_DXA : PORTRAIT_DXA;
            if (pageDxa == LANDSCAPE_DXA) {
                applyLandscape(doc);
            }
            int[] colDxa = fitWidths(desired, pageDxa);

            int cols = model.getHeaders().size();
            XWPFTable table = doc.createTable(1 + model.getRows().size(), cols);
            table.setWidthType(TableWidthType.DXA);
            table.setWidth(pageDxa);
            // 固定布局：列宽按 tcW 硬约束，不随内容自动伸缩（自动布局会挤压数字列）
            table.getCTTbl().getTblPr().addNewTblLayout().setType(STTblLayoutType.FIXED);
            applyBorders(table);

            // 表头行：居中加粗
            XWPFTableRow headerRow = table.getRow(0);
            for (int c = 0; c < cols; c++) {
                fillCell(headerRow.getCell(c), model.getHeaders().get(c),
                        ParagraphAlignment.CENTER, true, colDxa[c]);
            }
            // 数据行
            for (int i = 0; i < model.getRows().size(); i++) {
                XWPFTableRow row = table.getRow(i + 1);
                String[] vals = model.getRows().get(i);
                for (int c = 0; c < cols; c++) {
                    fillCell(row.getCell(c), c < vals.length ? vals[c] : "",
                            alignOf(model, c), false, colDxa[c]);
                }
            }

            doc.write(out);
            return out.toByteArray();
        }
    }

    /** 逐列取「表头与所有数据行的最大内容宽度 + 内边距」，单列封顶半页 */
    private static int[] desiredWidths(ReportModel model) {
        int cols = model.getHeaders().size();
        int[] result = new int[cols];
        for (int c = 0; c < cols; c++) {
            int max = textDxa(model.getHeaders().get(c));
            for (String[] row : model.getRows()) {
                if (c < row.length && row[c] != null) {
                    max = Math.max(max, textDxa(row[c]));
                }
            }
            result[c] = Math.min(max + CELL_PADDING, PORTRAIT_DXA / 2);
        }
        return result;
    }

    /** 近似估算串宽（dxa）：非 ASCII 按全角、ASCII 按半角 */
    private static int textDxa(String s) {
        int dxa = 0;
        for (int i = 0; i < s.length(); i++) {
            dxa += s.charAt(i) > 127 ? CJK_CHAR : ASCII_CHAR;
        }
        return dxa;
    }

    /**
     * 把期望列宽适配进页面可用宽度：
     * 有富余按比例摊给各列（列宽更舒展）；超页按比例压缩，压缩后仍低于下限的列保底。
     * 最后一列吸收取整余数，保证总和恰等于页面宽度。
     */
    private static int[] fitWidths(int[] desired, int pageDxa) {
        int cols = desired.length;
        int[] result = new int[cols];
        long sum = Arrays.stream(desired).sum();
        long acc = 0;
        for (int c = 0; c < cols; c++) {
            result[c] = (int) ((long) desired[c] * pageDxa / sum);
            acc += result[c];
        }
        result[cols - 1] += (int) (pageDxa - acc);
        // 保底：压缩过狠的列提到下限（总和可能略超页宽，由 Word 固定布局吸收，可接受）
        for (int c = 0; c < cols; c++) {
            if (result[c] < MIN_COL_DXA) {
                result[c] = MIN_COL_DXA;
            }
        }
        return result;
    }

    /** 页面转横向：A4 宽高互换 + orient 属性 */
    private static void applyLandscape(XWPFDocument doc) {
        CTSectPr sect = doc.getDocument().getBody().isSetSectPr()
                ? doc.getDocument().getBody().getSectPr()
                : doc.getDocument().getBody().addNewSectPr();
        var pgSz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
        pgSz.setW(BigInteger.valueOf(16838));
        pgSz.setH(BigInteger.valueOf(11906));
        pgSz.setOrient(STPageOrientation.LANDSCAPE);
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

    private static void fillCell(XWPFTableCell cell, String text, ParagraphAlignment align,
                                 boolean bold, int widthDxa) {
        cell.setWidthType(TableWidthType.DXA);
        cell.setWidth(String.valueOf(widthDxa));
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
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
