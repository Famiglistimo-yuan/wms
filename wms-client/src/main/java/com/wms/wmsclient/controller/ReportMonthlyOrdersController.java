package com.wms.wmsclient.controller;

import com.wms.common.dto.MonthlyOrderPreviewRow;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ApiTasks;
import com.wms.wmsclient.util.ExportHelper;
import com.wms.wmsclient.util.PreviewPager;
import com.wms.wmsclient.util.TableState;
import io.vproxy.vfx.ui.button.FusionButton;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.Map;

/**
 * 月度进出仓单导出面板（FR-5-2）：选择时间与格式（Excel/Word），可先「预览」查看将导出的数据；
 * 导出后保存，路径显示在状态栏。预览分页逻辑见 PreviewPager。
 */
public class ReportMonthlyOrdersController {

    /** 状态栏默认提示，导出结束后恢复 */
    private static final String STATUS_HINT = "选择月份后导出：该月进出仓单全部按单号顺序打印";

    /** 预览分页大小（技术方案 §6.3 上限 100 内；月度表 10 列较宽，取 20） */
    private static final int PAGE_SIZE = 20;

    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<Integer> cbMonth;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private FusionButton btnExport;
    @FXML private Label lblStatus;
    @FXML private FusionButton btnPreview;
    @FXML private TableView<MonthlyOrderPreviewRow> tvPreview;
    @FXML private FusionButton btnPrev;
    @FXML private FusionButton btnNext;
    @FXML private Label lblPage;

    private boolean loading;
    private PreviewPager<MonthlyOrderPreviewRow> pager;

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
        pager = new PreviewPager<>(tvPreview, btnPreview, btnPrev, btnNext, lblPage,
                PAGE_SIZE, "条明细", "report-monthly-preview",
                () -> ApiClient.getResultPage(ApiClient.get(previewPath()), MonthlyOrderPreviewRow.class),
                pg -> pg.getRecords(), pg -> pg.getTotal(),
                pg -> {
                    if (pg.getRecords().isEmpty())
                        TableState.empty(tvPreview, cbYear.getValue() + "年" + cbMonth.getValue() + "月无进出仓单据");
                });
        // 监听器加在初值设定之后，避免初始化期间误触发清空
        cbYear.valueProperty().addListener((o, ov, nv) -> pager.reset());
        cbMonth.valueProperty().addListener((o, ov, nv) -> pager.reset());
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

    @FXML
    protected void onPreviewClicked() {
        pager.first();
    }

    @FXML
    protected void onPrevClicked() {
        pager.prev();
    }

    @FXML
    protected void onNextClicked() {
        pager.next();
    }

    /** 预览取数路径（当前年月 + 页码） */
    private String previewPath() {
        return "/api/reports/monthly-orders/"
                + String.format("%d%02d", cbYear.getValue(), cbMonth.getValue())
                + "/preview?page=" + pager.page() + "&size=" + PAGE_SIZE;
    }

    /** 导出中：按钮禁用变文案 + 状态栏提示（百万级单据导出耗时长，防重复触发） */
    private void setLoading(boolean on) {
        loading = on;
        btnExport.setDisable(on);
        btnExport.setText(on ? "导出中…" : "导出");
        lblStatus.setText(on ? "正在导出…" : STATUS_HINT);
    }
}
