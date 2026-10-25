package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
import com.wms.wmsclient.App;
import com.wms.wmsclient.dto.PersonOptionVO;
import com.wms.wmsclient.dto.UserVO;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.TableState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理界面：列表 + 新增/启停用/删除。
 */
public class UserManageController {

    @FXML private TableView<UserRow> tvUsers;
    @FXML private Label lblStatus;

    private final ObservableList<UserRow> rows = FXCollections.observableArrayList();
    /** 加载互斥：在途时忽略刷新触发，串行化请求避免响应乱序覆盖 */
    private boolean loading;
    private List<PersonOptionVO> personOptions;

    /** App 注入构造器（PanelScene 的 ControllerFactory 要求，ADR-007） */
    public UserManageController(App app) {
        // app 仅为工厂注入约定保留，面板内未使用
    }

    @FXML
    protected void initialize() {
        TableState.addCol(tvUsers, "ID", "id", 60);
        TableState.addCol(tvUsers, "用户名", "username", 150);
        TableState.addCol(tvUsers, "人员编号", "personCode", 100);
        TableState.addCol(tvUsers, "真实姓名", "realName", 120);
        TableState.addCol(tvUsers, "状态", "statusText", 80);
        tvUsers.setItems(rows);

        loadUsers();
        loadPersonOptions(null);
    }

    @FXML
    protected void onRefreshClicked() {
        loadUsers();
    }

    @FXML
    protected void onCreateUserClicked() {
        // 人员下拉是异步加载：尚未拿到数据时先补取一次，避免弹窗空列表/NPE
        if (personOptions == null) {
            loadPersonOptions(() -> showCreateUserDialog(null, null, null));
            return;
        }
        showCreateUserDialog(null, null, null);
    }

