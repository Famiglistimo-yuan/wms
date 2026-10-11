package com.wms.wmsclient.controller;

import com.wms.common.dto.MaterialOption;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 仓库账本导出面板（FR-5-3）：选择物料与年份，导出该物料按日期的进仓/出仓/结存（Excel/Word）。
 * 物料下拉数据来自 /api/stats/material-options（stats 包内接口，不依赖 FR-1 的 /api/materials）。
 */
public class ReportLedgerController {

    private final App app;

    @FXML private ComboBox<MaterialOption> cbMaterial;
    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private Button btnExport;
    @FXML private Label lblStatus;

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
        lblStatus.setText("选择物料与年份后导出：按日期列出进仓、出仓与结存");
        loadMaterials();
    }

    private void loadMaterials() {
        Task<List<MaterialOption>> task = new Task<>() {
            @Override
            protected List<MaterialOption> call() throws Exception {
                String json = ApiClient.get("/api/stats/material-options");
                return ApiClient.getResultList(json, MaterialOption.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
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
        MaterialOption material = cbMaterial.getValue();
        if (material == null) {
            new Alert(Alert.AlertType.WARNING, "请选择物料").showAndWait();
            return;
        }
        int year = cbYear.getValue();
        String fmt = ExportHelper.formatKey(cbFormat.getValue());
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ApiClient.downloadFile("/api/reports/ledger/" + material.getMaterialCode() + "/" + year,
                        Map.of("format", fmt));
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                File saved = ExportHelper.saveAndOpen(btnExport.getScene().getWindow(),
                        "仓库账本_" + material.getMaterialCode() + "_" + year + ExportHelper.extensionOf(fmt),
                        ExportHelper.extensionOf(fmt), task.getValue());
                lblStatus.setText(saved == null ? "已取消保存" : "已导出：" + saved.getAbsolutePath());
            } catch (UncheckedIOException ex) {
                new Alert(Alert.AlertType.ERROR, "文件保存失败：" + ex.getCause().getMessage()).showAndWait();
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "导出失败：" + ExportHelper.rootMessage(task.getException()))
                        .showAndWait()));
        Thread t = new Thread(task, "report-ledger-export");
        t.setDaemon(true);
        t.start();
    }
}
