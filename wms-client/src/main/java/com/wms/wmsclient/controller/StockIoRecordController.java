package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import com.wms.wmsclient.util.Alerts;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 进出库流水查询面板：支持类型/单据号/物料关键字/经手人/来源去向/日期范围筛选 + 分页 + 每页条数。
 * 后端接口：POST /api/material/io-records/page、GET /api/material/order/{id}（单据详情）
 */
public class StockIoRecordController {

    private final App app;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObservableList<RecordRow> rows = FXCollections.observableArrayList();
    /** materialId → "编码 名称（规格）" 查找表，客户端 JOIN */
    private final Map<Long, String> materialLookup = new HashMap<>();

    // 分页状态
    private int page = 1;
    private int pageSize = 20;
    private long total = 0;

    @FXML private Label lblTitle;
    @FXML private ComboBox<String> cbFilter;          // 全部/入库/出库
    @FXML private TextField tfOrderNo;
    @FXML private TextField tfMaterialKeyword;
    @FXML private TextField tfHandler;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private TableView<RecordRow> tvRecords;
    @FXML private TableColumn<RecordRow, String> colTime;
    @FXML private TableColumn<RecordRow, String> colType;
    @FXML private TableColumn<RecordRow, String> colOrderNo;
    @FXML private TableColumn<RecordRow, String> colMaterial;
    @FXML private TableColumn<RecordRow, String> colNum;
    @FXML private TableColumn<RecordRow, String> colBefore;
    @FXML private TableColumn<RecordRow, String> colAfter;
    @FXML private TableColumn<RecordRow, String> colOperator;
    @FXML private TableColumn<RecordRow, String> colSourceTarget;
    @FXML private TableColumn<RecordRow, String> colRemark;
    @FXML private Label lblStatus;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private TextField tfPageSize;
    @FXML private TextField tfJumpPage;

    public StockIoRecordController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        lblTitle.setText("单据查询（进出库流水）");

        // 筛选下拉
        cbFilter.setItems(FXCollections.observableArrayList("全部", "入库", "出库"));
        cbFilter.setValue("全部");

        // 每页条数：默认 20，允许 1~50
        tfPageSize.setText(String.valueOf(pageSize));

        // ===== TableView 列 =====
        tvRecords.setEditable(false);
        tvRecords.setItems(rows);

