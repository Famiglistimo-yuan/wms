package com.wms.wmsclient.util;

import com.wms.wmsclient.theme.ThemeManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * Dialog/Alert 主题工厂（ADR-010 深色主题）：弹窗是独立 scene，拿不到面板级样式表，
 * 须经 dialogPane 单独挂载才能跟随主题。全应用 Alert 一律经 of() 创建；
 * 自建 Dialog 子类在构造器调 theme(this) 走同一机制。
 */
public final class Alerts {

    private Alerts() {
    }

    /** 按当前主题创建 Alert（可带自定义按钮，如 ButtonType.YES/NO） */
    public static Alert of(Alert.AlertType type, String message, ButtonType... buttons) {
        Alert alert = new Alert(type, message, buttons);
        theme(alert);
        return alert;
    }

    /** 给自建 Dialog 挂主题样式表（不挂则深色主题下弹窗整体 Modena 浅色） */
    public static <D extends Dialog<?>> D theme(D dialog) {
        dialog.getDialogPane().getStylesheets().add(ThemeManager.stylesheetUrl());
        return dialog;
    }
}
