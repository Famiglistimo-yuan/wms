package com.wms.wmsclient.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端自动重启（ADR-010 主题切换生效机制）。
 * 打包模式：重启 jpackage 启动器本身（命令即启动器路径）。
 * dev 模式：用当前 java + 完整 classpath 重拉主类（javafx:run fork 的进程
 * java.class.path 为绝对路径全集；工作目录继承，data/ 读写位置与原进程一致）。
 * 兜底：classpath 不含 javafx（如跑在 Maven JVM 内）或拉起失败时提示手动重启。
 */
public final class Restarter {

    private static final String MAIN_CLASS = "com.wms.wmsclient.Launcher";

    private Restarter() {
    }

    /** 主题切换后的重启（FX 线程调用）：优先自动重启，环境不允许则提示手动重启 */
    public static void restartForThemeChange() {
        boolean autoStarted = false;
        if (AppPaths.appRoot() != null) {
            // 打包模式：重启 jpackage 启动器
            autoStarted = startProcess(List.of(currentCommand()));
        } else {
            // dev 模式：java -cp <当前完整 classpath> <主类>
            String cp = System.getProperty("java.class.path");
            if (cp != null && cp.contains("javafx")) {
                List<String> cmd = new ArrayList<>(List.of(currentCommand()));
                cmd.add("-cp");
                cmd.add(cp);
                cmd.add(MAIN_CLASS);
                autoStarted = startProcess(cmd);
            }
        }
        if (autoStarted) {
            Platform.exit();
        } else {
            Alerts.of(Alert.AlertType.INFORMATION,
                    "主题偏好已保存，自动重启不可用，请手动重启客户端后生效").showAndWait();
        }
    }

    private static String currentCommand() {
        return ProcessHandle.current().info().command()
                .orElseThrow(() -> new IllegalStateException("无法确定当前进程命令"));
    }

    private static boolean startProcess(List<String> cmd) {
        try {
            // 不设置工作目录：继承当前 cwd（data/ 相对路径与原进程一致）
            new ProcessBuilder(cmd).start();
            return true;
        } catch (Exception e) {
            System.err.println("自动重启拉起失败：" + e.getMessage());
            return false;
        }
    }
}
