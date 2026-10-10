package com.wms.wmsclient.controller;

import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.util.*;

/**
 * 物料档案完整实现（FR-1-3）。服务端接口：GET/POST/PUT/DELETE /api/materials。
 * FR-1-3-2 代码非空唯一、FR-1-3-3 单位可选+可输入、FR-1-3-4 库存默认 0、FR-1-3-5 三字段模糊。
 */
public class MaterialManageController {

    /** FR-1-3-3 预设单位，ComboBox 还允许自由输入 */
    private static final List<String> UNIT_PRESETS = List.of("件", "套", "公斤", "吨", "升", "米", "毫米", "个", "箱", "包");

    private final ObservableList<MaterialRow> rows = FXCollections.observableArrayList();

    @FXML private TextField tfSearch;
    @FXML private TableView<MaterialRow> tvMaterials;
    @FXML private Label lblStatus;

    public MaterialManageController() {}

    @FXML
    protected void initialize() {
        tvMaterials.setItems(rows);
        addCol("ID", "idStr", 60);
        addCol("物料代码", "materialCode", 130);
        addCol("名称", "name", 150);
        addCol("规格型号", "spec", 150);
        addCol("单位", "unit", 70);
        addCol("库存", "stockStr", 100);
        addCol("备注", "remark", 200);
        onRefreshClicked();
    }

    private void addCol(String title, String prop, double w) {
        TableColumn<MaterialRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        tvMaterials.getColumns().add(c);
    }

