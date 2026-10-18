package com.wms.wmsclient.theme;

import io.vproxy.vfx.manager.image.ImageManager;
import io.vproxy.vfx.theme.impl.DarkTheme;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * 浅色主题（ADR-010）：vfx 1.4.x 仅内置 DarkTheme，而本项目 FXML 面板为浅色配色，
 * 故继承 DarkTheme 覆盖颜色与窗口按钮图标，统一全应用浅色视觉。
 * 必须在任何 vfx 组件创建之前经 Theme.setTheme 注入（App.start 第一行），
 * 否则 Theme.current() 已锁定 DarkTheme 且 setTheme 抛 IllegalStateException。
 */
public final class LightTheme extends DarkTheme {

    /** 窗口控制按钮 normal 态深灰变体（hover 沿用 DarkTheme 预注册的红/绿/黄语义色） */
    private static final String DARK_ICON = "wms-dark";

    static {
        ImageManager.get().loadBlackAndChangeColor("io/vproxy/vfx/res/image/close.png", Map.of(DARK_ICON, 0xff3a3a3c));
        ImageManager.get().loadBlackAndChangeColor("io/vproxy/vfx/res/image/maximize.png", Map.of(DARK_ICON, 0xff3a3a3c));
        ImageManager.get().loadBlackAndChangeColor("io/vproxy/vfx/res/image/reset-window-size.png", Map.of(DARK_ICON, 0xff3a3a3c));
        ImageManager.get().loadBlackAndChangeColor("io/vproxy/vfx/res/image/iconify.png", Map.of(DARK_ICON, 0xff3a3a3c));
    }

    @Override
    public Color normalTextColor() {
        return new Color(0x33 / 255d, 0x33 / 255d, 0x36 / 255d, 1);
    }

    @Override
    public Color sceneBackgroundColor() {
        return new Color(0xf2 / 255d, 0xf2 / 255d, 0xf4 / 255d, 1);
    }

    @Override
    public Color subSceneBackgroundColor() {
        return new Color(0xe4 / 255d, 0xe4 / 255d, 0xe7 / 255d, 1);
    }

    @Override
    public Color fusionButtonHoverBackgroundColor() {
        return new Color(0xe0 / 255d, 0xe0 / 255d, 0xe4 / 255d, 1);
    }

    @Override
    public Color fusionButtonDownBackgroundColor() {
        return new Color(0xd5 / 255d, 0xd5 / 255d, 0xda / 255d, 1);
    }

    @Override
    public Color transparentFusionButtonDownBackgroundColor() {
        return new Color(0, 0, 0, 0.1);
    }

    @Override
    public Color fusionPaneHoverBackgroundColor() {
        return new Color(0xea / 255d, 0xea / 255d, 0xee / 255d, 1);
    }

    @Override
    public Color transparentFusionPaneHoverBackgroundColor() {
        return new Color(0, 0, 0, 0.08);
    }

    @Override
    public Color fusionButtonDisabledTextColor() {
        return new Color(0xa8 / 255d, 0xa8 / 255d, 0xad / 255d, 1);
    }

    @Override
    public Color scrollBarColor() {
        return new Color(0x9d / 255d, 0x9d / 255d, 0xa2 / 255d, 1);
    }

    @Override
    public Color coverBackgroundColor() {
        return new Color(0, 0, 0, 0.08);
    }

    @Override
    public Color tableCellSelectedBackgroundColor() {
        return new Color(0xc9 / 255d, 0xd4 / 255d, 0xe8 / 255d, 1);
    }

    @Override
    public Color tableCellBackgroundColor2() {
        return new Color(0xf7 / 255d, 0xf7 / 255d, 0xf9 / 255d, 1);
    }

    @Override
    public Color tableHeaderTopBackgroundColor() {
        return new Color(0xea / 255d, 0xea / 255d, 0xee / 255d, 1);
    }

    @Override
    public Color tableHeaderBottomBackgroundColor() {
        return new Color(0xe0 / 255d, 0xe0 / 255d, 0xe5 / 255d, 1);
    }

    @Override
    public Color progressBarBackgroundColor() {
        return new Color(0xd5 / 255d, 0xd5 / 255d, 0xda / 255d, 1);
    }

    @Override
    public Color toggleSwitchUnselectedTrayColor() {
        return new Color(0xd5 / 255d, 0xd5 / 255d, 0xda / 255d, 1);
    }

    @Override
    public Color toggleSwitchSelectedTrayColor() {
        return new Color(0xa8 / 255d, 0xb0 / 255d, 0xbd / 255d, 1);
    }

    // ====== 窗口控制按钮 normal 态用深灰变体（hover 保留红/绿/黄语义色，macOS 风格）======

    @Override
    public Image windowCloseButtonNormalImage() {
        return ImageManager.get().load("io/vproxy/vfx/res/image/close.png:" + DARK_ICON);
    }

    @Override
    public Image windowMaximizeButtonNormalImage() {
        return ImageManager.get().load("io/vproxy/vfx/res/image/maximize.png:" + DARK_ICON);
    }

    @Override
    public Image windowResetWindowSizeButtonNormalImage() {
        return ImageManager.get().load("io/vproxy/vfx/res/image/reset-window-size.png:" + DARK_ICON);
    }

    @Override
    public Image windowIconifyButtonNormalImage() {
        return ImageManager.get().load("io/vproxy/vfx/res/image/iconify.png:" + DARK_ICON);
    }
}
