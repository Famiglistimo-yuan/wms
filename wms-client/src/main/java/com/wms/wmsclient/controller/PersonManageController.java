package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
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

import java.time.LocalDate;
import java.util.*;

/**
 * 人员档案完整实现（FR-1-2）。服务端接口：GET/POST/PUT/DELETE /api/persons。
 * FR-1-2-2 人员代码非空、FR-1-2-3 性别 ComboBox、FR-1-2-4 DatePicker、FR-1-2-5 身份证正则、FR-1-2-7 单字模糊。
 */
public class PersonManageController {

    private final ObservableList<PersonRow> rows = FXCollections.observableArrayList();

    @FXML private TextField tfSearch;
    @FXML private Button btnRefresh;
    @FXML private TableView<PersonRow> tvPersons;
    @FXML private Label lblStatus;

    public PersonManageController() {
    }

    @FXML
    protected void initialize() {
        tvPersons.setItems(rows);
        addCol("ID", "idStr", 60);
        addCol("人员代码", "personCode", 120);
        addCol("姓名", "name", 100);
        addCol("性别", "gender", 60);
        addCol("出生日期", "birthDateStr", 110);
        addCol("身份证号", "idCard", 160);
        addCol("电话", "phone", 120);
        addCol("备注", "remark", 200);
        onRefreshClicked();
    }

    private void addCol(String title, String prop, double w) {
        TableColumn<PersonRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        tvPersons.getColumns().add(c);
    }

