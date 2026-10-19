package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.dto.PermissionVO;
import com.wms.wmsclient.dto.RoleVO;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import com.wms.wmsclient.util.Alerts;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;
import java.util.Optional;

/**
 * 角色授权界面：左侧角色列表，右侧权限勾选，保存授权。
 */
public class RoleGrantController {

    private final App app;

    @FXML private Label lblStatus;
    @FXML private ListView<RoleVO> lvRoles;
    @FXML private ListView<PermItem> lvPermissions;

    private List<PermissionVO> allPermissions;

    public RoleGrantController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        lblStatus.setText("点击「刷新」加载角色和权限");
        loadAll();
    }

    @FXML
    protected void onRefreshClicked() {
        loadAll();
    }

    @FXML
    protected void onSaveClicked() {
        RoleVO selected = lvRoles.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.of(Alert.AlertType.WARNING, "请先选择角色").showAndWait();
            return;
        }

        List<Long> checkedIds = lvPermissions.getItems().stream()
                .filter(PermItem::isChecked)
                .map(item -> item.perm.getId())
                .toList();

        Optional<ButtonType> confirm = Alerts.of(Alert.AlertType.CONFIRMATION,
                "确定要保存角色「" + selected.getRoleName() + "」的权限吗？共 " + checkedIds.size() + " 项").showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.OK) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String json = ApiClient.post("/api/roles/" + selected.getId() + "/permissions",
                        ApiClient.toJson(checkedIds));
                ApiClient.checkSuccess(json);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                lblStatus.setText("已保存角色「" + selected.getRoleName() + "」的 " + checkedIds.size() + " 项权限")));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "保存失败：" + msg).showAndWait());
        });
        new Thread(task, "role-save-perms").start();
    }

    private void loadAll() {
        Task<List<RoleVO>> roleTask = new Task<>() {
            @Override
            protected List<RoleVO> call() throws Exception {
                String json = ApiClient.get("/api/roles");
                return ApiClient.getResultList(json, RoleVO.class);
            }
        };
        Task<List<PermissionVO>> permTask = new Task<>() {
            @Override
            protected List<PermissionVO> call() throws Exception {
                String json = ApiClient.get("/api/roles/permissions");
                return ApiClient.getResultList(json, PermissionVO.class);
            }
        };

        roleTask.setOnSucceeded(e -> Platform.runLater(() -> {
            lvRoles.setItems(FXCollections.observableArrayList(roleTask.getValue()));
            lblStatus.setText("已加载 " + roleTask.getValue().size() + " 个角色");
        }));
        roleTask.setOnFailed(e -> {
            Throwable t = roleTask.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "加载角色失败：" + msg).showAndWait());
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

        new Thread(roleTask, "role-list").start();
        new Thread(permTask, "perm-list").start();
    }

    @FXML
    protected void onRoleSelected() {
        RoleVO selected = lvRoles.getSelectionModel().getSelectedItem();
        if (selected == null || allPermissions == null) return;

        // 加载该角色已分配的权限
        Task<List<Long>> task = new Task<>() {
            @Override
            protected List<Long> call() throws Exception {
                String json = ApiClient.get("/api/roles/" + selected.getId() + "/permissions");
                return ApiClient.getResultList(json, Long.class);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Long> assignedIds = task.getValue();
            ObservableList<PermItem> items = FXCollections.observableArrayList();
            for (PermissionVO p : allPermissions) {
                items.add(new PermItem(p, assignedIds != null && assignedIds.contains(p.getId())));
            }
            lvPermissions.setItems(items);
            lvPermissions.setCellFactory(CheckBoxListCell.forListView(PermItem::checkedProperty));
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = t instanceof ApiException ae ? ae.getMessage() : t.getMessage();
            Platform.runLater(() -> Alerts.of(Alert.AlertType.ERROR, "加载角色权限失败：" + msg).showAndWait());
        });
        new Thread(task, "role-perms").start();
    }

    /** 权限勾选项 */
    private static class PermItem {
        final PermissionVO perm;
        private final BooleanProperty checked;

        PermItem(PermissionVO perm, boolean checked) {
            this.perm = perm;
            this.checked = new SimpleBooleanProperty(checked);
        }

        boolean isChecked() { return checked.get(); }
        BooleanProperty checkedProperty() { return checked; }

        @Override
        public String toString() {
            return perm.getPermName() + " (" + perm.getPermCode() + ")";
        }
    }
}
