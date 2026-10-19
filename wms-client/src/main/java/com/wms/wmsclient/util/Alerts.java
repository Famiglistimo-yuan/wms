package com.wms.wmsclient.util;

import com.wms.wmsclient.theme.ThemeManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Alert 工厂（ADR-010 深色主题）：弹窗是独立 scene，拿不到面板级样式表，
 * 须经 dialogPane 单独挂载才能跟随主题。全应用 Alert 一律经此创建。
 */
public final class Alerts {

    private Alerts() {
    }

    /** 按当前主题创建 Alert（可带自定义按钮，如 ButtonType.YES/NO） */
    public static Alert of(Alert.AlertType type, String message, ButtonType... buttons) {
        Alert alert = new Alert(type, message, buttons);
        alert.getDialogPane().getStylesheets().add(ThemeManager.stylesheetUrl());
        return alert;
    }
}
