package com.wms.wmsclient.controller;

import com.wms.common.dto.MonthlyOrderPreviewRow;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ApiTasks;
import com.wms.wmsclient.util.ExportHelper;
import com.wms.wmsclient.util.TableState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.Map;

/**
 * 月度进出仓单导出面板（FR-5-2）：选择时间与格式（Excel/Word），可先「预览」查看将导出的数据；
 * 导出后保存，路径显示在状态栏。
 */
public class ReportMonthlyOrdersController {

    /** 状态栏默认提示，导出结束后恢复 */
    private static final String STATUS_HINT = "选择月份后导出：该月进出仓单全部按单号顺序打印";

    /** 预览分页大小（技术方案 §6.3 上限 100 内；月度表 10 列较宽，取 20） */
    private static final int PAGE_SIZE = 20;

    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<Integer> cbMonth;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private Button btnExport;
    @FXML private Label lblStatus;
    @FXML private Button btnPreview;
    @FXML private TableView<MonthlyOrderPreviewRow> tvPreview;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;

    private final ObservableList<MonthlyOrderPreviewRow> previewRows = FXCollections.observableArrayList();
    private boolean loading;
    private boolean previewing;
    private long totalRows;
    private int page = 1;
    /** 查询代次：条件变更或重新预览时自增，在途响应落地时代次不符则丢弃，防旧数据回填 */
    private int loadSeq;

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
        tvPreview.setItems(previewRows);
        TableState.addCol(tvPreview, "单号", "orderNo", 160);
        TableState.addCol(tvPreview, "类型", "typeLabel", 70);
        TableState.addCol(tvPreview, "日期", "orderDate", 120);
        TableState.addCol(tvPreview, "操作人", "operatorName", 100);
        TableState.addCol(tvPreview, "经手人", "handlerName", 100);
        TableState.addCol(tvPreview, "物料代码", "materialCode", 110);
        TableState.addCol(tvPreview, "物料名称", "materialName", 140);
        TableState.addCol(tvPreview, "单位", "unit", 70);
        TableState.addCol(tvPreview, "数量", "quantity", 100);
        TableState.addCol(tvPreview, "备注", "remark", 220);
        TableState.empty(tvPreview, "点击「预览」查看将导出的数据");
        // 监听器加在初值设定之后，避免初始化期间误触发清空
        cbYear.valueProperty().addListener((o, ov, nv) -> resetPreview());
        cbMonth.valueProperty().addListener((o, ov, nv) -> resetPreview());
    }

    @FXML
    protected void onExportClicked() {
        if (loading) return;
        String ym = String.format("%d%02d", cbYear.getValue(), cbMonth.getValue());
        String fmt = ExportHelper.formatKey(cbFormat.getValue());
        // JavaFX 线程规则：下载走后台线程（CONTRIBUTING §3.4）
        setLoading(true);
        ApiTasks.run("report-monthly-export",
                () -> ApiClient.downloadFile("/api/reports/monthly-orders/" + ym, Map.of("format", fmt)),
                bytes -> {
                    setLoading(false);
                    String status = ExportHelper.saveAndDescribe(btnExport.getScene().getWindow(),
                            "月度进出仓单_" + ym + ExportHelper.extensionOf(fmt),
                            ExportHelper.extensionOf(fmt), bytes);
                    if (status != null) lblStatus.setText(status);
                },
                ex -> {
                    setLoading(false);
                    Alerts.error("导出失败：" + ExportHelper.rootMessage(ex));
                });
    }

    // ====== 导出前预览 ======

    @FXML
    protected void onPreviewClicked() {
        page = 1;
        loadPreview();
    }

    @FXML
    protected void onPrevClicked() {
        if (!previewing && page > 1) {
            page--;
            loadPreview();
        }
    }

    @FXML
    protected void onNextClicked() {
        if (!previewing && page < pages()) {
            page++;
            loadPreview();
        }
    }

    private long pages() {
        return Math.max(1, (totalRows + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void loadPreview() {
        if (previewing) return;
        previewing = true;
        btnPreview.setDisable(true);
        btnPrev.setDisable(true);
        btnNext.setDisable(true);
        TableState.loading(tvPreview);
        int seq = ++loadSeq;
        int year = cbYear.getValue();
        int month = cbMonth.getValue();
        String ym = String.format("%d%02d", year, month);
        String path = "/api/reports/monthly-orders/" + ym + "/preview?page=" + page + "&size=" + PAGE_SIZE;
        // JavaFX 线程规则：HTTP 走后台线程，回调经 ApiTasks 保证在 FX 线程
        ApiTasks.run("report-monthly-preview",
                () -> ApiClient.getResultPage(ApiClient.get(path), MonthlyOrderPreviewRow.class),
                pg -> {
                    previewing = false;
                    btnPreview.setDisable(false);
                    if (seq != loadSeq) return; // 期间条件已变更或已重新预览，本页作废
                    previewRows.setAll(pg.getRecords());
                    totalRows = pg.getTotal();
                    lblPage.setText("第 " + page + "/" + pages() + " 页 · 共 " + totalRows + " 条明细");
                    btnPrev.setDisable(page <= 1);
                    btnNext.setDisable(page >= pages());
                    TableState.empty(tvPreview, year + "年" + month + "月无进出仓单据");
                },
                ex -> {
                    previewing = false;
                    btnPreview.setDisable(false);
                    if (seq != loadSeq) return;
                    TableState.failed(tvPreview, ExportHelper.rootMessage(ex));
                    lblPage.setText("预览失败");
                });
    }

    /** 年/月变更：清空旧预览防陈旧数据（进面板不自动查询，对齐 stats-flow 的决策） */
    private void resetPreview() {
        loadSeq++;
        previewRows.clear();
        page = 1;
        totalRows = 0;
        lblPage.setText("");
        btnPrev.setDisable(true);
        btnNext.setDisable(true);
        TableState.empty(tvPreview, "条件已变更，点击「预览」查看");
    }

    /** 导出中：按钮禁用变文案 + 状态栏提示（百万级单据导出耗时长，防重复触发） */
    private void setLoading(boolean on) {
        loading = on;
        btnExport.setDisable(on);
        btnExport.setText(on ? "导出中…" : "导出");
        lblStatus.setText(on ? "正在导出…" : STATUS_HINT);
    }
}
