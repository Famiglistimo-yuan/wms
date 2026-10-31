package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import com.wms.wmsclient.util.Alerts;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

/**
 * 多物料进仓 / 出仓共用面板控制器。
 * <p>
 * UI：TableView 列出所有物料（☑ 选择 + 编码 + 名称 + 规格 + 单位 + 当前库存 + 出入数量），
 * 用户勾选需要的物料，各自填数量，点确认一次性批量提交。
 * <p>
 * 两种模式（进仓 / 出仓）由 App.STOCK_MODE_INBOUND 决定，
 * FXML 文件名对应设置（App.showPanel 里已处理）。
 * <p>
 * 后端接口：GET /api/material/list、POST /api/material/in/batch、POST /api/material/out/batch
 */
public class StockIoController {

    private final App app;
    private final boolean isInbound;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObservableList<MaterialRow> rows = FXCollections.observableArrayList();

    @FXML private Label lblTitle;
    @FXML private Button btnSubmit;
    @FXML private TableView<MaterialRow> tvMaterials;
    @FXML private TableColumn<MaterialRow, Boolean> colSelected;
    @FXML private TableColumn<MaterialRow, String> colCode;
    @FXML private TableColumn<MaterialRow, String> colName;
    @FXML private TableColumn<MaterialRow, String> colSpec;
    @FXML private TableColumn<MaterialRow, String> colUnit;
    @FXML private TableColumn<MaterialRow, String> colStock;
    @FXML private TableColumn<MaterialRow, String> colInput;
    @FXML private TextField tfSource;
    @FXML private TextField tfRemark;
    @FXML private Label lblStatus;

    public StockIoController(App app) {
        this.app = app;
        this.isInbound = App.STOCK_MODE_INBOUND;
    }

