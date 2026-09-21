package com.wms.wmsclient;

import javafx.application.Application;
import com.wms.wmsclient.controller.LoginController;
import com.wms.wmsclient.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 客户端入口：登录窗 → 主界面。
 * 登录成功后携带权限信息进入主界面的衔接由 FR-4 实现时补全（showMain 预留扩展）。
 */
public class App extends Application {

    private final Stage stage = new Stage();

    @Override
    public void start(Stage primaryStage) throws IOException {
        showLogin();
        stage.setTitle("仓库管理系统");
        stage.show();
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
        stage.setScene(new Scene(loader.load(), 800, 600));
        stage.centerOnScreen();
    }
}
