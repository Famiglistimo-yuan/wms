package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

/**
 * 权限授予界面骨架。服务端接口已就绪（/api/roles, /api/roles/permissions），
 * 前端后续接入角色列表 + 权限码勾选。
 */
public class RoleGrantController {

    private final App app;

    @FXML private Label lblStatus;

    public RoleGrantController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        lblStatus.setText("权限授予界面骨架 — 服务端角色/权限接口已就绪");
    }

    @FXML
    protected void onRefresh() {
        new Alert(Alert.AlertType.INFORMATION, "权限授予界面将在后续接入服务端接口").showAndWait();
    }
}
