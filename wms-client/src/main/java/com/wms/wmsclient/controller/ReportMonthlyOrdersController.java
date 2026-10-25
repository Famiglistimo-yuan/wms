package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ExportHelper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.Map;

/**
 * 月度进出仓单导出面板（FR-5-2）：选择时间与格式（Excel/Word），导出后保存，路径显示在状态栏。
 */
public class ReportMonthlyOrdersController {

    /** 状态栏默认提示，导出结束后恢复 */
    private static final String STATUS_HINT = "选择月份后导出：该月进出仓单全部按单号顺序打印";

    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<Integer> cbMonth;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private Button btnExport;
    @FXML private Label lblStatus;

    private boolean loading;

    /** App 注入构造器（PanelScene 的 ControllerFactory 要求，ADR-007） */
    public ReportMonthlyOrdersController(App app) {
        // app 仅为工厂注入约定保留，面板内未使用
    }

    @FXML
    protected void initialize() {
        ExportHelper.fillYears(cbYear);
        for (int m = 1; m <= 12; m++) {
            cbMonth.getItems().add(m);
        }
        cbMonth.setValue(LocalDate.now().getMonthValue());
        cbFormat.getItems().addAll(ExportHelper.formatLabels());
        cbFormat.setValue(ExportHelper.formatLabels().get(0));
        lblStatus.setText(STATUS_HINT);
    }

    @FXML
    protected void onExportClicked() {
        if (loading) return;
        String ym = String.format("%d%02d", cbYear.getValue(), cbMonth.getValue());
        String fmt = ExportHelper.formatKey(cbFormat.getValue());
        // JavaFX 线程规则：下载走后台 Task（CONTRIBUTING §3.4）
        setLoading(true);
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ApiClient.downloadFile("/api/reports/monthly-orders/" + ym, Map.of("format", fmt));
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            String status = ExportHelper.saveAndDescribe(btnExport.getScene().getWindow(),
                    "月度进出仓单_" + ym + ExportHelper.extensionOf(fmt),
                    ExportHelper.extensionOf(fmt), task.getValue());
            if (status != null) lblStatus.setText(status);
        });
        task.setOnFailed(e -> {
            setLoading(false);
            Alerts.error("导出失败：" + ExportHelper.rootMessage(task.getException()));
        });
        Thread t = new Thread(task, "report-monthly-export");
        t.setDaemon(true);
        t.start();
    }

    /** 导出中：按钮禁用变文案 + 状态栏提示（百万级单据导出耗时长，防重复触发） */
    private void setLoading(boolean on) {
        loading = on;
        btnExport.setDisable(on);
        btnExport.setText(on ? "导出中…" : "导出");
        lblStatus.setText(on ? "正在导出…" : STATUS_HINT);
    }
}
