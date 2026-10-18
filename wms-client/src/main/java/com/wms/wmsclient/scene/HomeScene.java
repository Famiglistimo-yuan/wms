package com.wms.wmsclient.scene;

import com.wms.wmsclient.SessionContext;
import io.vproxy.vfx.manager.font.FontManager;
import io.vproxy.vfx.ui.layout.VPadding;
import io.vproxy.vfx.ui.scene.VScene;
import io.vproxy.vfx.ui.scene.VSceneRole;
import io.vproxy.vfx.ui.wrapper.ThemeLabel;
import io.vproxy.vfx.util.FXUtils;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

/**
 * 主页欢迎场景（ADR-010，替代原 main.fxml 的空 center 区）：登录后默认展示，
 * 功能导航走左上角「菜单」按钮唤出的抽屉（MenuScene）。
 */
public final class HomeScene extends VScene {

    private final ThemeLabel lblUser = new ThemeLabel();

    public HomeScene() {
        super(VSceneRole.MAIN);
        enableAutoContentWidthHeight();

        ThemeLabel title = new ThemeLabel("仓库管理系统");
        FontManager.get().setFont(title, settings -> settings.setSize(28));
        ThemeLabel hint = new ThemeLabel("点击左上角「菜单」按钮打开功能导航");

        VBox box = new VBox(10, title, new VPadding(14), lblUser, new VPadding(14), hint);
        box.setAlignment(Pos.CENTER);
        FXUtils.observeWidthCenter(getContentPane(), box);
        FXUtils.observeHeightCenter(getContentPane(), box);
        getContentPane().getChildren().add(box);
    }

    /** 刷新当前用户显示（登录成功 / 会话恢复后调用） */
    public void refreshUser() {
        String realName = SessionContext.getRealName();
        String username = SessionContext.getUsername();
        lblUser.setText("当前用户：" + (realName != null ? realName : username));
    }
}
