package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.dto.PermissionVO;
import com.wms.wmsclient.dto.UserPermissionVO;
import com.wms.wmsclient.dto.UserVO;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import com.wms.wmsclient.util.Alerts;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户授权界面：左侧用户列表，右侧权限逐项分配（继承/允许/禁止）。
 * 对单个用户直接授予或收回某菜单执行权，不影响其他用户（FR-4-6）。
 * 有效权限 = (角色继承并集 ∪ 允许) − 禁止。
 */
public class UserGrantController {

    private static final String OPT_INHERIT = "继承";
    private static final String OPT_ALLOW = "允许";
    private static final String OPT_DENY = "禁止";

    private final App app;

    @FXML private Label lblStatus;
    @FXML private ListView<UserVO> lvUsers;
    @FXML private ListView<PermAssignItem> lvPermissions;

    private List<PermissionVO> allPermissions;

    public UserGrantController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        lblStatus.setText("点击「刷新」加载用户和权限");
        lvPermissions.setCellFactory(lv -> new PermAssignCell());
        loadAll();
    }

    @FXML
    protected void onRefreshClicked() {
        loadAll();
    }

    @FXML
    protected void onSaveClicked() {
        UserVO selected = lvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.of(Alert.AlertType.WARNING, "请先选择用户").showAndWait();
            return;
        }

        List<Long> allowIds = lvPermissions.getItems().stream()
                .filter(item -> OPT_ALLOW.equals(item.choice.getValue()))
                .map(item -> item.perm.getId()).toList();
        List<Long> denyIds = lvPermissions.getItems().stream()
                .filter(item -> OPT_DENY.equals(item.choice.getValue()))
                .map(item -> item.perm.getId()).toList();

        Optional<ButtonType> confirm = Alerts.of(Alert.AlertType.CONFIRMATION,
                "确定保存用户「" + selected.getUsername() + "」的直接授权吗？允许 " + allowIds.size()
                        + " 项，禁止 " + denyIds.size() + " 项").showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.OK) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("allow", allowIds);
                body.put("deny", denyIds);
                String json = ApiClient.post("/api/users/" + selected.getId() + "/permissions",
                        ApiClient.toJson(body));
                ApiClient.checkSuccess(json);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                lblStatus.setText("已保存用户「" + selected.getUsername() + "」的直接授权（重新登录或刷新权限后生效）")));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "保存失败：" + msg).showAndWait());
        });
        new Thread(task, "user-grant-save").start();
    }

    private void loadAll() {
        Task<List<UserVO>> userTask = new Task<>() {
            @Override
            protected List<UserVO> call() throws Exception {
                String json = ApiClient.get("/api/users");
                return ApiClient.getResultList(json, UserVO.class);
            }
        };
        Task<List<PermissionVO>> permTask = new Task<>() {
            @Override
            protected List<PermissionVO> call() throws Exception {
                String json = ApiClient.get("/api/roles/permissions");
                return ApiClient.getResultList(json, PermissionVO.class);
            }
        };

        userTask.setOnSucceeded(e -> Platform.runLater(() -> {
            lvUsers.setItems(FXCollections.observableArrayList(userTask.getValue()));
            lblStatus.setText("已加载 " + userTask.getValue().size() + " 个用户");
        }));
        userTask.setOnFailed(e -> {
            Throwable t = userTask.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "加载用户失败：" + msg).showAndWait());
        });

        permTask.setOnSucceeded(e -> Platform.runLater(() -> {
            allPermissions = permTask.getValue();
            lblStatus.setText(lblStatus.getText() + "，" + allPermissions.size() + " 项权限");
        }));
        permTask.setOnFailed(e -> {
            Throwable t = permTask.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "加载权限失败：" + msg).showAndWait());
        });

        new Thread(userTask, "user-grant-users").start();
        new Thread(permTask, "user-grant-perms").start();
    }

    @FXML
    protected void onUserSelected() {
        UserVO selected = lvUsers.getSelectionModel().getSelectedItem();
        if (selected == null || allPermissions == null) return;

        Task<UserPermissionVO> task = new Task<>() {
            @Override
            protected UserPermissionVO call() throws Exception {
                String json = ApiClient.get("/api/users/" + selected.getId() + "/permissions");
                return ApiClient.getResultData(json, UserPermissionVO.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            UserPermissionVO vo = task.getValue();
            List<Long> roleIds = vo.getRolePermIds() != null ? vo.getRolePermIds() : List.of();
            List<Long> allowIds = vo.getAllowPermIds() != null ? vo.getAllowPermIds() : List.of();
            List<Long> denyIds = vo.getDenyPermIds() != null ? vo.getDenyPermIds() : List.of();
            lvPermissions.getItems().clear();
            for (PermissionVO p : allPermissions) {
                String choice = allowIds.contains(p.getId()) ? OPT_ALLOW
                        : denyIds.contains(p.getId()) ? OPT_DENY : OPT_INHERIT;
                lvPermissions.getItems().add(new PermAssignItem(p, roleIds.contains(p.getId()), choice));
            }
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "加载用户权限失败：" + msg).showAndWait());
        });
        new Thread(task, "user-grant-load").start();
    }

    /** 权限分配行：perm + 是否角色继承 + 当前选择（继承/允许/禁止） */
    private static class PermAssignItem {
        final PermissionVO perm;
        final boolean fromRole;
        final ComboBox<String> choice = new ComboBox<>(
                FXCollections.observableArrayList(OPT_INHERIT, OPT_ALLOW, OPT_DENY));

        PermAssignItem(PermissionVO perm, boolean fromRole, String initial) {
            this.perm = perm;
            this.fromRole = fromRole;
            choice.setValue(initial);
        }
    }

    /** 行渲染：权限名（继承来源标注） + 三态下拉 */
    private static class PermAssignCell extends ListCell<PermAssignItem> {
        private final HBox box = new HBox(10);
        private final Label name = new Label();
        private final Region spacer = new Region();

        PermAssignCell() {
            HBox.setHgrow(spacer, Priority.ALWAYS);
            box.getChildren().addAll(name, spacer);
        }

        @Override
        protected void updateItem(PermAssignItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            name.setText(item.perm.getPermName() + " (" + item.perm.getPermCode() + ")"
                    + (item.fromRole ? "　[角色继承]" : ""));
            if (box.getChildren().size() < 3) {
                box.getChildren().add(item.choice);
            } else {
                box.getChildren().set(2, item.choice);
            }
            setGraphic(box);
        }
    }
}
