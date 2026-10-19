package com.wms.wmsclient.controller;

import com.wms.common.RegexPatterns;
import com.wms.common.dto.LoginRequest;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsclient.App;
import com.wms.wmsclient.SessionContext;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.http.ApiException;
import com.wms.wmsclient.panel.MorePanel;
import com.wms.wmsclient.util.Alerts;
import com.wms.wmsclient.util.TokenStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    @FXML
    private Button btnLogin;

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
            btnLogin.setDisable(false);
            LoginResponse loginResp = task.getValue();
            SessionContext.set(loginResp);
            ApiClient.auth(loginResp.getToken());
            TokenStore.save(loginResp);
            try {
                app.showMain();
            } catch (Exception ex) {
                alertError("进入主界面失败：" + describe(ex));
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnLogin.setDisable(false);
            Throwable t = task.getException();
            String msg;
            if (t instanceof ApiException ae) {
                msg = ae.getMessage(); // 服务端透传的"用户名或口令错误"等
            } else {
                msg = "网络异常：" + t.getMessage();
            }
            alertError(msg);
        }));

        // 防连点：请求期间禁用登录按钮，成功/失败回调里恢复
        btnLogin.setDisable(true);
        new Thread(task, "login-task").start();
    }

    private void alert(String message) {
        Alerts.of(Alert.AlertType.WARNING, message).showAndWait();
    }

    // ====== 「更多」面板入口（FR-6：/api/version 为公开接口，登录前可用）======

    @FXML
    protected void onMoreClicked() {
        MorePanel.show(app);
    }

    private void alertError(String message) {
        Alerts.of(Alert.AlertType.ERROR, message).showAndWait();
    }

    /** 拼接整条异常 cause 链：FXML 加载失败等原因藏在最内层（如「属性不存在」），
     *  只看外壳 message 只剩「fxml:行号」，排障只能靠行号反推 */
    private static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder(t.toString());
        Throwable c = t.getCause();
        while (c != null && c != t && c != c.getCause()) {
            sb.append("\n原因：").append(c.toString());
            c = c.getCause();
        }
        return sb.toString();
    }
}
