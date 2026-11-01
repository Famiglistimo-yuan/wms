package com.wms.wmsclient.controller;

import com.wms.common.dto.PersonOptionVO;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ApiTasks;
import com.wms.wmsclient.util.TableState;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进仓 / 出仓录入（FR-2，多物料一单：勾选 + 数量 → POST /api/stock/orders）。
 * <p>
 * 两个面板（rg2402_11_12_13_stock-in / stock-out）共用本 Controller，进/出模式由
 * PanelScene 注入的面板名决定。物料列表复用子系统1 GET /api/materials?keyword=，
 * 本地分页切片（与单据查询面板交互一致：回车触发、页码钳位回显）；
 * 经手人从人员档案下拉选择（GET /api/stock/person-options）；
 * 事务/乐观锁在服务端存储过程内，出库前端仅做库存预校验改善体验。
 */
public class StockIoController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    /** 每页条数上限（本地切片保护） */
    private static final int MAX_PAGE_SIZE = 50;

    private final App app;
    /** true=进仓（IN）false=出仓（OUT），由 FXML 面板名注入 */
    private final boolean isInbound;

    /** 当前页切片（表格展示） */
    private final ObservableList<MaterialRow> rows = FXCollections.observableArrayList();
    /** 全量数据：分页切片来源 + 提交/预校验跨页收集勾选 */
    private List<MaterialRow> allRows = List.of();
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    /** 在途互斥：刷新/提交串行化，避免响应乱序覆盖 */
    private boolean loading;
    private boolean submitting;

    @FXML private Label lblTitle;
    @FXML private TextField tfSearch;
    @FXML private TableView<MaterialRow> tvMaterials;
    @FXML private Label lblStatus;
    @FXML private ComboBox<PersonOptionVO> cbHandler;
    @FXML private TextField tfRemark;
    @FXML private Button btnSubmit;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private TextField tfPageSize;
    @FXML private TextField tfJumpPage;

    /** 双参构造器：PanelScene 按面板名注入进/出模式 */
    public StockIoController(App app, String fxmlName) {
        this.app = app; // 仅为工厂注入约定保留，面板内未使用
        this.isInbound = !fxmlName.contains("stock-out");
    }

    @FXML
    protected void initialize() {
        lblTitle.setText(isInbound ? "进仓录入" : "出仓录入");
        btnSubmit.setText(isInbound ? "确认进仓" : "确认出仓");
        tfPageSize.setText(String.valueOf(pageSize));

        tvMaterials.setItems(rows);
        tvMaterials.setEditable(true);

        // ☑ 选择列（可编辑勾选）
        TableColumn<MaterialRow, Boolean> colSelected = new TableColumn<>("选择");
        colSelected.setCellValueFactory(c -> c.getValue().selectedProperty());
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setEditable(true);
        colSelected.setPrefWidth(50);
        tvMaterials.getColumns().add(colSelected);

        TableState.addCol(tvMaterials, "物料代码", "materialCode", 130);
        TableState.addCol(tvMaterials, "名称", "name", 150);
        TableState.addCol(tvMaterials, "规格型号", "spec", 120);
        TableState.addCol(tvMaterials, "单位", "unit", 70);
        TableState.addCol(tvMaterials, "当前库存", "stockStr", 100);

        // 数量列：内嵌 TextField，失焦写回行数据
        TableColumn<MaterialRow, String> colQty = new TableColumn<>(isInbound ? "进仓数量" : "出仓数量");
        colQty.setCellValueFactory(c -> c.getValue().inputQuantityProperty());
        colQty.setCellFactory(col -> new TableCell<>() {
            private final TextField tf = new TextField();
            {
                tf.setPrefWidth(120);
                tf.setOnAction(e -> tf.getParent().requestFocus());
                tf.focusedProperty().addListener((obs, oldF, newF) -> {
                    if (!newF && getTableRow() != null && getTableRow().getItem() != null) {
                        ((MaterialRow) getTableRow().getItem()).setInputQuantity(tf.getText());
                        refreshPreview();
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
        colQty.setPrefWidth(130);
        tvMaterials.getColumns().add(colQty);

        // 经手人下拉：显示「编号 姓名」，值携带 personId
        cbHandler.setConverter(new StringConverter<>() {
            @Override public String toString(PersonOptionVO p) {
                return p == null ? "" : p.getPersonCode() + " " + p.getRealName();
            }
            @Override public PersonOptionVO fromString(String s) { return null; }
        });

        loadHandlerOptions();
        onRefreshClicked();
    }

    /** 经手人下拉选项（人员档案） */
    private void loadHandlerOptions() {
        ApiTasks.run("stock-handler-options",
                () -> ApiClient.getResultList(ApiClient.get("/api/stock/person-options"), PersonOptionVO.class),
                list -> cbHandler.getItems().setAll(list),
                ex -> lblStatus.setText("经手人列表加载失败：" + ex.getMessage()));
    }

    /** 关键字过滤物料（复用子系统1 /api/materials），加载后按 id 跨页保留勾选与数量 */
    @FXML
    protected void onRefreshClicked() {
        if (loading) return;
        loading = true;
        TableState.loading(tvMaterials);
        String keyword = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        String path = keyword.isEmpty()
                ? "/api/materials"
                : "/api/materials?keyword=" + ApiClient.urlEncode(keyword);

        ApiTasks.run("stock-io-refresh",
                () -> {
                    List<MaterialRowVO> vos = ApiClient.getResultList(ApiClient.get(path), MaterialRowVO.class);
                    return vos.stream().map(MaterialRow::new).toList();
                },
                list -> {
                    loading = false;
                    // 按物料 id 回填旧行的勾选/数量（跨页），换页/筛选/刷新不丢已填内容
                    Map<Long, MaterialRow> oldById = new LinkedHashMap<>();
                    for (MaterialRow r : allRows) oldById.put(r.id, r);
                    for (MaterialRow fresh : list) {
                        MaterialRow old = oldById.get(fresh.id);
                        if (old != null) {
                            fresh.setSelected(old.isSelected());
                            fresh.setInputQuantity(old.getInputQuantity());
                        }
                    }
                    allRows = list;
                    slicePage();
                    updatePageUi();
                    lblStatus.setText("共 " + allRows.size() + " 条物料"
                            + (keyword.isEmpty() ? "" : "（关键字「" + keyword + "」）")
                            + "，勾选并填写数量后提交");
                    TableState.empty(tvMaterials, keyword.isEmpty() ? null : "没有匹配「" + keyword + "」的物料");
                    refreshPreview();
                },
                ex -> {
                    loading = false;
                    String msg = ex.getMessage();
                    lblStatus.setText("加载失败：" + msg);
                    TableState.failed(tvMaterials, msg);
                });
    }

    // ==================== 本地分页 ====================

    /** 按 page/pageSize 从 allRows 切出当前页 → rows；页码越界贴边 */
    private void slicePage() {
        if (allRows.isEmpty()) {
            rows.clear();
            return;
        }
        int totalPages = (allRows.size() + pageSize - 1) / pageSize;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, allRows.size());
        rows.setAll(allRows.subList(from, to));
    }

    private void updatePageUi() {
        int totalPages = Math.max(1, (allRows.size() + pageSize - 1) / pageSize);
        lblPage.setText(page + " / " + totalPages);
        btnPrev.setDisable(page <= 1);
        btnNext.setDisable(page >= totalPages);
    }

    @FXML
    protected void onPrevPage() {
        if (page > 1) {
            page--;
            slicePage();
            updatePageUi();
        }
    }

    @FXML
    protected void onNextPage() {
        int totalPages = Math.max(1, (allRows.size() + pageSize - 1) / pageSize);
        if (page < totalPages) {
            page++;
            slicePage();
            updatePageUi();
        }
    }

    /** 每页条数（回车触发）：钳位 1~50，非法回退默认 20，回显实际生效值 */
    @FXML
    protected void onApplyPageSize() {
        try {
            pageSize = clampPageSize(Integer.parseInt(tfPageSize.getText().trim()));
        } catch (NumberFormatException ex) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        tfPageSize.setText(String.valueOf(pageSize));
        page = 1;
        slicePage();
        updatePageUi();
    }

    /** 跳到指定页（回车触发，钳位 1~总页数；非法输入回退到第 1 页） */
    @FXML
    protected void onJumpPage() {
        int totalPages = Math.max(1, (allRows.size() + pageSize - 1) / pageSize);
        try {
            int target = Integer.parseInt(tfJumpPage.getText().trim());
            if (target < 1) target = 1;
            if (target > totalPages) target = totalPages;
            page = target;
        } catch (NumberFormatException ex) {
            page = 1;
        }
        tfJumpPage.setText(String.valueOf(page));
        slicePage();
        updatePageUi();
    }

    private int clampPageSize(int v) {
        if (v < 1) return DEFAULT_PAGE_SIZE;
        if (v > MAX_PAGE_SIZE) return MAX_PAGE_SIZE;
        return v;
    }

    // ==================== 整单预览与提交 ====================

    /** 底部整单预览：已勾 N 条 · 合计 X（出仓附加库存不足预警），扫描全量（跨页勾选） */
    private void refreshPreview() {
        int checked = 0;
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder warn = new StringBuilder();
        for (MaterialRow r : allRows) {
            if (!r.isSelected()) continue;
            checked++;
            BigDecimal qty = r.parsedQuantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            total = total.add(qty);
            if (!isInbound && r.stock.compareTo(qty) < 0) {
                warn.append(r.materialCode).append(' ');
            }
        }
        if (checked == 0) {
            lblStatus.setText("勾选需要操作的物料并填写数量…");
        } else {
            String s = "已勾 " + checked + " 条 · 合计 " + total.stripTrailingZeros().toPlainString();
            if (!warn.isEmpty()) s += "  [库存不足：" + warn + "]";
            lblStatus.setText(s);
        }
    }

    /** 提交进出仓单：POST /api/stock/orders（事务在服务端 SP 内），跨页收集勾选明细 */
    @FXML
    protected void onSubmitClicked() {
        if (submitting) return;
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MaterialRow r : allRows) {
            if (!r.isSelected()) continue;
            BigDecimal qty = r.parsedQuantity();
            if (qty == null) {
                errors.add("物料「" + r.materialCode + " " + r.name + "」未填写数量");
                continue;
            }
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("物料「" + r.materialCode + "」数量须大于 0");
                continue;
            }
            if (!isInbound && r.stock.compareTo(qty) < 0) {
                errors.add("物料「" + r.materialCode + " " + r.name + "」库存不足（当前 "
                        + r.stock.stripTrailingZeros().toPlainString()
                        + "，申请 " + qty.stripTrailingZeros().toPlainString() + "）");
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("materialId", r.id);
            m.put("quantity", qty.stripTrailingZeros().toPlainString());
            items.add(m);
        }
        if (!errors.isEmpty()) {
            Alerts.error(String.join("\n", errors));
            return;
        }
        if (items.isEmpty()) {
            Alerts.info("请至少勾选一条物料并填写数量");
            return;
        }
        PersonOptionVO handler = cbHandler.getValue();
        if (handler == null) {
            Alerts.info("请选择经手人");
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderType", isInbound ? "IN" : "OUT");
        body.put("orderDate", LocalDate.now().toString());
        body.put("handlerId", handler.getId());
        String remark = tfRemark.getText() == null ? "" : tfRemark.getText().trim();
        if (!remark.isEmpty()) body.put("remark", remark);
        body.put("items", items);

        submitting = true;
        btnSubmit.setDisable(true);
        String modeLabel = isInbound ? "进仓" : "出仓";
        ApiTasks.run("stock-order-create",
                () -> {
                    String json = ApiClient.post("/api/stock/orders", ApiClient.toJson(body));
                    return ApiClient.getResultData(json, String.class); // data = 单号
                },
                orderNo -> {
                    submitting = false;
                    btnSubmit.setDisable(false);
                    lblStatus.setText("✅ " + modeLabel + "单 " + orderNo + " 创建成功");
                    Alerts.info(modeLabel + "单 " + orderNo + " 创建成功，共 " + items.size() + " 条明细。\n可到「进出仓单查询」查看详情。");
                    // 清空全量行的勾选与数量、刷新库存显示
                    for (MaterialRow r : allRows) {
                        r.setSelected(false);
                        r.setInputQuantity("");
                    }
                    tfRemark.clear();
                    onRefreshClicked();
                },
                ex -> {
                    submitting = false;
                    btnSubmit.setDisable(false);
                    // SP 驳回（409：库存不足/物料不存在/重复等）message 原样透传
                    Alerts.error(modeLabel + "失败：\n" + ex.getMessage());
                });
    }

    // ==================== 行数据 ====================

    /** 服务端 MaterialVO 字段（/api/materials 响应） */
    public static class MaterialRowVO {
        public Long id;
        public String materialCode;
        public String name;
        public String spec;
        public String unit;
        public String stock;
    }

    /** 表格行 = 物料字段 + 勾选/数量 UI 属性 */
    public static class MaterialRow {
        public Long id;
        public String materialCode;
        public String name;
        public String spec;
        public String unit;
        public BigDecimal stock;

        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final StringProperty inputQuantity = new SimpleStringProperty("");

        public MaterialRow() {}

        public MaterialRow(MaterialRowVO vo) {
            this.id = vo.id;
            this.materialCode = vo.materialCode;
            this.name = vo.name;
            this.spec = vo.spec;
            this.unit = vo.unit;
            this.stock = vo.stock != null ? new BigDecimal(vo.stock) : BigDecimal.ZERO;
        }

        public String getMaterialCode() { return materialCode == null ? "" : materialCode; }
        public String getName() { return name == null ? "" : name; }
        public String getSpec() { return spec == null ? "" : spec; }
        public String getUnit() { return unit == null ? "" : unit; }
        public String getStockStr() { return stock == null ? "0" : stock.stripTrailingZeros().toPlainString(); }

        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean v) { selected.set(v); }
        public BooleanProperty selectedProperty() { return selected; }

        public String getInputQuantity() { return inputQuantity.get(); }
        public void setInputQuantity(String v) { inputQuantity.set(v); }
        public StringProperty inputQuantityProperty() { return inputQuantity; }

        /** 解析数量输入；空白或非法返回 null */
        public BigDecimal parsedQuantity() {
            String s = inputQuantity.get() == null ? "" : inputQuantity.get().trim();
            if (s.isEmpty()) return null;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
