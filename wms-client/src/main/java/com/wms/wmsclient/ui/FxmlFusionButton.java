package com.wms.wmsclient.ui;

import io.vproxy.vfx.ui.button.FusionButton;

/**
 * FXML 可绑定的 FusionButton：vfx 原生类只有 setText 没有 getText，
 * 不构成 JavaBean 属性对，FXML 的 text="..." 绑定会抛
 * PropertyNotFoundException（Property "text" does not exist or is read-only）。
 * 补上 getter 即可通过 FXMLLoader 的属性绑定检查。
 */
public class FxmlFusionButton extends FusionButton {

    public FxmlFusionButton() {
    }

    public String getText() {
        return getTextNode().getText();
    }
}