        colTime.setCellValueFactory(c -> c.getValue().timeProperty());
        colType.setCellValueFactory(c -> c.getValue().typeProperty());
        colType.setCellFactory(col -> typeCell());
        colOrderNo.setCellValueFactory(c -> c.getValue().orderNoProperty());
        // 单据号列：可点击（下划线蓝）
        colOrderNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (empty || item == null || item.isEmpty()) {
                    setStyle(null);
                    setOnMouseClicked(null);
                } else {
                    setStyle("-fx-text-fill: #1976d2; -fx-underline: true; -fx-cursor: hand;");
                    setOnMouseClicked(evt -> {
                        if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                            if (getTableRow() != null && getTableRow().getItem() instanceof RecordRow row) {
                                showOrderDetail(row);
                            }
                        }
                    });
                }
            }
        });
        colMaterial.setCellValueFactory(c -> c.getValue().materialProperty());
        colNum.setCellValueFactory(c -> c.getValue().numProperty());
        colBefore.setCellValueFactory(c -> c.getValue().beforeProperty());
        colAfter.setCellValueFactory(c -> c.getValue().afterProperty());
        colOperator.setCellValueFactory(c -> c.getValue().operatorProperty());
        colSourceTarget.setCellValueFactory(c -> c.getValue().sourceTargetProperty());
        colRemark.setCellValueFactory(c -> c.getValue().remarkProperty());

        // 首屏加载
        onRefreshClicked();
    }

    /** 刷新/查询：POST /api/material/io-records/page */
    @FXML
    protected void onRefreshClicked() {
        lblStatus.setText("正在加载…");

        // 构建查询 body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", page);
        body.put("pageSize", pageSize);

        String sel = cbFilter.getValue();
        if ("入库".equals(sel)) body.put("orderType", 1);
        else if ("出库".equals(sel)) body.put("orderType", 2);

        String orderNo = tfOrderNo.getText() == null ? "" : tfOrderNo.getText().trim();
        String materialKeyword = tfMaterialKeyword == null || tfMaterialKeyword.getText() == null ? "" : tfMaterialKeyword.getText().trim();
        String handler = tfHandler == null || tfHandler.getText() == null ? "" : tfHandler.getText().trim();

        if (!orderNo.isEmpty()) body.put("orderNo", orderNo);
        if (!materialKeyword.isEmpty()) body.put("materialKeyword", materialKeyword);
        if (!handler.isEmpty()) body.put("handler", handler);

        // 日期范围
        LocalDate sd = dpStartDate == null ? null : dpStartDate.getValue();
        LocalDate ed = dpEndDate == null ? null : dpEndDate.getValue();
        if (sd != null) body.put("startTime", sd.atStartOfDay().toString());
        if (ed != null) body.put("endTime", ed.atTime(LocalTime.MAX).toString());

        String jsonBody;
        try { jsonBody = ApiClient.toJson(body); }
        catch (Exception ex) { lblStatus.setText("序列化失败：" + ex.getMessage()); return; }

        Task<Void> task = new Task<>() {
            List<RecordRow> parsedRows;
            Map<Long, String> lookup;
            long totalLocal;
            int pageLocal, sizeLocal;

            @Override
            protected Void call() throws Exception {
                // 1) 同时拉物料列表（用于 materialId → 编码/名称 客户端 JOIN）
                String listJson = ApiClient.get("/api/material/list");
                JsonNode listNode = MAPPER.readTree(listJson);
                JsonNode listData = listNode.isArray() ? listNode : listNode.path("data");
                lookup = new HashMap<>();
                for (JsonNode n : listData) {
                    long id = n.path("id").asLong();
                    String code = n.path("materialCode").asText("");
                    String name = n.path("materialName").asText("");
                    String spec = n.path("spec").asText("");
                    String label = code + " " + name + (spec.isBlank() ? "" : "（" + spec + "）");
                    lookup.put(id, label.trim());
                }

                // 2) 拉分页流水
                String recordJson = ApiClient.post("/api/material/io-records/page", jsonBody);
                JsonNode root = MAPPER.readTree(recordJson);
                if (root.has("code") && root.get("code").asInt() != 0) {
                    throw new ApiException(root.get("code").asInt(), root.path("message").asText("查询失败"));
                }
                JsonNode data = root.path("data");
                totalLocal = data.path("total").asLong();
                pageLocal = data.path("page").asInt();
                sizeLocal = data.path("pageSize").asInt();
                JsonNode records = data.path("records");

                parsedRows = new ArrayList<>();
                for (JsonNode node : records) {
                    RecordRow r = MAPPER.treeToValue(node, RecordRow.class);
                    r.materialLabel = lookup.getOrDefault(
                            r.materialId != null ? r.materialId : -1L, "(未知物料)");
                    parsedRows.add(r);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    rows.setAll(parsedRows);
                    materialLookup.clear();
                    materialLookup.putAll(lookup);
                    total = totalLocal;
                    page = pageLocal;
                    pageSize = sizeLocal;
                    // 同步分页控件
                    String cur = tfPageSize.getText();
                    if (!String.valueOf(pageSize).equals(cur)) tfPageSize.setText(String.valueOf(pageSize));
                    updatePageUI();
                    lblStatus.setText("共 " + total + " 条 · 当前页 " + rows.size() + " 条");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable t = getException();
                    String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                    lblStatus.setText("加载失败：" + msg);
                });
            }
        };
        new Thread(task, "stock-records-page").start();
    }

    /** 上一页 */
    @FXML
    protected void onPrevPage() {
        if (page > 1) { page--; onRefreshClicked(); }
    }

    /** 下一页 */
    @FXML
    protected void onNextPage() {
        if ((long) page * pageSize < total) { page++; onRefreshClicked(); }
    }

    /** 重置筛选条件 + 回到第一页 */
    @FXML
    protected void onResetFilter() {
        cbFilter.setValue("全部");
        if (tfOrderNo != null) tfOrderNo.clear();
        if (tfMaterialKeyword != null) tfMaterialKeyword.clear();
        if (tfHandler != null) tfHandler.clear();
        if (dpStartDate != null) dpStartDate.setValue(null);
        if (dpEndDate != null) dpEndDate.setValue(null);
        page = 1;
        try { int ps = Integer.parseInt(tfPageSize.getText().trim()); pageSize = clampPs(ps); }
        catch (NumberFormatException ignored) { pageSize = 20; tfPageSize.setText("20"); }
        onRefreshClicked();
    }

    /** 每页条数输入：校验 1~50，超出钳位 */
    private int clampPs(int v) {
        if (v < 1) return 20;
        if (v > 50) return 50;
        return v;
    }

    /** 手动应用每页条数 */
    @FXML
    protected void onApplyPageSize() {
        try {
            int v = Integer.parseInt(tfPageSize.getText().trim());
            pageSize = clampPs(v);
            tfPageSize.setText(String.valueOf(pageSize));
            page = 1;
            onRefreshClicked();
        } catch (NumberFormatException ex) {
            tfPageSize.setText("20");
            pageSize = 20;
            page = 1;
            onRefreshClicked();
        }
    }

    /** 跳转到指定页 */
    @FXML
    protected void onJumpPage() {
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        try {
            int target = Integer.parseInt(tfJumpPage.getText().trim());
            if (target < 1) target = 1;
            if (target > totalPages) target = totalPages;
            page = target;
            tfJumpPage.setText(String.valueOf(target));
            onRefreshClicked();
        } catch (NumberFormatException ex) {
            tfJumpPage.setText("1");
            page = 1;
            onRefreshClicked();
        }
    }

    /** 更新分页 UI（页码显示 + 按钮禁用） */
    private void updatePageUI() {
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        lblPage.setText(page + " / " + totalPages);
        btnPrev.setDisable(page <= 1);
        btnNext.setDisable((long) page * pageSize >= total);
    }

    /** 类型列：入库绿、出库红 */
    private TableCell<RecordRow, String> typeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (empty || item == null) setStyle(null);
                else if ("入库".equals(item)) setStyle("-fx-text-fill: #2e7d32;");
                else setStyle("-fx-text-fill: #c62828;");
            }
        };
    }

    /** 点击单据号 → 弹窗显示整张单据详情 */
    private void showOrderDetail(RecordRow row) {
        String orderNo = row.orderNo;
        if (orderNo == null || orderNo.isEmpty()) return;
        lblStatus.setText("正在加载单据 " + orderNo + " 详情…");

        Task<OrderDetail> task = new Task<>() {
            @Override
            protected OrderDetail call() throws Exception {
                // 从流水反查单据 ID：先查流水列表里的 orderId... 但流水没 orderId 字段
                // 所以需要通过 GET /api/material/order/list?orderType=&orderNo= 过滤...
                // 但 list 接口不支持 orderNo 筛选。那就直接用 GET /api/material/order/{id}
                // 问题是我们只有 orderNo 没有 id。简单做法：在流水表里存 orderId，
                // 但目前没有这个字段。变通：先 GET /api/material/order/list 拿到所有单据，
                // 前端按 orderNo 匹配找到 id，再 GET 详情。
                // TODO: 后端 list 接口支持 orderNo 筛选更合理。

                // 暂时用 order/list 拉列表匹配 orderNo
                String listJson = ApiClient.get("/api/material/order/list");
                JsonNode listRoot = MAPPER.readTree(listJson);
                JsonNode orderNode = null;
                if (listRoot.has("data") && listRoot.path("data").isArray()) {
                    for (JsonNode n : listRoot.path("data")) {
                        if (orderNo.equals(n.path("orderNo").asText())) {
                            orderNode = n;
                            break;
                        }
                    }
                }
                if (orderNode == null) {
                    return new OrderDetail(null, MAPPER.createArrayNode(), "单据 " + orderNo + " 不存在");
                }
                long orderId = orderNode.path("id").asLong(-1);
                if (orderId < 0) return new OrderDetail(orderNode, MAPPER.createArrayNode(), null);

                String detailJson = ApiClient.get("/api/material/order/" + orderId);
                JsonNode detailRoot = MAPPER.readTree(detailJson);
                if (detailRoot.has("code") && detailRoot.get("code").asInt() != 0) {
                    return new OrderDetail(orderNode, MAPPER.createArrayNode(),
                            detailRoot.path("message").asText("查询失败"));
                }
                JsonNode detailData = detailRoot.path("data");
                JsonNode detailOrder = detailData.path("order");
                JsonNode items = detailData.path("items").isArray() ? detailData.path("items") : MAPPER.createArrayNode();
                return new OrderDetail(detailOrder, items, null);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            OrderDetail d = task.getValue();
            lblStatus.setText(d.error != null ? "单据详情加载失败" : "单据 " + orderNo + " 详情已加载");
            if (d.error != null) {
                Alerts.of(Alert.AlertType.WARNING, d.error).showAndWait();
                return;
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("单据详情 — " + orderNo);
            alert.setHeaderText(null);
            alert.setContentText(d.formatText());
            alert.getDialogPane().setStyle("-fx-pref-width: 600px;");
            alert.showAndWait();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable t = task.getException();
            Platform.runLater(() -> lblStatus.setText("单据详情加载失败：" + t.getMessage()));
        }));
        new Thread(task, "order-detail").start();
    }

    // ==================== 单据详情辅助 ====================

    /** 临时 DTO：单据详情聚合 */
    private static class OrderDetail {
        final JsonNode order;
        final JsonNode items;     // ArrayNode of item objects
        final String error;

        OrderDetail(JsonNode order, JsonNode items, String error) {
            this.order = order;
            this.items = items;
            this.error = error;
        }

        String formatText() {
            StringBuilder sb = new StringBuilder();
            if (order == null) return "(单据不存在)";
            sb.append("【单据号】").append(order.path("orderNo").asText("")).append('\n');
            String ot = order.path("orderType").asText("");
            sb.append("【类型】").append("1".equals(ot) ? "入库" : "2".equals(ot) ? "出库" : ot).append('\n');
            sb.append("【来源/去向】").append(order.path("sourceTarget").asText("(未填写)")).append('\n');
            sb.append("【经手人】").append(order.path("handler").asText("(未填写)")).append('\n');
            sb.append("【操作人】").append(order.path("operator").asText("(未填写)")).append('\n');
            sb.append("【备注】").append(order.path("remark").asText("(无)")).append('\n');
            sb.append("【创建时间】").append(order.path("createTime").asText("")).append('\n');
            sb.append("【明细】共 ").append(order.path("totalCount").asInt(items == null ? 0 : items.size())).append(" 条\n");
            sb.append("───\n");
            if (items != null && items.isArray()) {
                for (JsonNode it : items) {
                    String code = it.path("materialCode").asText("");
                    String name = it.path("materialName").asText("");
                    String spec = it.path("spec").asText("");
                    String num = it.path("ioNum").asText("");
                    String before = it.path("beforeStock").asText("");
                    String after = it.path("afterStock").asText("");
                    sb.append("· ").append(code).append(' ').append(name);
                    if (!spec.isBlank()) sb.append('（').append(spec).append('）');
                    sb.append("  数量 ").append(num);
                    sb.append("  ").append(before).append(" → ").append(after).append('\n');
                }
            }
            return sb.toString();
        }
    }

    // ==================== 流水表格行 ====================

    public static class RecordRow {
        public Long id;
        public Long materialId;
        public Integer ioType;       // 1入库 / 2出库
        public BigDecimal ioNum;
        public BigDecimal beforeStock;
        public BigDecimal afterStock;
        public String remark;
        public LocalDateTime createTime;
        public String operator;
        public String orderNo;
        public Integer orderType;
        public String sourceTarget;

        /** 客户端 JOIN 的物料标签 */
        public String materialLabel;

        public RecordRow() {}

        // ===== 格式化 getter =====
        public String getTime()    { return createTime == null ? "" : createTime.format(FMT); }
        public String getType()    { return ioType == null ? "" : (ioType == 1 ? "入库" : "出库"); }
        public String getOrderNo() { return orderNo == null ? "" : orderNo; }
        public String getMaterial(){ return materialLabel == null ? "" : materialLabel; }
        public String getNum()     { return ioNum == null ? "" : strip(ioNum); }
        public String getBefore()  { return beforeStock == null ? "" : strip(beforeStock); }
        public String getAfter()   { return afterStock == null ? "" : strip(afterStock); }
        public String getOperator(){ return operator == null ? "" : operator; }
        public String getSourceTarget() { return sourceTarget == null ? "" : sourceTarget; }
        public String getRemark()  { return remark == null ? "" : remark; }

        // ===== Property =====
        public javafx.beans.property.StringProperty timeProperty()         { return new javafx.beans.property.SimpleStringProperty(getTime()); }
        public javafx.beans.property.StringProperty typeProperty()         { return new javafx.beans.property.SimpleStringProperty(getType()); }
        public javafx.beans.property.StringProperty orderNoProperty()      { return new javafx.beans.property.SimpleStringProperty(getOrderNo()); }
        public javafx.beans.property.StringProperty materialProperty()     { return new javafx.beans.property.SimpleStringProperty(getMaterial()); }
        public javafx.beans.property.StringProperty numProperty()          { return new javafx.beans.property.SimpleStringProperty(getNum()); }
        public javafx.beans.property.StringProperty beforeProperty()       { return new javafx.beans.property.SimpleStringProperty(getBefore()); }
        public javafx.beans.property.StringProperty afterProperty()        { return new javafx.beans.property.SimpleStringProperty(getAfter()); }
        public javafx.beans.property.StringProperty operatorProperty()     { return new javafx.beans.property.SimpleStringProperty(getOperator()); }
        public javafx.beans.property.StringProperty sourceTargetProperty() { return new javafx.beans.property.SimpleStringProperty(getSourceTarget()); }
        public javafx.beans.property.StringProperty remarkProperty()       { return new javafx.beans.property.SimpleStringProperty(getRemark()); }

        private static String strip(BigDecimal d) { return d.stripTrailingZeros().toPlainString(); }
    }
}
