package com.wms.wmsclient.scene;

import com.wms.wmsclient.App;
import com.wms.wmsclient.SessionContext;
import com.wms.wmsclient.panel.MorePanel;
import com.wms.wmsclient.util.Alerts;
import io.vproxy.vfx.control.scroll.VScrollPane;
import io.vproxy.vfx.manager.font.FontManager;
import io.vproxy.vfx.theme.Theme;
import io.vproxy.vfx.ui.button.FusionButton;
import io.vproxy.vfx.ui.layout.VPadding;
import io.vproxy.vfx.ui.scene.VScene;
import io.vproxy.vfx.ui.scene.VSceneRole;
import io.vproxy.vfx.ui.wrapper.ThemeLabel;
import io.vproxy.vfx.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 抽屉导航菜单（ADR-010，替代原 MenuBar 骨架）：DRAWER_VERTICAL 场景从左侧滑出。
 * 权限显隐逻辑平移自原 MainController（FR-4-4）：无权按钮 hide + disable 双保险，
 * visible 绑定 managed，隐藏时不占布局位。
 */
public final class MenuScene extends VScene {

    private static final double BTN_WIDTH = 230;
    private static final double BTN_HEIGHT = 40;

    private final App app;
    private final ThemeLabel lblUser = new ThemeLabel("未登录");
    private final List<Entry> entries = new ArrayList<>();

    /** 菜单项；anyOfPerms 为权限码集合，任一命中即可见 */
    private record Entry(FusionButton button, Set<String> anyOfPerms) {
    }

    public MenuScene(App app) {
        super(VSceneRole.DRAWER_VERTICAL);
        this.app = app;

        getNode().setPrefWidth(300);
        enableAutoContentWidth();
        getNode().setBackground(new Background(new BackgroundFill(
                Theme.current().subSceneBackgroundColor(), CornerRadii.EMPTY, Insets.EMPTY)));

        VBox box = new VBox();
        box.setPadding(new Insets(16, 0, 16, 24));
        FontManager.get().setFont(lblUser, settings -> settings.setSize(16));
        box.getChildren().addAll(lblUser, new VPadding(16));

        section(box, "档案管理");
        item(box, "人员档案管理", Set.of("menu.person.view", "menu.person.add",
                "menu.person.edit", "menu.person.delete"), "rg2402_11_12_13_person-manage");
        item(box, "物料档案管理", Set.of("menu.material.view", "menu.material.add",
                "menu.material.edit", "menu.material.delete"), "rg2402_11_12_13_material-manage");

        section(box, "进出仓");
        item(box, "进仓录入", Set.of("menu.stock.in"), null);
        item(box, "出仓录入", Set.of("menu.stock.out"), null);
        item(box, "单据查询", Set.of("menu.stock.query"), null);

        section(box, "统计报表");
        item(box, "物料流量统计", Set.of("menu.stats.flow"), "rg2402_11_12_13_stats-flow");
        item(box, "月度进出仓单", Set.of("menu.stats.monthly"), "rg2402_11_12_13_report-monthly-orders");
        item(box, "仓库账本", Set.of("menu.stats.ledger"), "rg2402_11_12_13_report-ledger");

        section(box, "系统管理");
        item(box, "用户管理", Set.of("menu.auth.user"), "rg2402_11_12_13_user-manage");
        item(box, "权限授予", Set.of("menu.auth.grant"), "rg2402_11_12_13_role-grant");
        item(box, "用户授权", Set.of("menu.auth.grant"), "rg2402_11_12_13_user-grant");

        // 底部固定项（不受权限控制）：更多 + 退出登录
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().addAll(spacer, new VPadding(8));
        fixed(box, "更多", () -> MorePanel.show(app));
        fixed(box, "退出登录", this::logout);

        VScrollPane scroll = new VScrollPane();
        scroll.setContent(box);
        getContentPane().getChildren().add(scroll.getNode());
        FXUtils.observeWidthHeight(getContentPane(), scroll.getNode());
    }

    /** 分组标题 */
    private void section(VBox box, String title) {
        ThemeLabel label = new ThemeLabel(title);
        FontManager.get().setFont(label, settings -> settings.setSize(13));
        box.getChildren().addAll(new VPadding(10), label, new VPadding(6));
    }

    /** 权限受控菜单项；fxmlName 为 null 表示功能未开发（占位提示，与原菜单行为一致） */
    private void item(VBox box, String text, Set<String> anyOfPerms, String fxmlName) {
        FusionButton button = new FusionButton(text);
        button.setDisableAnimation(true);
        button.setPrefSize(BTN_WIDTH, BTN_HEIGHT);
        button.managedProperty().bind(button.visibleProperty());
        if (fxmlName != null) {
            button.setOnAction(e -> {
                app.hideMenu();
                try {
                    app.showPanel(fxmlName);
                } catch (Exception ex) {
                    Alerts.of(Alert.AlertType.ERROR, "无法打开 " + fxmlName + "：" + ex.getMessage()).showAndWait();
                }
            });
        } else {
            button.setOnAction(e -> {
                app.hideMenu();
                Alerts.of(Alert.AlertType.INFORMATION, text + " 将在对应子系统开发中开放").showAndWait();
            });
        }
        box.getChildren().add(button);
        entries.add(new Entry(button, anyOfPerms));
    }

    /** 恒可见菜单项 */
    private void fixed(VBox box, String text, Runnable action) {
        FusionButton button = new FusionButton(text);
        button.setDisableAnimation(true);
        button.setPrefSize(BTN_WIDTH, BTN_HEIGHT);
        button.setOnAction(e -> action.run());
        box.getChildren().add(button);
    }

    private void logout() {
        app.hideMenu();
        app.clearSession();
        try {
            app.showLogin();
        } catch (Exception e) {
            Alerts.of(Alert.AlertType.ERROR, "返回登录失败：" + e.getMessage()).showAndWait();
        }
    }

    /** 按权限码显隐菜单项（FR-4-4）。public 供 App.restoreSession 异步校验成功后重跑 */
    public void applyMenuPermissions() {
        for (Entry entry : entries) {
            boolean has = entry.anyOfPerms().stream().anyMatch(SessionContext::hasPermission);
            entry.button().setVisible(has);
            entry.button().setDisable(!has);
        }
    }

    /** 刷新当前用户显示（登录成功 / 会话恢复校验成功后调用） */
    public void refreshUser() {
        String realName = SessionContext.getRealName();
        String username = SessionContext.getUsername();
        lblUser.setText("当前用户：" + (realName != null ? realName : username));
    }
}
