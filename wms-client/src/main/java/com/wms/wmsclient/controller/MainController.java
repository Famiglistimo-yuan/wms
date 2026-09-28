package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

/**
 * 主界面骨架：菜单结构对应权限码分组（menu.person/material/stock/auth）。
 * 权限码接入（登录后按权限显隐菜单，FR-4-4）由 zheng-qifan 随 FR-4 实现，当前全部可见。
 * 阶段 2 各子系统在此菜单框架上接入自己的程序。
 */
public class MainController {

    private final App app;

    @FXML
    private Label lblUser;

    public MainController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        // TODO(FR-4)：登录接入后显示当前用户名，并按权限码显隐菜单
        lblUser.setText("当前用户：未登录");
    }

    @FXML
    protected void onPlaceholderClicked() {
        // 阶段 2 接入示例：app.showPanel("rg2402_11_12_13_person-manage");
        new Alert(Alert.AlertType.INFORMATION, "该功能将在对应子系统开发中开放").showAndWait();
    }

    @FXML
    protected void onLogoutClicked() {
        try {
            app.showLogin();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "返回登录失败：" + e.getMessage()).showAndWait();
        }
    }
}
