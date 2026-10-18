package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
import com.wms.wmsclient.App;
import com.wms.wmsclient.dto.PersonOptionVO;
import com.wms.wmsclient.dto.RoleVO;
import com.wms.wmsclient.dto.UserVO;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;
import java.util.Optional;

/**
 * 用户管理界面：列表 + 新增/启停用/分配角色。
 */
public class UserManageController {

    private final App app;

    @FXML private TableView<UserRow> tvUsers;
    @FXML private Label lblStatus;

    private final ObservableList<UserRow> rows = FXCollections.observableArrayList();
    private List<PersonOptionVO> personOptions;
    private List<RoleVO> roleList;

    public UserManageController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        TableColumn<UserRow, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<UserRow, String> colUsername = new TableColumn<>("用户名");
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setPrefWidth(150);

        TableColumn<UserRow, String> colPersonCode = new TableColumn<>("人员编号");
        colPersonCode.setCellValueFactory(new PropertyValueFactory<>("personCode"));
        colPersonCode.setPrefWidth(100);

        TableColumn<UserRow, String> colRealName = new TableColumn<>("真实姓名");
        colRealName.setCellValueFactory(new PropertyValueFactory<>("realName"));
        colRealName.setPrefWidth(120);

        TableColumn<UserRow, String> colStatus = new TableColumn<>("状态");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusText"));
        colStatus.setPrefWidth(80);

        tvUsers.getColumns().addAll(colId, colUsername, colPersonCode, colRealName, colStatus);
        tvUsers.setItems(rows);

