package com.wms.wmsclient.controller;

import com.wms.common.dto.MaterialFlowReport;
import com.wms.common.dto.MaterialFlowStat;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 物料流量统计面板（FR-5-1）：指定时间区间内各物料进出仓总数，BarChart 图形显示；
 * 服务端已算好流动量最小物料（分析结论直接展示，业务逻辑收敛服务端 ADR-005）。
 */
public class StatsFlowController {

    private final App app;

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private BarChart<String, Number> chartFlow;
    @FXML private Label lblMinFlow;

    public StatsFlowController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        // 默认区间：本月第一天 ~ 今天
        LocalDate today = LocalDate.now();
        dpFrom.setValue(today.withDayOfMonth(1));
        dpTo.setValue(today);
        onQueryClicked();
    }

    @FXML
    protected void onQueryClicked() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        if (from == null || to == null) {
            Alerts.of(Alert.AlertType.WARNING, "请选择起始与截止日期").showAndWait();
            return;
        }
        if (from.isAfter(to)) {
            Alerts.of(Alert.AlertType.WARNING, "起始日期不能晚于截止日期").showAndWait();
            return;
        }
        // JavaFX 线程规则：HTTP 走后台 Task，渲染回 UI 线程（CONTRIBUTING §3.4）
        Task<MaterialFlowReport> task = new Task<>() {
            @Override
            protected MaterialFlowReport call() throws Exception {
                String json = ApiClient.get("/api/stats/material-flow",
                        Map.of("from", from.toString(), "to", to.toString()));
                return ApiClient.getResultData(json, MaterialFlowReport.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> render(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            Alerts.of(Alert.AlertType.ERROR, "统计失败：" + (ex == null ? "未知错误" : ex.getMessage()))
                    .showAndWait();
        }));
        Thread t = new Thread(task, "stats-flow-query");
        t.setDaemon(true);
        t.start();
    }

    private void render(MaterialFlowReport report) {
        chartFlow.getData().clear();
        XYChart.Series<String, Number> inSeries = new XYChart.Series<>();
        inSeries.setName("进仓数量");
        XYChart.Series<String, Number> outSeries = new XYChart.Series<>();
        outSeries.setName("出仓数量");
        // 服务端已按流动量降序，图表从左到右自然从高到低
        for (MaterialFlowStat it : report.getItems()) {
            String label = it.getMaterialCode() + " " + it.getName();
            inSeries.getData().add(new XYChart.Data<>(label, it.getInQty()));
            outSeries.getData().add(new XYChart.Data<>(label, it.getOutQty()));
        }
        chartFlow.getData().addAll(inSeries, outSeries);

        var mins = report.getMinFlowMaterials();
        if (mins == null || mins.isEmpty()) {
            lblMinFlow.setText("暂无物料数据");
        } else {
            String detail = mins.stream()
                    .map(m -> m.getMaterialCode() + " " + m.getName()
                            + "（进仓 " + strip(m.getInQty()) + "，出仓 " + strip(m.getOutQty())
                            + "，合计 " + strip(m.getTotalFlow()) + " " + m.getUnit() + "）")
                    .collect(java.util.stream.Collectors.joining("；"));
            lblMinFlow.setText(mins.size() == 1
                    ? "流动量最小的物料：" + detail
                    : "流动量最小的物料（" + mins.size() + " 种并列）：" + detail);
        }
    }

    /** 去掉 BigDecimal 多余的小数零（800.000 → 800），仅展示用 */
    private String strip(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
