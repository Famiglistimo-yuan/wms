package com.wms.wmsclient.controller;

import com.wms.common.dto.OrderDetailVO;
import com.wms.common.dto.OrderItemVO;
import com.wms.common.dto.OrderVO;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ApiTasks;
import com.wms.wmsclient.util.TableState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 进出仓单查询（FR-2：单据组合查询 + 详情）。
 * 服务端接口：GET /api/stock/orders（dateFrom/dateTo/material/operator/remark/orderType + page/size）、
 * GET /api/stock/orders/{id}（详情）。列表分页走服务端（page/size 契约），双击行查看明细。
 */
public class StockOrderQueryController {

    private static final int PAGE_SIZE = 20;

    private final App app;

    private final ObservableList<OrderRow> rows = FXCollections.observableArrayList();
    /** 在途互斥：串行化翻页/查询请求，避免响应乱序覆盖 */
    private boolean loading;
    private long page = 1;
    private long total = 0;

    @FXML private ComboBox<String> cbType;
    @FXML private TextField tfMaterial;
    @FXML private TextField tfOperator;
    @FXML private TextField tfRemark;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private TableView<OrderRow> tvOrders;
    @FXML private Label lblStatus;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;

    /** App 注入构造器（PanelScene 的 ControllerFactory 要求，ADR-007）；标题文案在 FXML 内维护 */
    public StockOrderQueryController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        cbType.getItems().setAll("全部", "进仓", "出仓");
        cbType.getSelectionModel().selectFirst();

        tvOrders.setItems(rows);
        TableState.addCol(tvOrders, "单号", "orderNo", 150);
        TableState.addCol(tvOrders, "类型", "typeLabel", 60);
        TableState.addCol(tvOrders, "日期", "orderDate", 100);
        TableState.addCol(tvOrders, "操作人", "operatorName", 90);
        TableState.addCol(tvOrders, "经手人", "handlerName", 90);
        TableState.addCol(tvOrders, "物料数", "itemCountStr", 70);
        TableState.addCol(tvOrders, "数量合计", "totalQuantityStr", 90);
        TableState.addCol(tvOrders, "备注", "remark", 180);

