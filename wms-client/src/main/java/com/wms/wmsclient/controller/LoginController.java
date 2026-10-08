package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
import com.wms.common.dto.LoginRequest;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsclient.App;
import com.wms.wmsclient.SessionContext;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import tools.jackson.databind.ObjectMapper;

/**
 * 登录界面：客户端正则预校验 → 后台线程调 /api/auth/login → 成功后写 SessionContext + ApiClient.auth → 进入主界面。
 */
public class LoginController {

    private final App app;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // 客户端正则预校验（FR-4-5）
        if (!username.matches(RegexPatterns.USERNAME)) {
            alert("用户名须字母开头，4-30 位字母/数字/下划线");
            return;
        }
        if (!password.matches(RegexPatterns.PASSWORD)) {
            alert("口令须为 6-20 位字母/数字");
            return;
        }

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        Task<LoginResponse> task = new Task<>() {
            @Override
            protected LoginResponse call() throws Exception {
                String jsonBody = objectMapper.writeValueAsString(req);
                String respJson = ApiClient.post("/api/auth/login", jsonBody);
                return ApiClient.getResultData(respJson, LoginResponse.class);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            LoginResponse loginResp = task.getValue();
            SessionContext.set(loginResp);
            ApiClient.auth(loginResp.getToken());
            try {
                app.showMain();
            } catch (Exception ex) {
                alertError("进入主界面失败：" + ex.getMessage());
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable t = task.getException();
            String msg;
            if (t instanceof ApiException ae) {
                msg = ae.getMessage(); // 服务端透传的"用户名或口令错误"等
            } else {
                msg = "网络异常：" + t.getMessage();
            }
            alertError(msg);
        }));

        // 防止重复点击
        new Thread(task, "login-task").start();
    }

    private void alert(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private void alertError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
