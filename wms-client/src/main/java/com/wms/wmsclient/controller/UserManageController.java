package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

/**
 * 用户管理界面（骨架 + 列表 + 操作按钮）。
 * 完整 CRUD + 角色分配 Dialog 后续接入，当前展示列表 + 启停用。
 */
public class UserManageController {

    private final App app;

    @FXML private TableView<UserRow> tvUsers;
    @FXML private Label lblStatus;

    public UserManageController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        // 列绑定
        TableColumn<UserRow, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<UserRow, String> colUsername = new TableColumn<>("用户名");
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setPrefWidth(150);

        TableColumn<UserRow, String> colRealName = new TableColumn<>("真实姓名");
        colRealName.setCellValueFactory(new PropertyValueFactory<>("realName"));
        colRealName.setPrefWidth(150);

        TableColumn<UserRow, Integer> colStatus = new TableColumn<>("状态");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(80);

        tvUsers.getColumns().addAll(colId, colUsername, colRealName, colStatus);

        lblStatus.setText("点击「刷新」加载用户列表");
    }

    @FXML
    protected void onRefreshClicked() {
        lblStatus.setText("用户管理：点击「刷新」从服务端加载（功能待接入）");
        new Alert(Alert.AlertType.INFORMATION, "用户管理列表刷新将在子系统后续接入服务端接口").showAndWait();
    }

    @FXML
    protected void onCreateUserClicked() {
        showPlaceholder("新增用户");
    }

    @FXML
    protected void onToggleStatusClicked() {
        showPlaceholder("启/停用用户");
    }

    @FXML
    protected void onAssignRolesClicked() {
        showPlaceholder("给用户分配角色");
    }

    private void showPlaceholder(String featureName) {
        new Alert(Alert.AlertType.INFORMATION,
                featureName + " 的服务端接口已就绪，前端对接后续接入").showAndWait();
    }

    /** TableView 行模型 */
    public static class UserRow {
        private Long id;
        private String username;
        private String realName;
        private Integer status;

        public UserRow(Long id, String username, String realName, Integer status) {
            this.id = id; this.username = username; this.realName = realName; this.status = status;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getRealName() { return realName; }
        public Integer getStatus() { return status; }
    }
}
