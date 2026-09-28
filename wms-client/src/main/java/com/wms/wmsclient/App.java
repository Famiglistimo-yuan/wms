package com.wms.wmsclient;

import javafx.application.Application;
import com.wms.wmsclient.controller.LoginController;
import com.wms.wmsclient.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
