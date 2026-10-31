package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.SessionContext;
import com.wms.common.WmsConstants;
import com.wms.common.dto.PermissionVO;
import com.wms.common.dto.UserPermissionVO;
import com.wms.common.dto.UserVO;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.ApiTasks;
import io.vproxy.vfx.ui.button.FusionButton;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户授权界面：左侧用户列表，右侧权限逐项分配（允许/禁止）。
 * 对单个用户直接授予或收回某菜单执行权，不影响其他用户（FR-4-6）。
 * 有效权限 = 允许 − 禁止（不含角色继承，角色仅作标签）。
 */
public class UserGrantController {

    private static final String OPT_ALLOW = "允许";
    private static final String OPT_DENY = "禁止";

    @FXML private Label lblStatus;
    @FXML private TextField tfSearch;
    @FXML private ListView<UserVO> lvUsers;
    @FXML private ListView<PermAssignItem> lvPermissions;
    @FXML private FusionButton btnSave;
    @FXML private FusionButton btnAllowAll;
    @FXML private FusionButton btnDenyAll;

    private List<PermissionVO> allPermissions;

    /** App 注入构造器（PanelScene 的 ControllerFactory 要求，ADR-007） */
    public UserGrantController(App app) {
        // app 仅为工厂注入约定保留，面板内未使用
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

    /** 关键字搜索左侧用户列表：模糊匹配用户名/人员编号/真实姓名（回车或按钮触发） */
    @FXML
    protected void onSearchClicked() {
        loadUsers();
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
            // 自救防护：当前登录用户的关键权限项不可被「全部禁止」置为禁止
            if (!item.locked) {
                item.choice.setValue(OPT_DENY);
            }
        }
    }

