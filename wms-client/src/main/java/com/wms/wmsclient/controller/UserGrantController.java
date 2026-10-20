package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.SessionContext;
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
 * 用户授权界面：左侧用户列表，右侧权限逐项分配（允许/禁止）。
 * 对单个用户直接授予或收回某菜单执行权，不影响其他用户（FR-4-6）。
 * 有效权限 = 允许 − 禁止（不含角色继承，角色仅作标签）。
 */
public class UserGrantController {

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
        lvUsers.setCellFactory(lv -> new UserCell());
        lvPermissions.setCellFactory(lv -> new PermAssignCell());
        loadAll();
    }

    @FXML
    protected void onRefreshClicked() {
        loadAll();
    }

    @FXML
    protected void onAllowAllClicked() {
        for (PermAssignItem item : lvPermissions.getItems()) {
            item.choice.setValue(OPT_ALLOW);
        }
    }

    @FXML
    protected void onDenyAllClicked() {
        for (PermAssignItem item : lvPermissions.getItems()) {
            item.choice.setValue(OPT_DENY);
        }
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
                lblStatus.setText("已保存用户「" + displayName(selected) + "」的直接授权（重新登录或刷新权限后生效）")));
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

        final long targetUserId = selected.getId();
        lvUsers.setDisable(true);

        Task<UserPermissionVO> task = new Task<>() {
            @Override
            protected UserPermissionVO call() throws Exception {
                String json = ApiClient.get("/api/users/" + targetUserId + "/permissions");
                return ApiClient.getResultData(json, UserPermissionVO.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lvUsers.setDisable(false);
            // 过期响应丢弃：若当前选中项已变更，不再覆盖
            UserVO current = lvUsers.getSelectionModel().getSelectedItem();
            if (current == null || current.getId() != targetUserId) return;

            UserPermissionVO vo = task.getValue();
            List<Long> allowIds = vo.getAllowPermIds() != null ? vo.getAllowPermIds() : List.of();
            boolean isSelf = current.getUsername() != null
                    && current.getUsername().equals(SessionContext.getUsername());
            lvPermissions.getItems().clear();
            for (PermissionVO p : allPermissions) {
                String choice = allowIds.contains(p.getId()) ? OPT_ALLOW : OPT_DENY;
                PermAssignItem item = new PermAssignItem(p, choice);
                // 自救防护：对当前登录用户，关键权限不可选「禁止」——避免 UI 锁死
                if (isSelf && ("menu.auth.grant".equals(p.getPermCode())
                        || "menu.auth.user".equals(p.getPermCode()))) {
                    item.choice.setItems(FXCollections.observableArrayList(OPT_ALLOW));
                    if (OPT_DENY.equals(choice)) {
                        item.choice.setValue(OPT_ALLOW);
                        lblStatus.setText("注意：当前登录用户的「" + p.getPermName() + "」不可设为禁止，已自动保持允许");
                    }
                }
                lvPermissions.getItems().add(item);
            }
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> {
                lvUsers.setDisable(false);
                Alerts.of(Alert.AlertType.ERROR, "加载用户权限失败：" + msg).showAndWait();
            });
        });
        new Thread(task, "user-grant-load").start();
    }

    /** 显示名：用户名（真实姓名） */
    private static String displayName(UserVO u) {
        return u.getRealName() == null || u.getRealName().isBlank()
                ? u.getUsername() : u.getUsername() + "（" + u.getRealName() + "）";
    }

    /** 行渲染：用户名（真实姓名）+ 停用标记 */
    private static class UserCell extends ListCell<UserVO> {
        @Override
        protected void updateItem(UserVO item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            setText(displayName(item)
                    + (Integer.valueOf(0).equals(item.getStatus()) ? "　[已停用]" : ""));
        }
    }

    /** 权限分配行：perm + 当前选择（允许/禁止） */
    private static class PermAssignItem {
        final PermissionVO perm;
        final ComboBox<String> choice = new ComboBox<>(
                FXCollections.observableArrayList(OPT_ALLOW, OPT_DENY));

        PermAssignItem(PermissionVO perm, String initial) {
            this.perm = perm;
            choice.setValue(initial);
        }
    }

    /** 行渲染：权限名 + 二态下拉 */
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
            name.setText(item.perm.getPermName() + " (" + item.perm.getPermCode() + ")");
            if (box.getChildren().size() < 3) {
                box.getChildren().add(item.choice);
            } else {
                box.getChildren().set(2, item.choice);
            }
            setGraphic(box);
        }
    }
}
