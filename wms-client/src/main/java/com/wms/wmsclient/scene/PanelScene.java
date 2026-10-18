package com.wms.wmsclient.scene;

import com.wms.wmsclient.App;
import io.vproxy.vfx.ui.scene.VScene;
import io.vproxy.vfx.ui.scene.VSceneRole;
import io.vproxy.vfx.util.FXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.Objects;

/**
 * 子程序面板的 VScene 包装（ADR-010）：FXML 加载出的 Region 放入 VScene 的 contentPane，
 * 面板内部保持 FXML + Controller 不变（渐进迁移，后续逐面板替换为 vfx 原生组件）。
 * Controller 须提供公共 (App) 构造器（沿用 ADR-007 工厂注入约定）。
 */
public final class PanelScene extends VScene {

    public PanelScene(App app, String fxmlName) throws IOException {
        super(VSceneRole.MAIN);
        enableAutoContentWidthHeight();

        String path = "/fxml/" + fxmlName + ".fxml";
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource(path), "缺少 FXML 资源：" + path));
        loader.setControllerFactory(c -> {
            try {
                return c.getConstructor(App.class).newInstance(app);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("面板 Controller 须提供公共 (App) 构造器：" + c.getName(), e);
            }
        });
        Region root = loader.load();
        // vfx 场景为深色主题，FXML 面板为浅色配色：panel-host 统一浅底保证对比度（样式值集中于 styles.css）
        root.getStyleClass().add("panel-host");
        getContentPane().getChildren().add(root);
        FXUtils.observeWidthHeight(getContentPane(), root);
    }
}
