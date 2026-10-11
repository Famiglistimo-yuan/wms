package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.ExportHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

import java.io.File;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * 月度进出仓单导出面板（FR-5-2）：选择月份与格式（Excel/Word），导出后保存并打开。
 */
public class ReportMonthlyOrdersController {

    private final App app;

    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<Integer> cbMonth;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private Button btnExport;
    @FXML private Label lblStatus;

    public ReportMonthlyOrdersController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        int thisYear = LocalDate.now().getYear();
        for (int y = thisYear - 5; y <= thisYear + 1; y++) {
            cbYear.getItems().add(y);
        }
        cbYear.setValue(thisYear);
        for (int m = 1; m <= 12; m++) {
            cbMonth.getItems().add(m);
        }
        cbMonth.setValue(LocalDate.now().getMonthValue());
        cbFormat.getItems().addAll(ExportHelper.formatLabels());
        cbFormat.setValue(ExportHelper.formatLabels().get(0));
        lblStatus.setText("选择月份后导出：该月进出仓单全部按单号顺序打印");
    }

    @FXML
    protected void onExportClicked() {
        String ym = String.format("%d%02d", cbYear.getValue(), cbMonth.getValue());
        String fmt = ExportHelper.formatKey(cbFormat.getValue());
        // JavaFX 线程规则：下载走后台 Task（CONTRIBUTING §3.4）
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ApiClient.downloadFile("/api/reports/monthly-orders/" + ym, Map.of("format", fmt));
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                File saved = ExportHelper.save(btnExport.getScene().getWindow(),
                        "月度进出仓单_" + ym + ExportHelper.extensionOf(fmt),
                        ExportHelper.extensionOf(fmt), task.getValue());
                lblStatus.setText(saved == null ? "已取消保存" : "已导出：" + saved.getAbsolutePath());
            } catch (UncheckedIOException ex) {
                new Alert(Alert.AlertType.ERROR, "文件保存失败：" + ex.getCause().getMessage()).showAndWait();
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "导出失败：" + ExportHelper.rootMessage(task.getException()))
                        .showAndWait()));
        Thread t = new Thread(task, "report-monthly-export");
        t.setDaemon(true);
        t.start();
    }
}
