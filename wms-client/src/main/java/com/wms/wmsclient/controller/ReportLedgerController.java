package com.wms.wmsclient.controller;

import com.wms.common.dto.MaterialOption;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ExportHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 仓库账本导出面板（FR-5-3）：选择物料与年份，导出该物料按日期的进仓/出仓/结存（Excel/Word）。
 * 物料下拉数据来自 /api/stats/material-options（stats 包内接口，不依赖 FR-1 的 /api/materials）。
 */
public class ReportLedgerController {

    /** 状态栏默认提示，导出/物料列表加载结束后恢复 */
    private static final String STATUS_HINT = "选择物料与年份后导出：按日期列出进仓、出仓与结存";

    private final App app;

    @FXML private ComboBox<MaterialOption> cbMaterial;
    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private Button btnExport;
    @FXML private Label lblStatus;

    private boolean loading;

    public ReportLedgerController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        int thisYear = LocalDate.now().getYear();
        for (int y = thisYear - 5; y <= thisYear + 1; y++) {
            cbYear.getItems().add(y);
        }
        cbYear.setValue(thisYear);
        cbFormat.getItems().addAll(ExportHelper.formatLabels());
        cbFormat.setValue(ExportHelper.formatLabels().get(0));
        cbMaterial.setConverter(new StringConverter<>() {
            @Override
            public String toString(MaterialOption option) {
                return option == null ? "" : option.getMaterialCode() + " " + option.getName();
            }

            @Override
            public MaterialOption fromString(String string) {
                return null; // 不可编辑下拉，用不到
            }
        });
        lblStatus.setText(STATUS_HINT);
        loadMaterials();
    }

    private void loadMaterials() {
        lblStatus.setText("物料列表加载中…");
        Task<List<MaterialOption>> task = new Task<>() {
            @Override
            protected List<MaterialOption> call() throws Exception {
                String json = ApiClient.get("/api/stats/material-options");
                return ApiClient.getResultList(json, MaterialOption.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lblStatus.setText(STATUS_HINT);
            cbMaterial.getItems().setAll(task.getValue());
            cbMaterial.getSelectionModel().selectFirst();
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                lblStatus.setText("物料列表加载失败：" + ExportHelper.rootMessage(task.getException()))));
        Thread t = new Thread(task, "ledger-materials");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    protected void onExportClicked() {
        if (loading) return;
        MaterialOption material = cbMaterial.getValue();
        if (material == null) {
            Alerts.of(Alert.AlertType.WARNING, "请选择物料").showAndWait();
            return;
        }
        int year = cbYear.getValue();
        String fmt = ExportHelper.formatKey(cbFormat.getValue());
        setLoading(true);
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                // 物料代码 URL 编码：万一含 / ? # 空格等字符不破坏路径（服务端路径变量自动解码）
                String code = URLEncoder.encode(material.getMaterialCode(), StandardCharsets.UTF_8);
                return ApiClient.downloadFile("/api/reports/ledger/" + code + "/" + year,
                        Map.of("format", fmt));
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setLoading(false);
            try {
                File saved = ExportHelper.save(btnExport.getScene().getWindow(),
                        "仓库账本_" + material.getMaterialCode() + "_" + year + ExportHelper.extensionOf(fmt),
                        ExportHelper.extensionOf(fmt), task.getValue());
                lblStatus.setText(saved == null ? "已取消保存" : "已导出：" + saved.getAbsolutePath());
            } catch (UncheckedIOException ex) {
                Alerts.of(Alert.AlertType.ERROR, "文件保存失败：" + ex.getCause().getMessage()).showAndWait();
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            Alerts.of(Alert.AlertType.ERROR, "导出失败：" + ExportHelper.rootMessage(task.getException()))
                    .showAndWait();
        }));
        Thread t = new Thread(task, "report-ledger-export");
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
