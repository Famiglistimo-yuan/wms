package com.wms.wmsclient.ui;

import io.vproxy.vfx.ui.button.FusionButton;

/**
 * FXML 可绑定的 FusionButton：
 * 1. vfx 原生类只有 setText 没有 getText，不构成 JavaBean 属性对，FXML 的 text="..."
 *    绑定会抛 PropertyNotFoundException（Property "text" does not exist or is read-only），
 *    补上 getter 即可通过 FXMLLoader 的属性绑定检查；
 * 2. 挂 fusion-btn 样式类，文字字体/字号由全局 CSS 接管（vfx 默认 Noto Sans SC 16px 偏大）。
 */
public class FxmlFusionButton extends FusionButton {

    public FxmlFusionButton() {
        getStyleClass().add("fusion-btn");
    }

    public FxmlFusionButton(String text) {
        super(text);
        getStyleClass().add("fusion-btn");
    }

    public String getText() {
        return getTextNode().getText();
    }
}
