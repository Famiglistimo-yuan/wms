package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 进出库流水查询面板：支持类型/单据号/操作人筛选 + 分页 + 每页条数。
 * 后端接口：POST /api/material/io-records/page
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
    @FXML private ComboBox<String> cbFilter;     // 全部/入库/出库
    @FXML private TextField tfOrderNo;            // 单据号筛选
    @FXML private TextField tfOperator;           // 操作人筛选
    @FXML private TableView<RecordRow> tvRecords;
    @FXML private TableColumn<RecordRow, String> colTime;
    @FXML private TableColumn<RecordRow, String> colType;
    @FXML private TableColumn<RecordRow, String> colOrderNo;
    @FXML private TableColumn<RecordRow, String> colMaterial;
    @FXML private TableColumn<RecordRow, String> colNum;
    @FXML private TableColumn<RecordRow, String> colBefore;
    @FXML private TableColumn<RecordRow, String> colAfter;
    @FXML private TableColumn<RecordRow, String> colOperator;
    @FXML private TableColumn<RecordRow, String> colRemark;
    @FXML private Label lblStatus;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private ComboBox<Integer> cbPageSize;

    public StockIoRecordController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        lblTitle.setText("单据查询（进出库流水）");

        // 筛选下拉
        cbFilter.setItems(FXCollections.observableArrayList("全部", "入库", "出库"));
        cbFilter.setValue("全部");

        // 每页条数
        cbPageSize.setItems(FXCollections.observableArrayList(10, 20, 50));
        cbPageSize.setValue(20);
        cbPageSize.setOnAction(e -> {
            pageSize = cbPageSize.getValue();
            page = 1;
            onRefreshClicked();
        });

        // ===== TableView 列 =====
        tvRecords.setEditable(false);
        tvRecords.setItems(rows);

        colTime.setCellValueFactory(c -> c.getValue().timeProperty());
        colType.setCellValueFactory(c -> c.getValue().typeProperty());
        colType.setCellFactory(col -> typeCell());
        colOrderNo.setCellValueFactory(c -> c.getValue().orderNoProperty());
        colMaterial.setCellValueFactory(c -> c.getValue().materialProperty());
        colNum.setCellValueFactory(c -> c.getValue().numProperty());
        colBefore.setCellValueFactory(c -> c.getValue().beforeProperty());
        colAfter.setCellValueFactory(c -> c.getValue().afterProperty());
        colOperator.setCellValueFactory(c -> c.getValue().operatorProperty());
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
        String operator = tfOperator.getText() == null ? "" : tfOperator.getText().trim();
        if (!orderNo.isEmpty()) body.put("orderNo", orderNo);
        if (!operator.isEmpty()) body.put("operator", operator);

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
        tfOrderNo.clear();
        tfOperator.clear();
        page = 1;
        pageSize = cbPageSize.getValue() == null ? 20 : cbPageSize.getValue();
        onRefreshClicked();
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
        public String getRemark()  { return remark == null ? "" : remark; }

        // ===== Property =====
        public javafx.beans.property.StringProperty timeProperty()    { return new javafx.beans.property.SimpleStringProperty(getTime()); }
        public javafx.beans.property.StringProperty typeProperty()    { return new javafx.beans.property.SimpleStringProperty(getType()); }
        public javafx.beans.property.StringProperty orderNoProperty() { return new javafx.beans.property.SimpleStringProperty(getOrderNo()); }
        public javafx.beans.property.StringProperty materialProperty(){ return new javafx.beans.property.SimpleStringProperty(getMaterial()); }
        public javafx.beans.property.StringProperty numProperty()     { return new javafx.beans.property.SimpleStringProperty(getNum()); }
        public javafx.beans.property.StringProperty beforeProperty()  { return new javafx.beans.property.SimpleStringProperty(getBefore()); }
        public javafx.beans.property.StringProperty afterProperty()   { return new javafx.beans.property.SimpleStringProperty(getAfter()); }
        public javafx.beans.property.StringProperty operatorProperty(){ return new javafx.beans.property.SimpleStringProperty(getOperator()); }
        public javafx.beans.property.StringProperty remarkProperty()  { return new javafx.beans.property.SimpleStringProperty(getRemark()); }

        private static String strip(BigDecimal d) { return d.stripTrailingZeros().toPlainString(); }
    }
}