        // 双击行查看单据详情
        tvOrders.setRowFactory(tv -> {
            TableRow<OrderRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    showDetail(row.getItem());
                }
            });
            return row;
        });

        onRefreshClicked();
    }

    /** 按当前条件查询（回到第 1 页） */
    @FXML
    protected void onRefreshClicked() {
        page = 1;
        loadPage();
    }

    @FXML
    protected void onPrevPage() {
        if (page > 1) {
            page--;
            loadPage();
        }
    }

    @FXML
    protected void onNextPage() {
        if (page * PAGE_SIZE < total) {
            page++;
            loadPage();
        }
    }

    /** 重置筛选条件并回到第 1 页 */
    @FXML
    protected void onResetClicked() {
        cbType.getSelectionModel().selectFirst();
        tfMaterial.clear();
        tfOperator.clear();
        tfRemark.clear();
        dpFrom.setValue(null);
        dpTo.setValue(null);
        onRefreshClicked();
    }

    /** 拉取当前页数据 */
    private void loadPage() {
        if (loading) return;
        loading = true;
        TableState.loading(tvOrders);
        lblStatus.setText("加载中…");
        Map<String, String> params = new LinkedHashMap<>();
        String type = cbType.getValue();
        if ("进仓".equals(type)) params.put("orderType", "IN");
        else if ("出仓".equals(type)) params.put("orderType", "OUT");
        putIfPresent(params, "material", tfMaterial);
        putIfPresent(params, "operator", tfOperator);
        putIfPresent(params, "remark", tfRemark);
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        if (from != null) params.put("dateFrom", from.toString());
        if (to != null) params.put("dateTo", to.toString());
        params.put("page", String.valueOf(page));
        params.put("size", String.valueOf(PAGE_SIZE));

        ApiTasks.run("stock-order-query",
                () -> ApiClient.getResultPage(ApiClient.get("/api/stock/orders", params), OrderVO.class),
                pr -> {
                    loading = false;
                    rows.setAll(pr.getRecords().stream().map(OrderRow::new).toList());
                    total = pr.getTotal();
                    updatePageUi();
                    lblStatus.setText("共 " + total + " 张单据");
                    TableState.empty(tvOrders, "没有符合条件的单据");
                },
                ex -> {
                    loading = false;
                    String msg = ex.getMessage();
                    lblStatus.setText("加载失败：" + msg);
                    TableState.failed(tvOrders, msg);
                });
    }

    private void updatePageUi() {
        long totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        lblPage.setText(page + " / " + totalPages);
        btnPrev.setDisable(page <= 1);
        btnNext.setDisable(page * PAGE_SIZE >= total);
    }

    private void putIfPresent(Map<String, String> params, String key, TextField tf) {
        String v = tf.getText() == null ? "" : tf.getText().trim();
        if (!v.isEmpty()) params.put(key, v);
    }

    /** 双击行 → 单据详情弹窗（主表信息 + 明细列表） */
    private void showDetail(OrderRow row) {
        ApiTasks.run("stock-order-detail",
                () -> ApiClient.getResultData(ApiClient.get("/api/stock/orders/" + row.id), OrderDetailVO.class),
                detail -> {
                    Alert alert = Alerts.of(Alert.AlertType.INFORMATION, formatDetail(detail));
                    alert.setTitle("单据详情 — " + row.getOrderNo());
                    alert.setHeaderText(null);
                    alert.showAndWait();
                },
                ex -> Alerts.error("详情加载失败：" + ex.getMessage()));
    }

    private String formatDetail(OrderDetailVO detail) {
        OrderVO o = detail.getOrder();
        StringBuilder sb = new StringBuilder();
        sb.append("【单号】").append(o.getOrderNo()).append('\n');
        sb.append("【类型】").append("IN".equals(o.getOrderType()) ? "进仓" : "出仓").append('\n');
        sb.append("【日期】").append(o.getOrderDate()).append('\n');
        sb.append("【操作人】").append(o.getOperatorName()).append('\n');
        sb.append("【经手人】").append(o.getHandlerName()).append('\n');
        sb.append("【备注】").append(o.getRemark() == null || o.getRemark().isEmpty() ? "（无）" : o.getRemark()).append('\n');
        sb.append("【明细】共 ").append(detail.getItems().size()).append(" 条\n");
        sb.append("──────────\n");
        for (OrderItemVO it : detail.getItems()) {
            sb.append("· ").append(it.getMaterialCode()).append(' ').append(it.getMaterialName());
            if (it.getSpec() != null && !it.getSpec().isEmpty()) sb.append("（").append(it.getSpec()).append("）");
            sb.append("  数量 ").append(strip(it.getQuantity()));
            if (it.getUnit() != null && !it.getUnit().isEmpty()) sb.append(' ').append(it.getUnit());
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String strip(BigDecimal d) {
        return d == null ? "0" : d.stripTrailingZeros().toPlainString();
    }

    // ==================== 行数据 ====================

    /** 表格行：OrderVO 包装，附加展示格式（类型中文、数量去零尾） */
    public static class OrderRow {
        public Long id;
        public final String orderNo;
        public final String orderType;
        public final String orderDate;
        public final String operatorName;
        public final String handlerName;
        public final Long itemCount;
        public final BigDecimal totalQuantity;
        public final String remark;

        public OrderRow(OrderVO vo) {
            this.id = vo.getId();
            this.orderNo = vo.getOrderNo();
            this.orderType = vo.getOrderType();
            this.orderDate = vo.getOrderDate();
            this.operatorName = vo.getOperatorName();
            this.handlerName = vo.getHandlerName();
            this.itemCount = vo.getItemCount();
            this.totalQuantity = vo.getTotalQuantity();
            this.remark = vo.getRemark();
        }

        public String getOrderNo() { return orderNo == null ? "" : orderNo; }
        public String getTypeLabel() { return "IN".equals(orderType) ? "进仓" : "出仓"; }
        public String getOrderDate() { return orderDate == null ? "" : orderDate; }
        public String getOperatorName() { return operatorName == null ? "" : operatorName; }
        public String getHandlerName() { return handlerName == null ? "" : handlerName; }
        public String getItemCountStr() { return itemCount == null ? "0" : String.valueOf(itemCount); }
        public String getTotalQuantityStr() { return strip(totalQuantity); }
        public String getRemark() { return remark == null ? "" : remark; }
    }
}
