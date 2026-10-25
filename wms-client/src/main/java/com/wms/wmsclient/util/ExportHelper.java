package com.wms.wmsclient.util;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表导出辅助（FR-5）：两个报表面板共用的「FileChooser 保存」、格式转换与导出状态。
 */
public final class ExportHelper {

    /** 导出格式选项：界面文案 → format 参数值 / 扩展名 */
    private static final List<String> FORMAT_LABELS = List.of("Excel (.xlsx)", "Word (.docx)");

    private ExportHelper() {
    }

    public static List<String> formatLabels() {
        return FORMAT_LABELS;
    }

    /** "Excel (.xlsx)" → "excel"；"Word (.docx)" → "word" */
    public static String formatKey(String label) {
        return label != null && label.startsWith("Word") ? "word" : "excel";
    }

    /** "excel" → ".xlsx"；"word" → ".docx" */
    public static String extensionOf(String formatKey) {
        return "word".equals(formatKey) ? ".docx" : ".xlsx";
    }

    /** 任务异常的展示文案（根因消息，无则「未知错误」） */
    public static String rootMessage(Throwable ex) {
        return ex == null ? "未知错误" : ex.getMessage();
    }

    /** 年份下拉填充：当前年−5…+1，默认当前年（两个报表面板共用） */
    public static void fillYears(ChoiceBox<Integer> cbYear) {
        int thisYear = LocalDate.now().getYear();
        for (int y = thisYear - 5; y <= thisYear + 1; y++) {
            cbYear.getItems().add(y);
        }
        cbYear.setValue(thisYear);
    }

    /**
     * 保存导出文件并生成状态栏文案（「已导出：路径」/「已取消保存」）。
     * 写盘失败弹窗提示后返回 null，调用方跳过状态栏更新。
     */
    public static String saveAndDescribe(Window owner, String suggestedName, String extension, byte[] bytes) {
        try {
            File saved = save(owner, suggestedName, extension, bytes);
            return saved == null ? "已取消保存" : "已导出：" + saved.getAbsolutePath();
        } catch (UncheckedIOException ex) {
            Alerts.error("文件保存失败：" + ex.getCause().getMessage());
            return null;
        }
    }

    /**
     * FileChooser 保存字节数组，保存路径由调用方展示在状态栏，不自动打开。
     * 取消选择返回 null；写盘失败抛 UncheckedIOException，由调用方弹窗。
     */
    public static File save(Window owner, String suggestedName, String extension, byte[] bytes) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(suggestedName);
        String label = extension.equalsIgnoreCase(".docx") ? "Word 文档 (*.docx)" : "Excel 文件 (*.xlsx)";
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(label, "*" + extension));
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null;
        }
        try {
            Files.write(file.toPath(), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return file;
    }
}