    /** 提交失败时不丢已填内容：提示原因后以原值重开弹窗，由操作员决定取消或改后重试 */
    private void showCreateUserDialog(String username, String password, Long personId) {
        CreateUserDialog dlg = new CreateUserDialog(username, password, personId);
        dlg.showAndWait().ifPresent(row -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("username", row.username);
                    body.put("password", row.password);
                    body.put("personId", row.personId);
                    String json = ApiClient.post("/api/users", ApiClient.toJson(body));
                    ApiClient.checkSuccess(json);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                loadUsers();
                lblStatus.setText("用户创建成功：" + row.username);
            });
            task.setOnFailed(e -> {
                Alerts.error("创建失败：" + task.getException().getMessage());
                showCreateUserDialog(row.username, row.password, row.personId);
            });
            new Thread(task, "user-create").start();
        });
    }

    @FXML
    protected void onToggleStatusClicked() {
        UserRow selected = tvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.warn("请先选择要启/停用的用户");
            return;
        }
        int newStatus = selected.status == 1 ? 0 : 1;
        String action = newStatus == 1 ? "启用" : "停用";
        if (!Alerts.confirm("确定要" + action + "用户「" + selected.username + "」吗？")) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", newStatus);
                String json = ApiClient.put("/api/users/" + selected.id, ApiClient.toJson(body));
                ApiClient.checkSuccess(json);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            selected.status = newStatus;
            tvUsers.refresh();
            lblStatus.setText(action + "成功：" + selected.username);
        });
        task.setOnFailed(e -> Alerts.error(action + "失败：" + task.getException().getMessage()));
        new Thread(task, "user-toggle").start();
    }

    /** 删除用户：二次确认后删除，服务端同事务连带解除其角色关系 */
    @FXML
    protected void onDeleteUserClicked() {
        UserRow selected = tvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.warn("请先选择要删除的用户");
            return;
        }
        if (!Alerts.confirm("确定删除用户「" + selected.username + "」吗？其已分配的角色将一并解除，且不可恢复。")) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String json = ApiClient.delete("/api/users/" + selected.id);
                ApiClient.checkSuccess(json);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            loadUsers();
            lblStatus.setText("已删除用户：" + selected.username);
        });
        task.setOnFailed(e -> Alerts.error("删除用户失败：" + task.getException().getMessage()));
        new Thread(task, "user-delete").start();
    }

    private void loadUsers() {
        if (loading) return;
        loading = true;
        TableState.loading(tvUsers);
        lblStatus.setText("加载中…");
        Task<List<UserVO>> task = new Task<>() {
            @Override
            protected List<UserVO> call() throws Exception {
                String json = ApiClient.get("/api/users");
                return ApiClient.getResultList(json, UserVO.class);
            }
        };
        task.setOnSucceeded(e -> {
            loading = false;
            List<UserVO> users = task.getValue();
            rows.clear();
            for (UserVO u : users) {
                rows.add(new UserRow(u.getId(), u.getUsername(), u.getPersonId(), u.getPersonCode(),
                        u.getRealName(), u.getStatus()));
            }
            lblStatus.setText("共 " + users.size() + " 个用户");
            TableState.empty(tvUsers, null);
        });
        task.setOnFailed(e -> {
            loading = false;
            String msg = task.getException().getMessage();
            TableState.failed(tvUsers, msg);
            Alerts.error("加载用户列表失败：" + msg);
        });
        new Thread(task, "user-list").start();
    }

    private void loadPersonOptions(Runnable onDone) {
        Task<List<PersonOptionVO>> task = new Task<>() {
            @Override
            protected List<PersonOptionVO> call() throws Exception {
                String json = ApiClient.get("/api/users/person-options");
                return ApiClient.getResultList(json, PersonOptionVO.class);
            }
        };
        task.setOnSucceeded(e -> {
            personOptions = task.getValue();
            if (onDone != null) onDone.run();
        });
        task.setOnFailed(e -> Alerts.error("加载人员选项失败：" + task.getException().getMessage()));
        new Thread(task, "person-options").start();
    }

    /** TableView 行模型 */
    public static class UserRow {
        Long id;
        String username;
        String password;
        Long personId;
        String personCode;
        String realName;
        Integer status;

        UserRow(Long id, String username, Long personId, String personCode, String realName, Integer status) {
            this.id = id; this.username = username; this.personId = personId;
            this.personCode = personCode; this.realName = realName; this.status = status;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getPersonCode() { return personCode; }
        public String getRealName() { return realName; }
        public Integer getStatus() { return status; }
        public String getStatusText() { return status == 1 ? "启用" : "停用"; }
    }

    /** 新增用户 Dialog；username/password/personId 非空时用于失败重试的预填 */
    private class CreateUserDialog extends Dialog<UserRow> {
        private final TextField tfUsername = new TextField();
        private final PasswordField pfPassword = new PasswordField();
        private final ComboBox<PersonOptionVO> cbPerson = new ComboBox<>();

        CreateUserDialog(String username, String password, Long personId) {
            Alerts.theme(this);
            setTitle("新增用户");
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            tfUsername.setPromptText("字母开头，4-30 位字母/数字/下划线");
            pfPassword.setPromptText("6-20 位字母/数字");
            cbPerson.setItems(FXCollections.observableArrayList(personOptions));
            cbPerson.setPromptText("选择关联人员");

            if (username != null) tfUsername.setText(username);
            if (password != null) pfPassword.setText(password);
            if (personId != null) {
                for (PersonOptionVO p : personOptions) {
                    if (p.getId().equals(personId)) {
                        cbPerson.setValue(p);
                        break;
                    }
                }
            }

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.addRow(0, new Label("用户名："), tfUsername);
            grid.addRow(1, new Label("口令："), pfPassword);
            grid.addRow(2, new Label("关联人员："), cbPerson);
            getDialogPane().setContent(grid);

            // 拦截「确定」的默认关窗动作：校验不过只弹警告、弹窗保持打开可继续修改
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                onSubmit();
            });
        }

        private void onSubmit() {
            String username = tfUsername.getText().trim();
            String password = pfPassword.getText();
            PersonOptionVO person = cbPerson.getValue();
            if (!username.matches(RegexPatterns.USERNAME)) {
                Alerts.warn("用户名须字母开头，4-30 位字母/数字/下划线");
                return;
            }
            if (!password.matches(RegexPatterns.PASSWORD)) {
                Alerts.warn("口令须为 6-20 位字母/数字");
                return;
            }
            if (person == null) {
                Alerts.warn("请选择关联人员");
                return;
            }
            UserRow row = new UserRow(null, username, person.getId(), person.getPersonCode(), person.getRealName(), 1);
            row.password = password;
            setResult(row);
            close();
        }
    }
}
