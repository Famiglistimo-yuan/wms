package com.wms.wmsclient;

import javafx.application.Application;
import javafx.application.Platform;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsclient.controller.LoginController;
import com.wms.wmsclient.controller.MainController;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.update.UpdateService;
import com.wms.wmsclient.util.TokenStore;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 客户端入口：登录窗 → 主界面。
 * 登录成功后携带权限信息进入主界面的衔接由 FR-4 实现时补全（showMain 预留扩展）。
 */
public class App extends Application {

    private Stage stage;

    /** 主界面根布局；showPanel 在其 center 区切换子程序面板（ADR-007） */
    private BorderPane mainRoot;

    /** 主界面 Controller 引用，restoreSession 异步校验成功后用来重显隐菜单 */
    private MainController mainController;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        // 窗口图标（Windows/Linux 任务栏；macOS 的 Dock 图标由 jpackage 的 .icns 提供）
        var iconUrl = getClass().getResource("/icons/wms.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        // 401 统一回调：任何业务接口返回 401 自动清态回登录窗（兜底，解决过期 token 卡主界面 +
        // 管理员收回权限后旧会话的陈旧菜单显隐）
        ApiClient.setOnUnauthorized(code -> Platform.runLater(() -> {
            // 登录接口口令错误也返回 401：未登录态（SessionContext 为空）的 401 属于登录失败，
            // 由 LoginController 弹窗提示，不重载登录窗；这里只处理会话过期/失效
            if (SessionContext.get() == null) {
                return;
            }
            SessionContext.reset();
            ApiClient.clearAuth();
            TokenStore.clear();
            try {
                showLogin();
            } catch (IOException e) {
                System.err.println("会话过期后回登录窗失败：" + e.getMessage());
            }
        }));

        // token 落盘恢复（FR-4/FR-6）：data/session.json 有存档则免重登直接进主界面
        if (!restoreSession()) {
            showLogin();
        }
        stage.setTitle("仓库管理系统");
        stage.show();
        // FR-6：后台检查新版本（失败静默，不阻塞登录；确有新版弹窗征求同意）
        UpdateService.checkAsync(false);
    }

    /**
     * 恢复落盘登录态。有存档时先进主界面（体验优先），再后台异步调 /api/auth/me：
     *   - 成功 → 用数据库最新权限集刷新 SessionContext + TokenStore，重显隐菜单
     *   - 401 → ApiClient 回调已自动清态回登录窗
     *   - 网络异常 → 静默（下次业务请求自然 401 兜底）
     * 返回「是否有存档」（有存档=true 先进主界面；无存档=false 直接 showLogin）。
     */
    private boolean restoreSession() {
        LoginResponse saved = TokenStore.load();
        if (saved == null || saved.getToken() == null || saved.getToken().isEmpty()) {
            return false;
        }
        // 先按落盘存档进入主界面（体验优先）
        SessionContext.set(saved);
        ApiClient.auth(saved.getToken());
        try {
            showMain();
        } catch (IOException e) {
            SessionContext.reset();
            ApiClient.clearAuth();
            TokenStore.clear();
            return false;
        }

        // 后台异步校验 token 有效性 + 刷新权限集
        startVerifyTask();

        return true;
    }

    /** 后台异步校验 token 有效性 + 刷新权限集（成功则覆盖会话并重显隐菜单；失败静默，见 ApiClient 401 兜底） */
    private void startVerifyTask() {
        Task<LoginResponse> verifyTask = new Task<>() {
            @Override
            protected LoginResponse call() throws Exception {
                String json = ApiClient.get("/api/auth/me");
                return ApiClient.getResultData(json, LoginResponse.class);
            }
        };
        verifyTask.setOnSucceeded(e -> Platform.runLater(() -> {
            LoginResponse fresh = verifyTask.getValue();
            // 用数据库最新权限集覆盖落盘陈旧值（管理员收回权限后自动同步）
            SessionContext.set(fresh);
            TokenStore.save(fresh);
            // 菜单显隐按最新权限集重跑
            if (mainController != null) {
                mainController.applyMenuPermissions();
            }
        }));
        verifyTask.setOnFailed(e -> {
            // 401：ApiClient.setOnUnauthorized 已清态回登录窗；
            // 其他异常（网络抖动等）：静默，下次业务请求自然 401 兜底
        });
        new Thread(verifyTask, "restore-session-verify").start();
    }

    /** 登录窗 */
    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rg2402_11_12_13_login.fxml"));
        loader.setControllerFactory(c -> new LoginController(this));
        stage.setScene(new Scene(loader.load(), 360, 260));
        stage.centerOnScreen();
    }

    /** 主界面（菜单骨架） */
    public void showMain() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rg2402_11_12_13_main.fxml"));
        loader.setControllerFactory(c -> new MainController(this));
        mainRoot = loader.load();
        mainController = loader.getController();
        stage.setScene(new Scene(mainRoot, 800, 600));
        stage.centerOnScreen();
    }

    /**
     * 在主界面 center 区切换子程序面板（ADR-007：全应用单窗口导航入口）。
     * 面板 Controller 须提供公共 (App) 构造器，供工厂注入本入口。
     *
     * @param fxmlName resources/fxml/ 下文件名（含前缀、不含扩展名），
     *                 如 "rg2402_11_12_13_person-manage"
     */
    public void showPanel(String fxmlName) throws IOException {
        if (mainRoot == null) {
            throw new IllegalStateException("尚未进入主界面，不能切换面板");
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlName + ".fxml"));
        loader.setControllerFactory(c -> {
            try {
                return c.getConstructor(App.class).newInstance(this);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("面板 Controller 须提供公共 (App) 构造器：" + c.getName(), e);
            }
        });
        mainRoot.setCenter(loader.load());
    }
}