    @FXML
    protected void initialize() {
        // 标题 + 提交按钮文字（FXML 里已经硬编码了一次，这里兜底，确保一致）
        lblTitle.setText(isInbound ? "进仓录入（多物料）" : "出仓录入（多物料）");
        btnSubmit.setText(isInbound ? "确认进仓" : "确认出仓");
        btnSubmit.setStyle(isInbound
                ? "-fx-background-color: #1976d2; -fx-text-fill: white; -fx-padding: 6px 20px;"
                : "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-padding: 6px 20px;");

        // ===== TableView 配置 =====
        tvMaterials.setEditable(true);
        tvMaterials.setItems(rows);

        // ☑ 选择列
        colSelected.setCellValueFactory(c -> c.getValue().selectedProperty());
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setEditable(true);
        colSelected.setPrefWidth(50);

        // 物料编码
        colCode.setCellValueFactory(new PropertyValueFactory<>("materialCode"));
        colCode.setPrefWidth(110);

        // 名称
        colName.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        colName.setPrefWidth(120);

        // 规格
        colSpec.setCellValueFactory(new PropertyValueFactory<>("spec"));
        colSpec.setPrefWidth(100);

        // 单位
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colUnit.setPrefWidth(60);

        // 当前库存
        colStock.setCellValueFactory(c -> c.getValue().stockDisplayProperty());
        colStock.setPrefWidth(110);

        // 出入数量（始终显示 TextField，实时校验 + 失焦写回 + 触发整单汇总）
        colInput.setCellValueFactory(c -> c.getValue().inputQuantityProperty());
        colInput.setCellFactory(column -> new TableCell<MaterialRow, String>() {
            private final TextField tf = new TextField();
            {
                tf.setStyle("-fx-max-width: 150px;");
                tf.textProperty().addListener((obs, oldV, newV) -> {
                    if (getTableRow() == null || getTableRow().getItem() == null) return;
                    MaterialRow row = (MaterialRow) getTableRow().getItem();
                    // 实时校验：空 / 非正数 / 数字格式 / 出库库存不足
                    String trimmed = newV == null ? "" : newV.trim();
                    String errStyle = "-fx-control-inner-background: #ffe0e0;";
                    String okStyle  = "-fx-control-inner-background: white;";
                    if (trimmed.isEmpty()) {
                        tf.setStyle("-fx-max-width: 150px;");
                    } else {
                        try {
                            BigDecimal n = new BigDecimal(trimmed);
                            if (n.compareTo(BigDecimal.ZERO) <= 0) {
                                tf.setStyle("-fx-max-width: 150px;" + errStyle);
                                tf.setTooltip(new Tooltip("数量必须大于 0"));
                            } else if (!isInbound && row.stockNum != null
                                    && row.stockNum.compareTo(n) < 0) {
                                tf.setStyle("-fx-max-width: 150px;" + errStyle);
                                tf.setTooltip(new Tooltip("库存不足（当前 "
                                        + row.stockNum.stripTrailingZeros().toPlainString() + ")"));
                            } else {
                                tf.setStyle("-fx-max-width: 150px;" + okStyle);
                                tf.setTooltip(null);
                            }
                        } catch (NumberFormatException ex) {
                            tf.setStyle("-fx-max-width: 150px;" + errStyle);
                            tf.setTooltip(new Tooltip("请输入数字"));
                        }
                    }
                });
                tf.focusedProperty().addListener((obs, oldF, newF) -> {
                    if (!newF) {
                        // 失焦：写回 row + 刷新整单汇总
                        if (getTableRow() != null && getTableRow().getItem() != null) {
                            MaterialRow row = (MaterialRow) getTableRow().getItem();
                            String val = tf.getText() == null ? "" : tf.getText().trim();
                            row.setInputQuantity(val);
                            refreshOrderPreview();
                        }
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else { tf.setText(item == null ? "" : item); setGraphic(tf); }
            }
        });
        colInput.setEditable(false);
        colInput.setPrefWidth(150);

        // 勾选变化也触发整单汇总
        rows.addListener((javafx.collections.ListChangeListener<MaterialRow>) c -> refreshOrderPreview());
        colSelected.setCellFactory(col -> new CheckBoxTableCell<>());
        colSelected.setCellValueFactory(c -> c.getValue().selectedProperty());

        // 加载数据
        onRefreshClicked();
    }

    /** 底部状态栏实时整单预览：已勾 N 条 / 合计 X / 出库库存不足⚠ */
    private void refreshOrderPreview() {
        int checked = 0;
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder warn = new StringBuilder();
        for (MaterialRow r : rows) {
            if (!r.isSelected()) continue;
            checked++;
            String qs = r.getInputQuantity() == null ? "" : r.getInputQuantity().trim();
            if (qs.isEmpty()) continue;
            try {
                BigDecimal q = new BigDecimal(qs);
                if (q.compareTo(BigDecimal.ZERO) > 0) total = total.add(q);
                if (!isInbound && r.stockNum != null && r.stockNum.compareTo(q) < 0) {
                    warn.append("⚠").append(r.materialCode).append(' ');
                }
            } catch (NumberFormatException ignored) {}
        }
        if (checked == 0) {
            lblStatus.setText("勾选需要操作的物料并填写数量…");
        } else {
            String s = "已勾 " + checked + " 条 · 合计 " + total.stripTrailingZeros().toPlainString();
            if (!warn.isEmpty()) s += "  [" + warn + "] 库存不足";
            lblStatus.setText(s);
        }
    }

    /** 刷新物料列表填充 TableView */
    @FXML
    protected void onRefreshClicked() {
        lblStatus.setText("正在加载物料列表…");
        Task<List<MaterialRow>> task = new Task<>() {
            @Override
            protected List<MaterialRow> call() throws Exception {
                String json = ApiClient.get("/api/material/list");
                JsonNode root = MAPPER.readTree(json);
                JsonNode dataNode;
                if (root.isArray()) {
                    dataNode = root; // 兼容裸数组
                } else if (root.isObject() && root.has("data") && root.get("data").isArray()) {
                    dataNode = root.get("data");
                } else {
                    throw new RuntimeException("无法解析物料列表响应格式");
                }
                List<MaterialRow> result = new ArrayList<>();
                for (JsonNode node : dataNode) {
                    result.add(MAPPER.treeToValue(node, MaterialRow.class));
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<MaterialRow> list = task.getValue();
            rows.setAll(list);
            lblStatus.setText("共 " + rows.size() + " 条物料，勾选需要操作的行，填写数量后点击「"
                    + (isInbound ? "进仓" : "出仓") + "」");
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable t = task.getException();
            lblStatus.setText("加载物料失败：" + (t instanceof ApiException ae ? ae.getMessage() : t.getMessage()));
        }));
        new Thread(task, "stock-io-refresh").start();
    }

    /** 提交批量进仓 / 出仓 */
    @FXML
    protected void onSubmitClicked() {
        // 收集勾选行 + 数量校验
        List<BatchItem> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MaterialRow row : rows) {
            if (!row.isSelected()) continue;
            String qtyStr = row.getInputQuantity() == null ? "" : row.getInputQuantity().trim();
            if (qtyStr.isEmpty()) {
                errors.add("物料「" + row.materialCode + " " + row.materialName + "」未填写数量");
                continue;
            }
            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyStr);
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("物料「" + row.materialCode + "」数量须大于 0");
                    continue;
                }
            } catch (NumberFormatException ex) {
                errors.add("物料「" + row.materialCode + "」数量格式错误：" + qtyStr);
                continue;
            }

            // 前端库存预校验（出仓）
            if (!isInbound && row.stockNum != null && row.stockNum.compareTo(qty) < 0) {
                errors.add("物料「" + row.materialCode + " " + row.materialName
                        + "」库存不足（当前 " + row.stockNum.stripTrailingZeros().toPlainString()
                        + "，申请 " + qty.stripTrailingZeros().toPlainString() + "）");
                continue;
            }

            items.add(new BatchItem(row.id, qty));
        }

