package com.wms.wmsserver.reports.render;

import com.wms.wmsserver.reports.dto.ReportModel;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Excel(.xlsx) 渲染器：报表头（标题/元信息）+ 全边框表格（FR-5-5）。
 */
final class ExcelTableRenderer {

    private ExcelTableRenderer() {
    }

    static byte[] render(ReportModel model) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet(model.getSheetName() == null ? "报表" : model.getSheetName());
            int cols = model.getHeaders().size();

            XSSFCellStyle titleStyle = borderedStyle(wb, HorizontalAlignment.CENTER);
            titleStyle.setFont(boldFont(wb, 14));
            XSSFCellStyle metaStyle = wb.createCellStyle();
            metaStyle.setFont(plainFont(wb, 10));
            XSSFCellStyle headerStyle = borderedStyle(wb, HorizontalAlignment.CENTER);
            headerStyle.setFont(boldFont(wb, 11));
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFCellStyle[] dataStyles = new XSSFCellStyle[3]; // 按 align 索引：0 left 1 center 2 right
            dataStyles[0] = borderedStyle(wb, HorizontalAlignment.LEFT);
            dataStyles[1] = borderedStyle(wb, HorizontalAlignment.CENTER);
            dataStyles[2] = borderedStyle(wb, HorizontalAlignment.RIGHT);

            int r = 0;
            // 报表头：标题行（合并居中）+ 元信息行（FR-5-5）
            XSSFRow titleRow = sheet.createRow(r++);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(model.getTitle());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, cols - 1));
            titleRow.setHeightInPoints(26);
            for (String line : model.getMetaLines()) {
                XSSFRow row = sheet.createRow(r++);
                row.createCell(0).setCellValue(line);
                row.getCell(0).setCellStyle(metaStyle);
            }
            sheet.createRow(r++); // 空行分隔报表头与表格

            // 表头行
            XSSFRow headerRow = sheet.createRow(r++);
            headerRow.setHeightInPoints(20);
            for (int c = 0; c < cols; c++) {
                XSSFCell cell = headerRow.createCell(c);
                cell.setCellValue(model.getHeaders().get(c));
                cell.setCellStyle(headerStyle);
            }

            // 数据行（全边框，FR-5-5）
            for (String[] vals : model.getRows()) {
                XSSFRow row = sheet.createRow(r++);
                for (int c = 0; c < cols; c++) {
                    XSSFCell cell = row.createCell(c);
                    cell.setCellValue(c < vals.length ? vals[c] : "");
                    cell.setCellStyle(dataStyles[alignIndex(model, c)]);
                }
            }

            // 列宽（字符数 × 256，数量列右对齐已在样式处理）
            for (int c = 0; c < cols; c++) {
                sheet.setColumnWidth(c, model.getWidths().get(c) * 256);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static int alignIndex(ReportModel model, int col) {
        String align = col < model.getAligns().size() ? model.getAligns().get(col) : "left";
        return "right".equals(align) ? 2 : "center".equals(align) ? 1 : 0;
    }

    private static XSSFCellStyle borderedStyle(XSSFWorkbook wb, HorizontalAlignment hAlign) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(hAlign);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static XSSFFont boldFont(XSSFWorkbook wb, int size) {
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) size);
        return font;
    }

    private static XSSFFont plainFont(XSSFWorkbook wb, int size) {
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) size);
        return font;
    }
}
