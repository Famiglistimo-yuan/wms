package com.wms.wmsclient.panel;

import com.wms.wmsclient.App;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.theme.ThemeManager;
import com.wms.wmsclient.update.UpdateService;
import com.wms.wmsclient.util.Restarter;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 「更多」面板（登录窗「更多」按钮与主界面「更多」菜单共用，布局参考 WorkBuddy 更多面板）：
 * 第一行 当前版本 + 检查更新；第二行 关于 WMS + 前往 GitHub。
 * 实现为独立无边框小窗口而非 MenuButton 弹出菜单：macOS 上 popup 类窗口（MenuButton/
 * ContextMenu 共用机制）弹出不稳定（实测登录窗 MenuButton 点不开），普通 Stage 行为可靠。
 * 非模态：点击面板外（焦点离开）或按 ESC 自动关闭，贴近系统 popover 交互。
 */
public final class MorePanel {

    /** 项目仓库（public），与 README 徽章口径一致 */
    private static final String GITHUB_URL = "https://github.com/Famiglistimo-yuan/wms";

    private MorePanel() {
    }

    /** 弹出「更多」面板。传入 App：HostServices 打开浏览器、initOwner/定位都以主窗口为基准 */
    public static void show(App app) {
        Stage panel = new Stage(StageStyle.TRANSPARENT);
        panel.initOwner(app.getStage());
        panel.initModality(Modality.NONE);

        // ====== 第一行：当前版本 + 检查更新 ======
        String version = UpdateService.localVersion();
        Label versionValue = new Label(version != null ? "v" + version : "开发模式（未打包）");
        versionValue.getStyleClass().add("subtle");
        Button btnCheck = new Button("检查更新");
        // 先收起面板再检查：升级确认弹窗与面板同屏会互相遮挡
        btnCheck.setOnAction(e -> {
            panel.hide();
            UpdateService.checkAsync(true);
        });

        // ====== 第二行：关于 + 前往 GitHub ======
        Button btnGitHub = new Button("前往 GitHub ↗");
        btnGitHub.setOnAction(e -> app.getHostServices().showDocument(GITHUB_URL));

        // ====== 第三行：服务器地址（只读）+ 测试连接 ======
        Label serverUrl = new Label(ApiClient.BASE_URL);
        serverUrl.getStyleClass().add("subtle");
        Button btnPing = new Button("测试连接");
        Label pingStatus = new Label("未测试");
        pingStatus.getStyleClass().add("subtle");
        // 后台线程发请求（CONTRIBUTING §3.4 第 1 条），结果切回 FX 线程更新 UI
        btnPing.setOnAction(e -> {
            btnPing.setDisable(true);
            pingStatus.setText("测试中…");
            pingStatus.getStyleClass().setAll("subtle");
            new Thread(() -> {
                boolean ok = ApiClient.pingServer();
                Platform.runLater(() -> {
                    btnPing.setDisable(false);
                    if (ok) {
                        pingStatus.setText("✓ 已连接");
                        pingStatus.getStyleClass().setAll("ping-ok");
                    } else {
                        pingStatus.setText("✗ 无法连接");
                        pingStatus.getStyleClass().setAll("ping-fail");
                    }
                });
            }, "ping-server").start();
        });

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox row1 = new HBox(12, new Label("当前版本"), versionValue, spacer1, btnCheck);
        row1.setAlignment(Pos.CENTER_LEFT);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox row2 = new HBox(12, new Label("关于 WMS"), spacer2, btnGitHub);
        row2.setAlignment(Pos.CENTER_LEFT);

        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        HBox row3 = new HBox(12, new Label("服务器"), serverUrl, spacer3, btnPing, pingStatus);
        row3.setAlignment(Pos.CENTER_LEFT);

        // ====== 第四行：主题切换（浅色 ⇄ 深色，重启后生效，ADR-010）======
        Button btnTheme = new Button(ThemeManager.isDark() ? "切换浅色主题" : "切换深色主题");
        btnTheme.setOnAction(e -> {
            panel.hide();
            ThemeManager.Mode target = ThemeManager.isDark() ? ThemeManager.Mode.LIGHT : ThemeManager.Mode.DARK;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "切换主题将重启客户端，确定切换吗？", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
                if (ThemeManager.persist(target)) {
                    Restarter.restartForThemeChange();   // 打包模式自动重启；dev 模式提示手动重启
                } else {
                    new Alert(Alert.AlertType.ERROR, "主题偏好写入失败，未切换").showAndWait();
                }
            });
        });

        Region spacer4 = new Region();
        HBox.setHgrow(spacer4, Priority.ALWAYS);
        HBox row4 = new HBox(12, new Label("主题"), spacer4, btnTheme);
        row4.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, row1, row2, row3, row4);
        root.getStyleClass().add("more-panel");
        Scene scene = new Scene(root);
        // TRANSPARENT 舞台必须配透明场景填充，圆角面板外才不会露白底
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(ThemeManager.stylesheetUrl());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                panel.hide();
            }
        });
        panel.setScene(scene);

        // popover 式关闭：焦点离开（点击面板外任意处）即收起
        panel.focusedProperty().addListener((o, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                panel.hide();
            }
        });

        // 面板居中于主窗口（show 后才有实际尺寸）
        panel.setOnShown(e -> {
            Stage owner = app.getStage();
            panel.setX(owner.getX() + (owner.getWidth() - panel.getWidth()) / 2);
            panel.setY(owner.getY() + (owner.getHeight() - panel.getHeight()) / 3);
        });
        panel.show();
    }
}