        loadUsers();
        loadPersonOptions();
    }

    @FXML
    protected void onRefreshClicked() {
        loadUsers();
    }

    @FXML
    protected void onCreateUserClicked() {
        CreateUserDialog dlg = new CreateUserDialog();
        dlg.showAndWait().ifPresent(row -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String json = ApiClient.post("/api/users",
                            "{\"username\":\"" + row.username + "\",\"password\":\"" + row.password
                                    + "\",\"personId\":" + row.personId + "}");
                    ApiClient.checkSuccess(json);
                    return null;
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadUsers();
                lblStatus.setText("用户创建成功：" + row.username);
            }));
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                Platform.runLater(() -> alertError("创建失败：" + msg));
            });
            new Thread(task, "user-create").start();
        });
    }

    @FXML
    protected void onToggleStatusClicked() {
        UserRow selected = tvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertWarn("请先选择要启/停用的用户");
            return;
        }
        int newStatus = selected.status == 1 ? 0 : 1;
        String action = newStatus == 1 ? "启用" : "停用";
        Optional<ButtonType> confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确定要" + action + "用户「" + selected.username + "」吗？").showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.OK) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String json = ApiClient.put("/api/users/" + selected.id,
                        "{\"status\":" + newStatus + "}");
                ApiClient.checkSuccess(json);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            selected.status = newStatus;
            tvUsers.refresh();
            lblStatus.setText(action + "成功：" + selected.username);
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> alertError(action + "失败：" + msg));
        });
        new Thread(task, "user-toggle").start();
    }

    @FXML
    protected void onAssignRolesClicked() {
        UserRow selected = tvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alertWarn("请先选择要分配角色的用户");
            return;
        }
        if (roleList == null || roleList.isEmpty()) {
            loadRoles(() -> onAssignRolesClicked());
            return;
        }

        // 加载该用户已分配的角色
        Task<List<Long>> loadTask = new Task<>() {
            @Override
            protected List<Long> call() throws Exception {
                String json = ApiClient.get("/api/users/" + selected.id + "/roles");
                return ApiClient.getResultList(json, Long.class);
            }
        };
        loadTask.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Long> assignedIds = loadTask.getValue();
            AssignRolesDialog dlg = new AssignRolesDialog(selected, roleList, assignedIds);
            dlg.showAndWait().ifPresent(roleIds -> {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String json = ApiClient.post("/api/users/" + selected.id + "/roles",
                                ApiClient.toJson(roleIds));
                        ApiClient.checkSuccess(json);
                        return null;
                    }
                };
                task.setOnSucceeded(e2 -> Platform.runLater(() ->
                        lblStatus.setText("角色分配已保存：" + selected.username)));
                task.setOnFailed(e2 -> {
                    Throwable t = task.getException();
                    String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
                    Platform.runLater(() -> alertError("分配角色失败：" + msg));
                });
                new Thread(task, "user-assign-roles").start();
            });
        }));
        loadTask.setOnFailed(e -> {
            Throwable t = loadTask.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> alertError("加载角色失败：" + msg));
        });
        new Thread(loadTask, "user-load-roles").start();
    }

    private void loadUsers() {
        Task<List<UserVO>> task = new Task<>() {
            @Override
            protected List<UserVO> call() throws Exception {
                String json = ApiClient.get("/api/users");
                return ApiClient.getResultList(json, UserVO.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<UserVO> users = task.getValue();
            rows.clear();
            for (UserVO u : users) {
                rows.add(new UserRow(u.getId(), u.getUsername(), u.getPersonId(), u.getPersonCode(),
                        u.getRealName(), u.getStatus()));
            }
            lblStatus.setText("共 " + users.size() + " 个用户");
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> alertError("加载用户列表失败：" + msg));
        });
        new Thread(task, "user-list").start();
    }

    private void loadPersonOptions() {
        Task<List<PersonOptionVO>> task = new Task<>() {
            @Override
            protected List<PersonOptionVO> call() throws Exception {
                String json = ApiClient.get("/api/users/person-options");
                return ApiClient.getResultList(json, PersonOptionVO.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> personOptions = task.getValue()));
        task.setOnFailed(e -> Platform.runLater(() ->
                lblStatus.setText("加载人员选项失败，新增用户可能无法选关联人员")));
        new Thread(task, "person-options").start();
    }

    private void loadRoles(Runnable onDone) {
        Task<List<RoleVO>> task = new Task<>() {
            @Override
            protected List<RoleVO> call() throws Exception {
                String json = ApiClient.get("/api/roles");
                return ApiClient.getResultList(json, RoleVO.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            roleList = task.getValue();
            if (onDone != null) onDone.run();
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> alertError("加载角色列表失败：" + msg));
        });
        new Thread(task, "role-list").start();
    }

    private void alertWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private void alertError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
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

    /** 新增用户 Dialog */
    private class CreateUserDialog extends Dialog<UserRow> {
        private final TextField tfUsername = new TextField();
        private final PasswordField pfPassword = new PasswordField();
        private final ComboBox<PersonOptionVO> cbPerson = new ComboBox<>();

        CreateUserDialog() {
            setTitle("新增用户");
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            tfUsername.setPromptText("字母开头，4-30 位字母/数字/下划线");
            pfPassword.setPromptText("6-20 位字母/数字");
            cbPerson.setItems(FXCollections.observableArrayList(personOptions));
            cbPerson.setPromptText("选择关联人员");

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.addRow(0, new Label("用户名："), tfUsername);
            grid.addRow(1, new Label("口令："), pfPassword);
            grid.addRow(2, new Label("关联人员："), cbPerson);
            getDialogPane().setContent(grid);

            setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                String username = tfUsername.getText().trim();
                String password = pfPassword.getText();
                PersonOptionVO person = cbPerson.getValue();
                if (!username.matches(RegexPatterns.USERNAME)) {
                    alertWarn("用户名须字母开头，4-30 位字母/数字/下划线");
                    return null;
                }
                if (!password.matches(RegexPatterns.PASSWORD)) {
                    alertWarn("口令须为 6-20 位字母/数字");
                    return null;
                }
                if (person == null) {
                    alertWarn("请选择关联人员");
                    return null;
                }
                UserRow row = new UserRow(null, username, person.getId(), person.getPersonCode(), person.getRealName(), 1);
                row.password = password;
                return row;
            });
        }
    }

    /** 分配角色 Dialog */
    private static class AssignRolesDialog extends Dialog<List<Long>> {
        AssignRolesDialog(UserRow user, List<RoleVO> roles, List<Long> assignedIds) {
            setTitle("分配角色 - " + user.username);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            ListView<RoleItem> lv = new ListView<>();
            for (RoleVO r : roles) {
                lv.getItems().add(new RoleItem(r, assignedIds != null && assignedIds.contains(r.getId())));
            }
            lv.setCellFactory(CheckBoxListCell.forListView(RoleItem::checkedProperty));
            getDialogPane().setContent(lv);

            setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                return lv.getItems().stream()
                        .filter(RoleItem::isChecked)
                        .map(item -> item.role.getId())
                        .toList();
            });
        }
    }

    /** 角色勾选项 */
    private static class RoleItem {
        final RoleVO role;
        private final BooleanProperty checked;

        RoleItem(RoleVO role, boolean checked) {
            this.role = role;
            this.checked = new SimpleBooleanProperty(checked);
        }

        boolean isChecked() { return checked.get(); }
        BooleanProperty checkedProperty() { return checked; }

        @Override
        public String toString() {
            return role.getRoleName() + " (" + role.getRoleCode() + ")";
        }
    }
}
