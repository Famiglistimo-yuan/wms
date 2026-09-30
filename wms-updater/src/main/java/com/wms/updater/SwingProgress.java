package com.wms.updater;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Updater 用户反馈（Swing，JDK 自带零依赖；仅错误弹窗，成功路径无打扰——替换耗时秒级）。
 * 若系统无显示环境（headless）则降级为控制台输出，不阻塞退出码。
 */
final class SwingProgress {

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 外观失败用默认，不影响功能
        }
    }

    private SwingProgress() {
    }

    /** 弹错误对话框后以非零码退出（headless 时打印到 stderr） */
    static void errorExit(String message) {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
        } else {
            JOptionPane.showMessageDialog(null, message, "升级程序", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
}