        if (!errors.isEmpty()) {
            alert(String.join("\n", errors));
            return;
        }
        if (items.isEmpty()) {
            alert("请至少勾选一条物料并填写数量");
            return;
        }

        String remark = tfRemark.getText() == null ? "" : tfRemark.getText().trim();
        String source  = tfSource  == null || tfSource.getText() == null ? "" : tfSource.getText().trim();
        boolean inbound = isInbound;

        // 构造单据请求体（新接口 order/create）
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderType", inbound ? 1 : 2);
        if (inbound) body.put("source", source.isEmpty() ? null : source);
        body.put("remark", remark.isEmpty() ? null : remark);
        body.put("items", items);

        String jsonBody;
        try {
            jsonBody = ApiClient.toJson(body);
        } catch (Exception ex) {
            alert("序列化请求失败：" + ex.getMessage());
            return;
        }

        String url = "/api/material/order/create";
        int selectedCount = items.size();

        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                String json = ApiClient.post(url, jsonBody);
                JsonNode root = MAPPER.readTree(json);
                if (root.isObject() && root.has("code")) {
                    int code = root.get("code").asInt();
                    if (code != 0) {
                        String msg = root.path("message").asText("未知错误");
                        if (root.has("data") && !root.path("data").asText("").isEmpty()) {
                            msg = msg + "：" + root.path("data").asText();
                        }
                        throw new ApiException(code, msg);
                    }
                    return root.path("data"); // 完整的 StockOrder JSON 对象
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            String modeLabel = inbound ? "进仓" : "出仓";
            JsonNode orderData = task.getValue();
            String orderNo = orderData != null ? orderData.path("orderNo").asText("") : "";
            BigDecimal totalNum = orderData != null
                    ? new BigDecimal(orderData.path("totalNum").asText("0")) : BigDecimal.ZERO;

            String successMsg = "✅ " + modeLabel + "单 " + orderNo
                    + " 创建成功，共 " + selectedCount + " 条 · 合计 "
                    + totalNum.stripTrailingZeros().toPlainString();
            lblStatus.setText(successMsg);
            // 弹窗让用户看到单号（关闭后清空）
            Alerts.of(Alert.AlertType.INFORMATION, successMsg
                    + "\n可以到「单据查询」查看明细").showAndWait();

            // 清空数量输入 + 取消勾选 + 刷新
            for (MaterialRow row : rows) {
                row.setSelected(false);
                row.setInputQuantity("");
            }
            tfRemark.clear();
            if (tfSource != null) tfSource.clear();
            refreshOrderPreview();
            onRefreshClicked();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            String modeLabel = inbound ? "进仓" : "出仓";
            lblStatus.setText("❌ " + modeLabel + "失败：" + msg);
            Alerts.of(Alert.AlertType.ERROR, modeLabel + "失败：\n" + msg).showAndWait();
        }));
        new Thread(task, "stock-io-batch").start();
    }

    private void alert(String msg) {
        Alerts.of(Alert.AlertType.WARNING, msg).showAndWait();
    }

    // ==================== 新增 / 编辑物料 ====================

    @FXML
    protected void onAddMaterialClicked() {
        MaterialDialog dlg = new MaterialDialog("新增物料档案", null);
        Optional<MaterialDialog.Result> opt = dlg.showAndWait();
        if (opt.isEmpty()) return;
        MaterialDialog.Result r = opt.get();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("materialCode", r.code);
        body.put("materialName", r.name);
        body.put("spec", r.spec);
        body.put("unit", r.unit);

        new Thread(() -> {
            try {
                String json = ApiClient.post("/api/material/add", ApiClient.toJson(body));
                ApiClient.checkSuccess(json);
                Platform.runLater(() -> {
                    lblStatus.setText("✅ 物料「" + r.code + " " + r.name + "」已新增，已自动选中");
                    onRefreshClicked();
                    // 刷新后自动选中新物料（新增行默认被勾选）
                    new Thread(() -> {
                        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            for (MaterialRow row : rows) {
                                if (r.code.equals(row.materialCode)) {
                                    row.setSelected(true);
                                    tvMaterials.getSelectionModel().select(row);
                                    break;
                                }
                            }
                        });
                    }, "auto-select").start();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR,
                        "新增物料失败：" + ex.getMessage()).showAndWait());
            }
        }, "material-add").start();
    }

    @FXML
    protected void onEditMaterialClicked() {
        MaterialRow selected = tvMaterials.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("请先在表格里选中一条要编辑的物料");
            return;
        }
        MaterialDialog dlg = new MaterialDialog("编辑物料档案", selected);
        Optional<MaterialDialog.Result> opt = dlg.showAndWait();
        if (opt.isEmpty()) return;
        MaterialDialog.Result r = opt.get();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", selected.id);
        body.put("materialCode", r.code);
        body.put("materialName", r.name);
        body.put("spec", r.spec);
        body.put("unit", r.unit);
        // 保持当前库存，不让用户通过编辑接口改
        body.put("stockNum", selected.stockNum);

        new Thread(() -> {
            try {
                String json = ApiClient.post("/api/material/update", ApiClient.toJson(body));
                ApiClient.checkSuccess(json);
                Platform.runLater(() -> {
                    lblStatus.setText("✅ 物料「" + r.code + " " + r.name + "」已更新");
                    onRefreshClicked();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR,
                        "修改物料失败：" + ex.getMessage()).showAndWait());
            }
        }, "material-update").start();
    }

    /**
     * 物料档案编辑 Dialog（新增 / 编辑共用）。
     * 返回编码、名称、规格、单位；取消返回 Optional.empty()。
     */
    private static class MaterialDialog {
        private final Dialog<Result> dialog;
        private final TextField tfCode = new TextField();
        private final TextField tfName = new TextField();
        private final TextField tfSpec = new TextField();
        private final TextField tfUnit = new TextField();

        /**
         * @param title Dialog 标题
         * @param row   编辑时传选中行（回填旧值）；新增时传 null
         */
        MaterialDialog(String title, MaterialRow row) {
            dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setHeaderText(null);
            dialog.getDialogPane().setContent(buildPane());

            if (row != null) {
                tfCode.setText(row.materialCode);
                tfName.setText(row.materialName);
                tfSpec.setText(row.spec == null ? "" : row.spec);
                tfUnit.setText(row.unit == null ? "" : row.unit);
            }

            ButtonType okBtn = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> {
                if (btn != okBtn) return null;
                String code = tfCode.getText().trim();
                String name = tfName.getText().trim();
                if (code.isEmpty()) { Alerts.of(Alert.AlertType.WARNING, "物料编码不能为空").showAndWait(); return null; }
                if (name.isEmpty()) { Alerts.of(Alert.AlertType.WARNING, "物料名称不能为空").showAndWait(); return null; }
                return new Result(code, name, tfSpec.getText().trim(), tfUnit.getText().trim());
            });
        }

        Optional<Result> showAndWait() { return dialog.showAndWait(); }

        private javafx.scene.layout.GridPane buildPane() {
            javafx.scene.layout.GridPane gp = new javafx.scene.layout.GridPane();
            gp.setHgap(8); gp.setVgap(8);
            gp.setStyle("-fx-padding: 10px;");
            gp.add(new Label("物料编码*："), 0, 0); gp.add(tfCode, 1, 0);
            gp.add(new Label("物料名称*："), 0, 1); gp.add(tfName, 1, 1);
            gp.add(new Label("规格："),       0, 2); gp.add(tfSpec,  1, 2);
            gp.add(new Label("单位："),       0, 3); gp.add(tfUnit,  1, 3);
            tfCode.setPrefWidth(250);
            return gp;
        }

        record Result(String code, String name, String spec, String unit) {}
    }

    /** TableView 行数据：后端 Material 字段 + 本地 UI 字段（selected + inputQuantity） */
    public static class MaterialRow {

        // ===== 后端字段（反序列化用） =====
        public Long id;
        public String materialCode;
        public String materialName;
        public String spec;
        public String unit;
        public BigDecimal stockNum;

        // ===== UI 字段（JavaFX 属性） =====
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final StringProperty inputQuantity = new SimpleStringProperty("");

        public MaterialRow() {}

        // ===== 后端字段的 getter（PropertyValueFactory 需要） =====
        public Long getId() { return id; }
        public String getMaterialCode() { return materialCode; }
        public String getMaterialName() { return materialName; }
        public String getSpec() { return spec; }
        public String getUnit() { return unit; }
        public BigDecimal getStockNum() { return stockNum; }

        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean v) { selected.set(v); }
        public BooleanProperty selectedProperty() { return selected; }

        public String getInputQuantity() { return inputQuantity.get(); }
        public void setInputQuantity(String v) { inputQuantity.set(v); }
        public StringProperty inputQuantityProperty() { return inputQuantity; }

        /** 右侧 stock 列显示文本 */
        public StringProperty stockDisplayProperty() {
            String stock = stockNum != null ? stockNum.stripTrailingZeros().toPlainString() : "0";
            String u = (unit != null && !unit.isBlank()) ? unit : "";
            return new SimpleStringProperty(stock + u);
        }
    }

    /** 批量提交请求体中的单个条目 */
    public static class BatchItem {
        public Long materialId;
        public BigDecimal ioNum;
        public BatchItem() {}
        public BatchItem(Long materialId, BigDecimal ioNum) {
            this.materialId = materialId;
            this.ioNum = ioNum;
        }
    }
}