    /** FR-1-2-7：单字模糊（搜索框输入任何单字即可按姓名 LIKE '%x%'） */
    @FXML
    protected void onRefreshClicked() {
        btnRefresh.setDisable(true);
        String keyword = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        // 任何非空输入都带 keyword；后端自动单字模糊
        String path = keyword.isEmpty()
                ? "/api/persons"
                : "/api/persons?keyword=" + ApiClient.urlEncode(keyword);

        Task<List<PersonRow>> task = new Task<>() {
            @Override
            protected List<PersonRow> call() throws Exception {
                String json = ApiClient.get(path);
                List<MapRowVO> vos = ApiClient.getResultList(json, MapRowVO.class);
                return vos.stream().map(PersonRow::new).toList();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            rows.setAll(task.getValue());
            lblStatus.setText("共 " + rows.size() + " 条" + (keyword.isEmpty() ? "" : "（关键字「" + keyword + "」）"));
            btnRefresh.setDisable(false);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            btnRefresh.setDisable(false);
            Throwable t = task.getException();
            lblStatus.setText("加载失败：" + (t instanceof ApiException ae ? ae.getMessage() : t.getMessage()));
        }));
        new Thread(task, "person-refresh").start();
    }

    @FXML
    protected void onCreateClicked() {
        PersonDialog dlg = new PersonDialog(null);
        dlg.showAndWait().ifPresent(row -> {
            Task<PersonRow> task = new Task<>() {
                @Override
                protected PersonRow call() throws Exception {
                    String json = ApiClient.post("/api/persons", row.toCreateJson());
                    return new PersonRow(ApiClient.getResultData(json, MapRowVO.class));
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                rows.add(task.getValue());
                lblStatus.setText("新增成功：" + row.personCode + " " + row.name);
            }));
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                Platform.runLater(() -> alertError("新增失败：" + msg));
            });
            new Thread(task, "person-create").start();
        });
    }

    @FXML
    protected void onEditClicked() {
        PersonRow selected = tvPersons.getSelectionModel().getSelectedItem();
        if (selected == null) { alertInfo("请先选择要修改的人员"); return; }
        PersonDialog dlg = new PersonDialog(selected);
        dlg.showAndWait().ifPresent(row -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String json = ApiClient.put("/api/persons/" + row.id, row.toUpdateJson());
                    ApiClient.checkSuccess(json);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                int idx = rows.indexOf(selected);
                rows.set(idx, row);
                lblStatus.setText("修改成功：" + row.personCode);
            }));
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                Platform.runLater(() -> alertError("修改失败：" + msg));
            });
            new Thread(task, "person-update").start();
        });
    }

    @FXML
    protected void onDeleteClicked() {
        PersonRow selected = tvPersons.getSelectionModel().getSelectedItem();
        if (selected == null) { alertInfo("请先选择要删除的人员"); return; }
        if (!confirm("确定删除人员「" + selected.name + "」（" + selected.personCode + "）？\n已关联登录账号的人员无法删除。")) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String json = ApiClient.delete("/api/persons/" + selected.id);
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
        new Thread(task, "person-delete").start();
    }

    private void alertInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void alertError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
    private boolean confirm(String msg) {
        return new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /** TableView 行模型 + Dialog 表单承载（Simple Bean，PropertyValueFactory 直接读） */
    public static class PersonRow {
        public Long id;
        public String personCode;
        public String name;
        public String gender;
        public LocalDate birthDate;
        public String idCard;
        public String phone;
        public String address;
        public String remark;

        public PersonRow() {}
        public PersonRow(MapRowVO vo) {
            this.id = vo.id;
            this.personCode = vo.personCode;
            this.name = vo.name;
            this.gender = vo.gender;
            this.birthDate = vo.birthDateStr != null && !vo.birthDateStr.isEmpty() ? LocalDate.parse(vo.birthDateStr) : null;
            this.idCard = vo.idCard;
            this.phone = vo.phone;
            this.address = vo.address;
            this.remark = vo.remark;
        }

        // PropertyValueFactory 用这些 getter
        public String getIdStr() { return id == null ? "" : id.toString(); }
        public String getBirthDateStr() { return birthDate != null ? birthDate.toString() : ""; }
        public String getPersonCode() { return personCode; }
        public String getName() { return name; }
        public String getGender() { return gender; }
        public String getIdCard() { return idCard == null ? "" : idCard; }
        public String getPhone() { return phone == null ? "" : phone; }
        public String getAddress() { return address == null ? "" : address; }
        public String getRemark() { return remark == null ? "" : remark; }

        public String toCreateJson() throws Exception {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("personCode", personCode);
            m.put("name", name);
            m.put("gender", gender);
            m.put("birthDate", birthDate != null ? birthDate.toString() : null);
            m.put("idCard", idCard);
            m.put("phone", phone);
            m.put("address", address);
            m.put("remark", remark);
            return ApiClient.toJson(m);
        }
        public String toUpdateJson() throws Exception {
            Map<String, Object> m = new LinkedHashMap<>();
            if (name != null) m.put("name", name);
            if (gender != null) m.put("gender", gender);
            if (birthDate != null) m.put("birthDate", birthDate.toString());
            if (idCard != null) m.put("idCard", idCard);
            if (phone != null) m.put("phone", phone);
            if (address != null) m.put("address", address);
            if (remark != null) m.put("remark", remark);
            return ApiClient.toJson(m);
        }
    }

    /** 服务端 VO 扁平映射 */
    public static class MapRowVO {
        public Long id;
        public String personCode;
        public String name;
        public String gender;
        public String birthDateStr;
        public String idCard;
        public String phone;
        public String address;
        public String remark;
    }

    /** 新增/修改 Dialog（内部自建 GridPane 表单） */
    private class PersonDialog extends Dialog<PersonRow> {
        private final PersonRow row;
        private final TextField tfCode = new TextField();
        private final TextField tfName = new TextField();
        private final ComboBox<String> cbGender = new ComboBox<>(FXCollections.observableArrayList("男", "女"));
        private final DatePicker dpBirth = new DatePicker();
        private final TextField tfIdCard = new TextField();
        private final TextField tfPhone = new TextField();
        private final TextField tfAddress = new TextField();
        private final TextField tfRemark = new TextField();

        PersonDialog(PersonRow existing) {
            setTitle(existing == null ? "新增人员" : "修改人员");
            setHeaderText("请填写人员信息");
            this.row = existing != null ? existing : new PersonRow();

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(8);
            grid.add(new Label("人员代码*"), 0, 0); grid.add(tfCode, 1, 0);
            grid.add(new Label("姓名*"), 0, 1);   grid.add(tfName, 1, 1);
            grid.add(new Label("性别*"), 0, 2);   grid.add(cbGender, 1, 2);
            grid.add(new Label("出生日期"), 0, 3); grid.add(dpBirth, 1, 3);
            grid.add(new Label("身份证号"), 0, 4); grid.add(tfIdCard, 1, 4);
            grid.add(new Label("电话"), 0, 5);     grid.add(tfPhone, 1, 5);
            grid.add(new Label("地址"), 0, 6);     grid.add(tfAddress, 1, 6);
            grid.add(new Label("备注"), 0, 7);     grid.add(tfRemark, 1, 7);

            if (existing != null) {
                tfCode.setText(existing.personCode);
                tfCode.setDisable(true);
                tfName.setText(existing.name);
                cbGender.setValue(existing.gender);
                dpBirth.setValue(existing.birthDate);
                tfIdCard.setText(existing.idCard);
                tfPhone.setText(existing.phone);
                tfAddress.setText(existing.address);
                tfRemark.setText(existing.remark);
            }

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                String error = validate();
                if (error != null) {
                    e.consume();
                    alertError(error);
                } else {
                    row.personCode = tfCode.getText().trim();
                    row.name = tfName.getText().trim();
                    row.gender = cbGender.getValue();
                    row.birthDate = dpBirth.getValue();
                    row.idCard = tfIdCard.getText().trim().isEmpty() ? null : tfIdCard.getText().trim();
                    row.phone = tfPhone.getText().trim().isEmpty() ? null : tfPhone.getText().trim();
                    row.address = tfAddress.getText().trim().isEmpty() ? null : tfAddress.getText().trim();
                    row.remark = tfRemark.getText().trim().isEmpty() ? null : tfRemark.getText().trim();
                }
            });
            setResultConverter(btn -> btn == ButtonType.OK ? row : null);
        }

        private String validate() {
            if (tfCode.getText() == null || tfCode.getText().isBlank()) return "人员代码不能为空";
            if (tfName.getText() == null || tfName.getText().isBlank()) return "姓名不能为空";
            if (cbGender.getValue() == null) return "请选择性别";
            String idc = tfIdCard.getText().trim();
            if (!idc.isEmpty() && !idc.matches(RegexPatterns.ID_CARD)) return "身份证号须为 18 位，最后一位为数字或 X";
            return null;
        }
    }
}
