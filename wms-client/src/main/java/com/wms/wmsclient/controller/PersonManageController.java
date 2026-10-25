package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ApiTasks;
import com.wms.wmsclient.util.TableState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.*;

/**
 * 人员档案完整实现（FR-1-2）。服务端接口：GET/POST/PUT/DELETE /api/persons。
 * FR-1-2-2 人员代码非空、FR-1-2-3 性别 ComboBox、FR-1-2-4 DatePicker、FR-1-2-5 身份证正则、FR-1-2-7 单字模糊。
 */
public class PersonManageController {

    private final ObservableList<PersonRow> rows = FXCollections.observableArrayList();
    /** 加载互斥：在途时忽略按钮/回车触发，串行化请求避免响应乱序覆盖 */
    private boolean loading;

    @FXML private TextField tfSearch;
    @FXML private Button btnRefresh;
    @FXML private TableView<PersonRow> tvPersons;
    @FXML private Label lblStatus;

    /** App 注入构造器（PanelScene 的 ControllerFactory 要求，ADR-007） */
    public PersonManageController(App app) {
        // app 仅为工厂注入约定保留，面板内未使用
    }

    @FXML
    protected void initialize() {
        tvPersons.setItems(rows);
        TableState.addCol(tvPersons, "ID", "idStr", 60);
        TableState.addCol(tvPersons, "人员代码", "personCode", 120);
        TableState.addCol(tvPersons, "姓名", "name", 100);
        TableState.addCol(tvPersons, "性别", "gender", 60);
        TableState.addCol(tvPersons, "出生日期", "birthDateStr", 110);
        TableState.addCol(tvPersons, "身份证号", "idCard", 160);
        TableState.addCol(tvPersons, "电话", "phone", 120);
        TableState.addCol(tvPersons, "备注", "remark", 200);
        onRefreshClicked();
    }

    /** FR-1-2-7：单字模糊（搜索框输入任何单字即可按姓名 LIKE '%x%'） */
    @FXML
    protected void onRefreshClicked() {
        if (loading) return;
        loading = true;
        btnRefresh.setDisable(true);
        TableState.loading(tvPersons);
        lblStatus.setText("加载中…");
        String keyword = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        // 任何非空输入都带 keyword；后端自动单字模糊
        String path = keyword.isEmpty()
                ? "/api/persons"
                : "/api/persons?keyword=" + ApiClient.urlEncode(keyword);

        ApiTasks.run("person-refresh",
                () -> {
                    String json = ApiClient.get(path);
                    List<MapRowVO> vos = ApiClient.getResultList(json, MapRowVO.class);
                    return vos.stream().map(PersonRow::new).toList();
                },
                list -> {
                    loading = false;
                    rows.setAll(list);
                    lblStatus.setText("共 " + rows.size() + " 条" + (keyword.isEmpty() ? "" : "（关键字「" + keyword + "」）"));
                    TableState.empty(tvPersons, keyword.isEmpty() ? null : "没有匹配「" + keyword + "」的记录");
                    btnRefresh.setDisable(false);
                },
                ex -> {
                    loading = false;
                    btnRefresh.setDisable(false);
                    String msg = ex.getMessage();
                    lblStatus.setText("加载失败：" + msg);
                    TableState.failed(tvPersons, msg);
                });
    }

    @FXML
    protected void onCreateClicked() {
        PersonDialog dlg = new PersonDialog(null);
        dlg.showAndWait().ifPresent(row -> {
            ApiTasks.run("person-create",
                    () -> {
                        String json = ApiClient.post("/api/persons", row.toCreateJson());
                        return new PersonRow(ApiClient.getResultData(json, MapRowVO.class));
                    },
                    created -> {
                        rows.add(created);
                        lblStatus.setText("新增成功：" + row.personCode + " " + row.name);
                    },
                    ex -> Alerts.error("新增失败：" + ex.getMessage()));
        });
    }

    @FXML
    protected void onEditClicked() {
        PersonRow selected = tvPersons.getSelectionModel().getSelectedItem();
        if (selected == null) { Alerts.info("请先选择要修改的人员"); return; }
        PersonDialog dlg = new PersonDialog(selected);
        dlg.showAndWait().ifPresent(row -> {
            ApiTasks.run("person-update",
                    () -> {
                        String json = ApiClient.put("/api/persons/" + row.id, row.toUpdateJson());
                        ApiClient.checkSuccess(json);
                        return null;
                    },
                    v -> {
                        int idx = rows.indexOf(selected);
                        rows.set(idx, row);
                        lblStatus.setText("修改成功：" + row.personCode);
                    },
                    ex -> Alerts.error("修改失败：" + ex.getMessage()));
        });
    }

