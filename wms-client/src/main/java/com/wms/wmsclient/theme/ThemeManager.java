package com.wms.wmsclient.theme;

import com.wms.wmsclient.util.AppPaths;
import io.vproxy.vfx.theme.Theme;
import io.vproxy.vfx.theme.impl.DarkTheme;
import javafx.application.Application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 主题管理（ADR-010）：浅色（LightTheme + styles.css）/ 深色（vfx DarkTheme + styles-dark.css）。
 * vfx 的 Theme.setTheme 仅允许进程启动时注入一次（组件构造时固色，无热切换机制），
 * 故切换 = 偏好落盘 + 「更多」面板确认弹窗 + 客户端重启后生效。
 */
public final class ThemeManager {

    public enum Mode { LIGHT, DARK }

    private static final String PREF_FILE = "preferences.properties";
    private static final String KEY_THEME = "theme";
    private static Mode current = Mode.LIGHT;

    private ThemeManager() {
    }

    /** 启动注入：读偏好 → 设置 vfx Theme 与 JavaFX 全局 UA stylesheet（Alert 等弹窗跟随）。
     *  必须在任何 vfx 组件创建之前调用（App.start 第一行） */
    public static void init() {
        current = loadPreference();
        if (current == Mode.DARK) {
            Theme.setTheme(new DarkTheme());
            Application.setUserAgentStylesheet(stylesheetUrl());
        } else {
            Theme.setTheme(new LightTheme());
            Application.setUserAgentStylesheet(null);
        }
    }

    public static boolean isDark() {
        return current == Mode.DARK;
    }

    /** 当前面板/浮层应使用的样式表（styles.css 或 styles-dark.css，类名同套、值各配） */
    public static String stylesheetUrl() {
        String name = isDark() ? "/styles-dark.css" : "/styles.css";
        return ThemeManager.class.getResource(name).toExternalForm();
    }

    /** 切换偏好并落盘（不即时生效，重启后 init 读取）。返回是否写入成功 */
    public static boolean persist(Mode mode) {
        Path file = prefFile();
        if (file == null) {
            return false;
        }
        Properties props = new Properties();
        props.setProperty(KEY_THEME, mode.name());
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, null);
            return true;
        } catch (IOException e) {
            System.err.println("主题偏好写入失败：" + e.getMessage());
            return false;
        }
    }

    private static Mode loadPreference() {
        Path file = prefFile();
        if (file == null || !Files.exists(file)) {
            return Mode.LIGHT;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            return "DARK".equals(props.getProperty(KEY_THEME)) ? Mode.DARK : Mode.LIGHT;
        } catch (IOException e) {
            return Mode.LIGHT;
        }
    }

    private static Path prefFile() {
        Path dataRoot = AppPaths.dataRoot();
        return dataRoot == null ? null : dataRoot.resolve(PREF_FILE);
    }
}