    /** FR-1-3-5：keyword 同时匹配代码/名称/规格 */
    @FXML
    protected void onRefreshClicked() {
        String keyword = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        String path = keyword.isEmpty()
                ? "/api/materials"
                : "/api/materials?keyword=" + ApiClient.urlEncode(keyword);

        Task<List<MaterialRow>> task = new Task<>() {
            @Override
            protected List<MaterialRow> call() throws Exception {
                String json = ApiClient.get(path);
                List<MapRowVO> vos = ApiClient.getResultList(json, MapRowVO.class);
                return vos.stream().map(MaterialRow::new).toList();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            rows.setAll(task.getValue());
            lblStatus.setText("共 " + rows.size() + " 条" + (keyword.isEmpty() ? "" : "（关键字「" + keyword + "」）"));
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable t = task.getException();
            lblStatus.setText("加载失败：" + (t instanceof ApiException ae ? ae.getMessage() : t.getMessage()));
        }));
        new Thread(task, "material-refresh").start();
    }

    @FXML
    protected void onCreateClicked() {
        MaterialDialog dlg = new MaterialDialog(null);
        dlg.showAndWait().ifPresent(row -> {
            Task<MaterialRow> task = new Task<>() {
                @Override
                protected MaterialRow call() throws Exception {
                    String json = ApiClient.post("/api/materials", row.toCreateJson());
                    return new MaterialRow(ApiClient.getResultData(json, MapRowVO.class));
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                rows.add(task.getValue());
                lblStatus.setText("新增成功：" + row.materialCode + " " + row.name);
            }));
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                Platform.runLater(() -> alertError("新增失败：" + msg));
            });
            new Thread(task, "material-create").start();
        });
    }

    @FXML
    protected void onEditClicked() {
        MaterialRow selected = tvMaterials.getSelectionModel().getSelectedItem();
        if (selected == null) { alertInfo("请先选择要修改的物料"); return; }
        MaterialDialog dlg = new MaterialDialog(selected);
        dlg.showAndWait().ifPresent(row -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String json = ApiClient.put("/api/materials/" + row.id, row.toUpdateJson());
                    ApiClient.checkSuccess(json);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                int idx = rows.indexOf(selected);
                rows.set(idx, row);
                lblStatus.setText("修改成功：" + row.materialCode);
            }));
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                Platform.runLater(() -> alertError("修改失败：" + msg));
            });
            new Thread(task, "material-update").start();
        });
    }

    @FXML
    protected void onDeleteClicked() {
        MaterialRow selected = tvMaterials.getSelectionModel().getSelectedItem();
        if (selected == null) { alertInfo("请先选择要删除的物料"); return; }
        if (!confirm("确定删除物料「" + selected.name + "」（" + selected.materialCode + "）？\n已被进出仓单引用的物料无法删除。")) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String json = ApiClient.delete("/api/materials/" + selected.id);
                ApiClient.checkSuccess(json);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            rows.remove(selected);
            lblStatus.setText("已删除");
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> alertError("删除失败：" + msg));
        });
        new Thread(task, "material-delete").start();
    }

    private void alertInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void alertError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
    private boolean confirm(String msg) {
        return new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public static class MaterialRow {
        public Long id;
        public String materialCode;
        public String name;
        public String spec;
        public String unit;
        public BigDecimal stock;
        public String remark;

        public MaterialRow() {}
        public MaterialRow(MapRowVO vo) {
            this.id = vo.id;
            this.materialCode = vo.materialCode;
            this.name = vo.name;
            this.spec = vo.spec;
            this.unit = vo.unit;
            this.stock = vo.stock != null ? new BigDecimal(vo.stock) : BigDecimal.ZERO;
            this.remark = vo.remark;
        }

        public String getIdStr() { return id == null ? "" : id.toString(); }
        public String getMaterialCode() { return materialCode; }
        public String getName() { return name; }
        public String getSpec() { return spec == null ? "" : spec; }
        public String getUnit() { return unit == null ? "" : unit; }
        public String getStockStr() { return stock != null ? stock.stripTrailingZeros().toPlainString() : "0"; }
        public String getRemark() { return remark == null ? "" : remark; }

        public String toCreateJson() throws Exception {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("materialCode", materialCode);
            m.put("name", name);
            m.put("spec", spec);
            m.put("unit", unit);
            m.put("stock", stock != null ? stock : BigDecimal.ZERO);
            m.put("remark", remark);
            return ApiClient.toJson(m);
        }
        public String toUpdateJson() throws Exception {
            Map<String, Object> m = new LinkedHashMap<>();
            if (name != null) m.put("name", name);
            if (spec != null) m.put("spec", spec);
            if (unit != null) m.put("unit", unit);
            if (stock != null) m.put("stock", stock);
            if (remark != null) m.put("remark", remark);
            return ApiClient.toJson(m);
        }
    }

    public static class MapRowVO {
        public Long id;
        public String materialCode;
        public String name;
        public String spec;
        public String unit;
        public String stock;
        public String remark;
    }

    private class MaterialDialog extends Dialog<MaterialRow> {
        private final MaterialRow row;
        private final TextField tfCode = new TextField();
        private final TextField tfName = new TextField();
        private final TextField tfSpec = new TextField();
        private final ComboBox<String> cbUnit = new ComboBox<>(FXCollections.observableArrayList(UNIT_PRESETS));
        private final TextField tfStock = new TextField("0"); // FR-1-3-4 默认 0
        private final TextField tfRemark = new TextField();

        MaterialDialog(MaterialRow existing) {
            setTitle(existing == null ? "新增物料" : "修改物料");
            setHeaderText("请填写物料信息");
            this.row = existing != null ? existing : new MaterialRow();
            cbUnit.setEditable(true); // FR-1-3-3 可选 + 可输入

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(8);
            grid.add(new Label("物料代码*"), 0, 0); grid.add(tfCode, 1, 0);
            grid.add(new Label("名称*"), 0, 1);     grid.add(tfName, 1, 1);
            grid.add(new Label("规格型号"), 0, 2); grid.add(tfSpec, 1, 2);
            grid.add(new Label("单位*"), 0, 3);     grid.add(cbUnit, 1, 3);
            grid.add(new Label("库存"), 0, 4);     grid.add(tfStock, 1, 4);
            grid.add(new Label("备注"), 0, 5);     grid.add(tfRemark, 1, 5);

            if (existing != null) {
                tfCode.setText(existing.materialCode);
                tfCode.setDisable(true);
                tfName.setText(existing.name);
                tfSpec.setText(existing.spec);
                cbUnit.setValue(existing.unit);
                tfStock.setText(existing.stock != null ? existing.stock.stripTrailingZeros().toPlainString() : "0");
                tfRemark.setText(existing.remark);
            }

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                String error = validate();
                if (error != null) { e.consume(); alertError(error); }
                else {
                    row.materialCode = tfCode.getText().trim();
                    row.name = tfName.getText().trim();
                    row.spec = tfSpec.getText().trim().isEmpty() ? null : tfSpec.getText().trim();
                    row.unit = cbUnit.getValue() != null ? cbUnit.getValue().trim() : null;
                    String stockStr = tfStock.getText().trim();
                    row.stock = stockStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(stockStr);
                    row.remark = tfRemark.getText().trim().isEmpty() ? null : tfRemark.getText().trim();
                }
            });
            setResultConverter(btn -> btn == ButtonType.OK ? row : null);
        }

        private String validate() {
            if (tfCode.getText() == null || tfCode.getText().isBlank()) return "物料代码不能为空";
            if (tfName.getText() == null || tfName.getText().isBlank()) return "物料名称不能为空";
            String unitVal = cbUnit.getValue();
            if (unitVal == null || unitVal.isBlank()) return "请选择或输入计量单位";
            String stockStr = tfStock.getText().trim();
            if (!stockStr.isEmpty()) {
                try {
                    BigDecimal s = new BigDecimal(stockStr);
                    if (s.compareTo(BigDecimal.ZERO) < 0) return "库存不能为负";
                } catch (NumberFormatException ex) { return "库存须为数字"; }
            }
            return null;
        }
    }
}