    @FXML
    protected void onDeleteClicked() {
        PersonRow selected = tvPersons.getSelectionModel().getSelectedItem();
        if (selected == null) { Alerts.info("请先选择要删除的人员"); return; }
        if (!Alerts.confirm("确定删除人员「" + selected.name + "」（" + selected.personCode + "）？\n已关联登录账号的人员无法删除。")) return;

        ApiTasks.run("person-delete",
                () -> {
                    String json = ApiClient.delete("/api/persons/" + selected.id);
                    ApiClient.checkSuccess(json);
                    return null;
                },
                v -> {
                    rows.remove(selected);
                    lblStatus.setText("已删除");
                },
                ex -> Alerts.error("删除失败：" + ex.getMessage()));
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
        public String hometown;
        public String address;
        public String remark;
        /** 新增时勾选「同时创建登录账号」后填充 */
        public boolean createUser;
        public String username;
        public String password;

        public PersonRow() {}
        public PersonRow(MapRowVO vo) {
            this.id = vo.id;
            this.personCode = vo.personCode;
            this.name = vo.name;
            this.gender = vo.gender;
            this.birthDate = vo.birthDate != null && !vo.birthDate.isEmpty() ? LocalDate.parse(vo.birthDate) : null;
            this.idCard = vo.idCard;
            this.phone = vo.phone;
            this.hometown = vo.hometown;
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
        public String getRemark() { return remark == null ? "" : remark; }

        public String toCreateJson() throws Exception {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("personCode", personCode);
            m.put("name", name);
            m.put("gender", gender);
            m.put("birthDate", birthDate != null ? birthDate.toString() : null);
            m.put("idCard", idCard);
            m.put("phone", phone);
            m.put("hometown", hometown);
            m.put("address", address);
            m.put("remark", remark);
            if (createUser) {
                m.put("createUser", true);
                m.put("username", username);
                m.put("password", password);
            }
            return ApiClient.toJson(m);
        }
        public String toUpdateJson() throws Exception {
            // 编辑全量提交 String 字段：空串=清空，非空串=新值（birthDate 除外，null=不改）
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("gender", gender);
            if (birthDate != null) m.put("birthDate", birthDate.toString());
            m.put("idCard", idCard);
            m.put("phone", phone);
            m.put("hometown", hometown);
            m.put("address", address);
            m.put("remark", remark);
            return ApiClient.toJson(m);
        }
    }

    /** 服务端 VO 扁平映射 */
    public static class MapRowVO {
        public Long id;
        public String personCode;
        public String name;
        public String gender;
        public String birthDate;
        public String idCard;
        public String phone;
        public String hometown;
        public String address;
        public String remark;
    }

    /** 新增/修改 Dialog（内部自建 GridPane 表单） */
    private class PersonDialog extends Dialog<PersonRow> {
        private final PersonRow row;
        private final boolean isCreate;
        private final TextField tfCode = new TextField();
        private final TextField tfName = new TextField();
        private final ComboBox<String> cbGender = new ComboBox<>(FXCollections.observableArrayList("男", "女"));
        private final DatePicker dpBirth = new DatePicker();
        private final TextField tfIdCard = new TextField();
        private final TextField tfPhone = new TextField();
        private final TextField tfHometown = new TextField();
        private final TextField tfAddress = new TextField();
        private final TextField tfRemark = new TextField();
        // 仅新增模式显示：勾选后同事务创建关联登录账号（FR-4-1）
        private final CheckBox cbCreateUser = new CheckBox("同时创建登录账号");
        private final TextField tfUsername = new TextField();
        private final PasswordField pfPassword = new PasswordField();

        PersonDialog(PersonRow existing) {
            Alerts.theme(this);
            setTitle(existing == null ? "新增人员" : "修改人员");
            setHeaderText("请填写人员信息");
            this.row = existing != null ? existing : new PersonRow();
            this.isCreate = existing == null;

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(8);
            grid.add(new Label("人员代码*"), 0, 0); grid.add(tfCode, 1, 0);
            grid.add(new Label("姓名*"), 0, 1);   grid.add(tfName, 1, 1);
            grid.add(new Label("性别*"), 0, 2);   grid.add(cbGender, 1, 2);
            grid.add(new Label("出生日期"), 0, 3); grid.add(dpBirth, 1, 3);
            grid.add(new Label("身份证号"), 0, 4); grid.add(tfIdCard, 1, 4);
            grid.add(new Label("电话"), 0, 5);     grid.add(tfPhone, 1, 5);
            grid.add(new Label("籍贯"), 0, 6);     grid.add(tfHometown, 1, 6);
            grid.add(new Label("地址"), 0, 7);     grid.add(tfAddress, 1, 7);
            grid.add(new Label("备注"), 0, 8);     grid.add(tfRemark, 1, 8);

            if (isCreate) {
                tfUsername.setPromptText("字母开头，4-30 位字母/数字/下划线");
                pfPassword.setPromptText("6-20 位字母/数字");
                tfUsername.setDisable(true);
                pfPassword.setDisable(true);
                cbCreateUser.selectedProperty().addListener((obs, oldVal, checked) -> {
                    tfUsername.setDisable(!checked);
                    pfPassword.setDisable(!checked);
                });
                grid.add(cbCreateUser, 1, 9);
                grid.add(new Label("用户名"), 0, 10); grid.add(tfUsername, 1, 10);
                grid.add(new Label("初始口令"), 0, 11); grid.add(pfPassword, 1, 11);
            }

            if (existing != null) {
                tfCode.setText(existing.personCode);
                tfCode.setDisable(true);
                tfName.setText(existing.name);
                cbGender.setValue(existing.gender);
                dpBirth.setValue(existing.birthDate);
                tfIdCard.setText(existing.idCard == null ? "" : existing.idCard);
                tfPhone.setText(existing.phone == null ? "" : existing.phone);
                tfHometown.setText(existing.hometown == null ? "" : existing.hometown);
                tfAddress.setText(existing.address == null ? "" : existing.address);
                tfRemark.setText(existing.remark == null ? "" : existing.remark);
            }

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                String error = validate();
                if (error != null) {
                    e.consume();
                    Alerts.error(error);
                } else {
                    row.personCode = tfCode.getText().trim();
                    row.name = tfName.getText().trim();
                    row.gender = cbGender.getValue();
                    row.birthDate = dpBirth.getValue();
                    // 空串表示清空（服务端 UpdateDTO @Pattern ^$|... 已预留空串语义）
                    row.idCard = tfIdCard.getText() == null ? "" : tfIdCard.getText().trim();
                    row.phone = tfPhone.getText() == null ? "" : tfPhone.getText().trim();
                    row.hometown = tfHometown.getText() == null ? "" : tfHometown.getText().trim();
                    row.address = tfAddress.getText() == null ? "" : tfAddress.getText().trim();
                    row.remark = tfRemark.getText() == null ? "" : tfRemark.getText().trim();
                    if (isCreate && cbCreateUser.isSelected()) {
                        row.createUser = true;
                        row.username = tfUsername.getText().trim();
                        row.password = pfPassword.getText();
                    }
                }
            });
            setResultConverter(btn -> btn == ButtonType.OK ? row : null);
        }

        private String validate() {
            if (tfCode.getText() == null || tfCode.getText().isBlank()) return "人员代码不能为空";
            if (tfName.getText() == null || tfName.getText().isBlank()) return "姓名不能为空";
            if (cbGender.getValue() == null) return "请选择性别";
            String idc = tfIdCard.getText() == null ? "" : tfIdCard.getText().trim();
            if (!idc.isEmpty() && !idc.matches(RegexPatterns.ID_CARD)) return "身份证号须为 18 位，最后一位为数字或 X";
            if (isCreate && cbCreateUser.isSelected()) {
                String un = tfUsername.getText() == null ? "" : tfUsername.getText().trim();
                if (!un.matches(RegexPatterns.USERNAME)) return "用户名须字母开头，4-30 位字母/数字/下划线";
                String pw = pfPassword.getText() == null ? "" : pfPassword.getText();
                if (!pw.matches(RegexPatterns.PASSWORD)) return "口令须为 6-20 位字母/数字";
            }
            return null;
        }
    }
}
