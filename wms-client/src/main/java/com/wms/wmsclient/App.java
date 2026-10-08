package com.wms.wmsclient;

import javafx.application.Application;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsclient.controller.LoginController;
import com.wms.wmsclient.controller.MainController;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.update.UpdateService;
import com.wms.wmsclient.util.TokenStore;
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

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        // 窗口图标（Windows/Linux 任务栏；macOS 的 Dock 图标由 jpackage 的 .icns 提供）
        stage.getIcons().add(new Image(getClass().getResource("/icons/wms.png").toExternalForm()));
        // token 落盘恢复（FR-4/FR-6）：data/session.json 有存档则免重登直接进主界面
        if (!restoreSession()) {
            showLogin();
        }
        stage.setTitle("仓库管理系统");
        stage.show();
        // FR-6：后台检查新版本（失败静默，不阻塞登录；确有新版弹窗征求同意）
        UpdateService.checkAsync(false);
    }

    /** 恢复落盘登录态；返回是否恢复成功（无存档/恢复失败回登录窗） */
    private boolean restoreSession() {
        LoginResponse saved = TokenStore.load();
        if (saved == null || saved.getToken() == null || saved.getToken().isEmpty()) {
            return false;
        }
        SessionContext.set(saved);
        ApiClient.auth(saved.getToken());
        try {
            showMain();
            return true;
        } catch (IOException e) {
            // 主界面加载失败不能留在空白窗口：清态回登录窗
            SessionContext.reset();
            ApiClient.clearAuth();
            TokenStore.clear();
            return false;
        }
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
        mainRoot = (BorderPane) loader.load();
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
