package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
import com.wms.wmsclient.App;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * 登录界面：正则预校验（FR-4-5，与服务端共用 RegexPatterns）。
 * 登录业务（HTTP 调用、token、权限获取、进入主界面）由 zheng-qifan 随 FR-4 实现。
 *
 * @author 已按规范移除类级署名（CONTRIBUTING §3.4）
 */
public class LoginController {

    private final App app;

    @FXML
    private TextField tfUsername;
    @FXML
    private PasswordField pfPassword;

    public LoginController(App app) {
        this.app = app;
    }

    @FXML
    protected void onLoginClicked() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();

        // 客户端预校验：规则与服务端同一份常量（双校验）
        if (!username.matches(RegexPatterns.USERNAME)) {
            alert("用户名须字母开头，4-30 位字母/数字/下划线");
            return;
        }
        if (!password.matches(RegexPatterns.PASSWORD)) {
            alert("口令须为 6-20 位字母/数字");
            return;
        }

        // TODO(FR-4)：调用服务端登录接口，成功后 app.showMain() 进入主界面
        new Alert(Alert.AlertType.INFORMATION, "登录功能将在子系统四开发中开放").showAndWait();
    }

    private void alert(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }
}