    @FXML
    protected void onSaveClicked() {
        UserVO selected = lvUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.warn("请先选择用户");
            return;
        }
        // 系统管理员权限恒为全部允许，不可修改（服务端同样拒绝）
        if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(selected.getUsername())) {
            Alerts.warn("系统管理员账号的权限不可修改");
            return;
        }

        List<Long> allowIds = lvPermissions.getItems().stream()
                .filter(item -> OPT_ALLOW.equals(item.choice.getValue()))
                .map(item -> item.perm.getId()).toList();
        List<Long> denyIds = lvPermissions.getItems().stream()
                .filter(item -> OPT_DENY.equals(item.choice.getValue()))
                .map(item -> item.perm.getId()).toList();

        if (!Alerts.confirm("确定保存用户「" + selected.getUsername() + "」的直接授权吗？允许 " + allowIds.size()
                + " 项，禁止 " + denyIds.size() + " 项")) return;

        ApiTasks.run("user-grant-save",
                () -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("allow", allowIds);
                    body.put("deny", denyIds);
                    String json = ApiClient.post("/api/users/" + selected.getId() + "/permissions",
                            ApiClient.toJson(body));
                    ApiClient.checkSuccess(json);
                    return null;
                },
                v -> lblStatus.setText("已保存用户「" + displayName(selected) + "」的直接授权（重新登录或刷新权限后生效）"),
                ex -> Alerts.error("保存失败：" + ex.getMessage()));
    }

    /** 加载用户权限期间锁定编辑操作，防止旧权限集被误保存 */
    private void setEditingEnabled(boolean enabled) {
        btnSave.setDisable(!enabled);
        btnAllowAll.setDisable(!enabled);
        btnDenyAll.setDisable(!enabled);
    }

    private void loadAll() {
        loadUsers();

        ApiTasks.run("user-grant-perms",
                () -> {
                    String json = ApiClient.get("/api/roles/permissions");
                    return ApiClient.getResultList(json, PermissionVO.class);
                },
                perms -> {
                    allPermissions = perms;
                    lblStatus.setText(lblStatus.getText() + "，" + allPermissions.size() + " 项权限");
                },
                ex -> Alerts.error("加载权限失败：" + ex.getMessage()));
    }

    /** 按关键字刷新左侧用户列表；过滤后清空右侧面板，避免旧选中用户的权限残留被误保存 */
    private void loadUsers() {
        String keyword = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        String path = keyword.isEmpty()
                ? "/api/users"
                : "/api/users?keyword=" + ApiClient.urlEncode(keyword);
        ApiTasks.run("user-grant-users",
                () -> {
                    String json = ApiClient.get(path);
                    return ApiClient.getResultList(json, UserVO.class);
                },
                users -> {
                    lvUsers.setItems(FXCollections.observableArrayList(users));
                    lvUsers.getSelectionModel().clearSelection();
                    lvPermissions.getItems().clear();
                    setEditingEnabled(true);
                    lblStatus.setText("已加载 " + users.size() + " 个用户"
                            + (keyword.isEmpty() ? "" : "（关键字「" + keyword + "」，点击用户加载其权限）"));
                },
                ex -> Alerts.error("加载用户失败：" + ex.getMessage()));
    }

    @FXML
    protected void onUserSelected() {
        UserVO selected = lvUsers.getSelectionModel().getSelectedItem();
        if (selected == null || allPermissions == null) return;

        final long targetUserId = selected.getId();
        lvUsers.setDisable(true);
        // 加载期间锁定编辑并清空旧内容，防止前一用户的权限集被误保存到当前选中用户
        setEditingEnabled(false);
        lvPermissions.getItems().clear();

        ApiTasks.run("user-grant-load",
                () -> {
                    String json = ApiClient.get("/api/users/" + targetUserId + "/permissions");
                    return ApiClient.getResultData(json, UserPermissionVO.class);
                },
                vo -> {
                    lvUsers.setDisable(false);
                    setEditingEnabled(true);
                    // 过期响应丢弃：若当前选中项已变更，不再覆盖
                    UserVO current = lvUsers.getSelectionModel().getSelectedItem();
                    if (current == null || current.getId() != targetUserId) return;

                    List<Long> allowIds = vo.getAllowPermIds() != null ? vo.getAllowPermIds() : List.of();
                    boolean isAdmin = WmsConstants.SYSTEM_ADMIN_USERNAME.equals(current.getUsername());
                    boolean isSelf = current.getUsername() != null
                            && current.getUsername().equals(SessionContext.getUsername());
                    for (PermissionVO p : allPermissions) {
                        String choice = allowIds.contains(p.getId()) ? OPT_ALLOW : OPT_DENY;
                        // 系统管理员权限恒为全部允许；当前登录用户关键权限不可禁止——均锁定为仅「允许」
                        boolean locked = isAdmin
                                || (isSelf && ("menu.auth.grant".equals(p.getPermCode())
                                || "menu.auth.user".equals(p.getPermCode())));
                        PermAssignItem item = new PermAssignItem(p, choice, locked);
                        if (locked) {
                            item.choice.setItems(FXCollections.observableArrayList(OPT_ALLOW));
                            if (OPT_DENY.equals(choice)) {
                                item.choice.setValue(OPT_ALLOW);
                                lblStatus.setText("注意：当前登录用户的「" + p.getPermName() + "」不可设为禁止，已自动保持允许");
                            }
                        }
                        lvPermissions.getItems().add(item);
                    }
                    if (isAdmin) {
                        btnSave.setDisable(true);
                        lblStatus.setText("系统管理员账号权限恒为全部允许，不可修改");
                    }
                },
                ex -> {
                    lvUsers.setDisable(false);
                    setEditingEnabled(true);
                    Alerts.error("加载用户权限失败：" + ex.getMessage());
                });
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

    /** 权限分配行：perm + 当前选择（允许/禁止）；locked=当前登录用户的关键权限，不可置为禁止 */
    private static class PermAssignItem {
        final PermissionVO perm;
        final boolean locked;
        final ComboBox<String> choice = new ComboBox<>(
                FXCollections.observableArrayList(OPT_ALLOW, OPT_DENY));

        PermAssignItem(PermissionVO perm, String initial, boolean locked) {
            this.perm = perm;
            this.locked = locked;
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
