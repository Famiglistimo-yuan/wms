package com.wms.wmsclient.scene;

import com.wms.wmsclient.App;
import com.wms.wmsclient.theme.ThemeManager;
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
 * Controller 须提供公共 (App) 构造器（沿用 ADR-007 工厂注入约定）；
 * 同一 Controller 服务多个面板时亦可提供 (App, String fxmlName) 双参构造器，由面板名注入模式。
 */
public final class PanelScene extends VScene {

    public PanelScene(App app, String fxmlName) throws IOException {
        super(VSceneRole.MAIN);
        enableAutoContentWidthHeight();

        String path = "/fxml/" + fxmlName + ".fxml";
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource(path), "缺少 FXML 资源：" + path));
        loader.setControllerFactory(c -> {
            // 双参 (App, String fxmlName) 优先：同 Controller 多面板时由面板名注入模式
            // （如 StockIoController 进/出仓共用）；无双参构造器则回落 (App) 约定
            try {
                return c.getConstructor(App.class, String.class).newInstance(app, fxmlName);
            } catch (NoSuchMethodException e) {
                try {
                    return c.getConstructor(App.class).newInstance(app);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("面板 Controller 须提供公共 (App) 或 (App, String) 构造器：" + c.getName(), ex);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("面板 Controller 实例化失败：" + c.getName(), e);
            }
        });
        Region root = loader.load();
        // 底色按主题取浅/深（.panel-host 类在 styles.css / styles-dark.css 各配其值）
        root.getStyleClass().add("panel-host");
        // FXML 里写死的 @../styles.css 按当前主题替换（浅色下为同一文件，行为不变）
        root.getStylesheets().setAll(ThemeManager.stylesheetUrl());
        getContentPane().getChildren().add(root);
        FXUtils.observeWidthHeight(getContentPane(), root);
    }
}
