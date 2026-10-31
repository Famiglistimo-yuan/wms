package com.wms.wmsclient.controller;

import com.wms.common.dto.LedgerPreview;
import com.wms.common.dto.LedgerPreviewRow;
import com.wms.common.dto.MaterialOption;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 仓库账本导出面板（FR-5-3）：选择物料与年份，可先「预览」查看该物料按日期的进仓/出仓/结存，
 * 再导出（Excel/Word）。物料下拉数据来自 /api/stats/material-options（stats 包内接口，不依赖 FR-1 的 /api/materials）。
 * 预览分页逻辑见 PreviewPager。
 */
public class ReportLedgerController {

    /** 状态栏默认提示，导出/物料列表加载结束后恢复 */
    private static final String STATUS_HINT = "选择物料与年份后导出：按日期列出进仓、出仓与结存";

    /** 预览分页大小（技术方案 §6.3 上限 100；全年最多 366 行，取满 100 减少翻页） */
    private static final int PAGE_SIZE = 100;

    @FXML private ComboBox<MaterialOption> cbMaterial;
    @FXML private ChoiceBox<Integer> cbYear;
    @FXML private ChoiceBox<String> cbFormat;
    @FXML private FusionButton btnExport;
    @FXML private Label lblStatus;
    @FXML private FusionButton btnPreview;
    @FXML private TableView<LedgerPreviewRow> tvPreview;
    @FXML private FusionButton btnPrev;
    @FXML private FusionButton btnNext;
    @FXML private Label lblPage;
    @FXML private Label lblMeta;

    private boolean loading;
    private PreviewPager<LedgerPreviewRow> pager;

    /** App 注入构造器（PanelScene 的 ControllerFactory 要求，ADR-007） */
    public ReportLedgerController(App app) {
        // app 仅为工厂注入约定保留，面板内未使用
    }

    @FXML
    protected void initialize() {
        ExportHelper.fillYears(cbYear);
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
        TableState.addCol(tvPreview, "日期", "orderDate", 140);
        TableState.addCol(tvPreview, "进仓数量", "inQty", 120);
        TableState.addCol(tvPreview, "出仓数量", "outQty", 120);
        TableState.addCol(tvPreview, "结存", "balance", 120);
        TableState.empty(tvPreview, "点击「预览」查看将导出的数据");
        pager = new PreviewPager<>(tvPreview, btnPreview, btnPrev, btnNext, lblPage,
                PAGE_SIZE, "日流水", "report-ledger-preview",
                () -> ApiClient.getResultData(ApiClient.get(previewPath()), LedgerPreview.class),
                pv -> pv.getRecords(), pv -> pv.getTotal(),
                pv -> {
                    lblMeta.setText("物料：" + pv.getMaterialCode() + " " + pv.getMaterialName()
                            + " · 规格：" + pv.getSpec()
                            + " · 单位：" + pv.getUnit()
                            + " · 期初结存：" + pv.getOpeningBalance() + " " + pv.getUnit());
                    if (pv.getRecords().isEmpty())
                        TableState.empty(tvPreview, pv.getMaterialName() + " " + cbYear.getValue() + "年无进出仓流水");
                });
        // 监听器加在初值设定之后；物料列表异步加载 selectFirst() 触发一次，仅重设占位符，无副作用
        cbMaterial.valueProperty().addListener((o, ov, nv) -> resetPreview());
        cbYear.valueProperty().addListener((o, ov, nv) -> resetPreview());
        loadMaterials();
    }

    private void loadMaterials() {
        lblStatus.setText("物料列表加载中…");
        ApiTasks.run("ledger-materials",
                () -> {
                    String json = ApiClient.get("/api/stats/material-options");
                    return ApiClient.getResultList(json, MaterialOption.class);
                },
                options -> {
                    lblStatus.setText(STATUS_HINT);
                    cbMaterial.getItems().setAll(options);
                    cbMaterial.getSelectionModel().selectFirst();
                },
                ex -> lblStatus.setText("物料列表加载失败：" + ExportHelper.rootMessage(ex)));
    }

    @FXML
    protected void onExportClicked() {
        if (loading) return;
        MaterialOption material = cbMaterial.getValue();
        if (material == null) {
            Alerts.warn("请选择物料");
            return;
        }
        int year = cbYear.getValue();
        String fmt = ExportHelper.formatKey(cbFormat.getValue());
        setLoading(true);
        ApiTasks.run("report-ledger-export",
                () -> {
                    // 物料代码 URL 编码：万一含 / ? # 空格等字符不破坏路径（服务端路径变量自动解码）
                    String code = URLEncoder.encode(material.getMaterialCode(), StandardCharsets.UTF_8);
                    return ApiClient.downloadFile("/api/reports/ledger/" + code + "/" + year,
                            Map.of("format", fmt));
                },
                bytes -> {
                    setLoading(false);
                    String status = ExportHelper.saveAndDescribe(btnExport.getScene().getWindow(),
                            "仓库账本_" + material.getMaterialCode() + "_" + year + ExportHelper.extensionOf(fmt),
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
        if (cbMaterial.getValue() == null) {
            Alerts.warn("请选择物料");
            return;
        }
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

    /** 预览取数路径（当前物料 + 年份 + 页码） */
    private String previewPath() {
        // 物料代码 URL 编码：万一含 / ? # 空格等字符不破坏路径（服务端路径变量自动解码）
        String code = URLEncoder.encode(cbMaterial.getValue().getMaterialCode(), StandardCharsets.UTF_8);
        return "/api/reports/ledger/" + code + "/" + cbYear.getValue()
                + "/preview?page=" + pager.page() + "&size=" + PAGE_SIZE;
    }

    /** 物料/年份变更：清空旧预览与摘要防陈旧数据（进面板不自动查询，对齐 stats-flow 的决策） */
    private void resetPreview() {
        pager.reset();
        lblMeta.setText("");
    }

    /** 导出中：按钮禁用变文案 + 状态栏提示（百万级单据导出耗时长，防重复触发） */
    private void setLoading(boolean on) {
        loading = on;
        btnExport.setDisable(on);
        btnExport.setText(on ? "导出中…" : "导出");
        lblStatus.setText(on ? "正在导出…" : STATUS_HINT);
    }
}
