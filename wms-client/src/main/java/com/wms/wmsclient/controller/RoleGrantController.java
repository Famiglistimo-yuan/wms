package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/**
 * 权限授予界面骨架。服务端接口已就绪（/api/roles, /api/roles/permissions），
 * 前端后续接入角色列表 + 权限码勾选。
 */
public class RoleGrantController {

    private final App app;

    @FXML private Label lblStatus;
    @FXML private ListView<Object> lvRoles;
    @FXML private ListView<Object> lvPermissions;

    public RoleGrantController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        lblStatus.setText("权限授予界面骨架 — 服务端角色/权限接口已就绪");
    }

    @FXML
    protected void onRefreshClicked() {
        // TODO(FR-4-6/7): 调 /api/roles 加载角色，调 /api/roles/permissions 加载权限码并勾选
    }
}
