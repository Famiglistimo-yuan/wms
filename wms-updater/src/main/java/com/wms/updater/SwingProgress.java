package com.wms.updater;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;

/**
 * Updater 用户反馈（Swing，JDK 自带零依赖）。
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

    private static boolean headless() {
        return java.awt.GraphicsEnvironment.isHeadless();
    }

    /**
     * 弹无关闭按钮的不定进度对话框（等待/下载/替换期间给用户「程序在工作」的反馈）。
     * 返回 null = headless，静默。所有 Swing 调用经 invokeAndWait 保证 EDT 线程。
     */
    static JDialog showProgress(String message) {
        if (headless()) {
            System.out.println(message);
            return null;
        }
        JDialog[] holder = new JDialog[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                JDialog dialog = new JDialog((Frame) null, "软件升级", Dialog.ModalityType.APPLICATION_MODAL);
                JProgressBar bar = new JProgressBar();
                bar.setIndeterminate(true);
                JPanel panel = new JPanel(new BorderLayout(12, 12));
                panel.add(new javax.swing.JLabel(message), BorderLayout.NORTH);
                panel.add(bar, BorderLayout.CENTER);
                dialog.setContentPane(panel);
                dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);   // 升级中不可关闭
                dialog.setResizable(false);
                dialog.setSize(320, 110);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                holder[0] = dialog;
            });
        } catch (Exception e) {
            System.out.println(message);
        }
        return holder[0];
    }

    /** 关闭进度框（对应 showProgress；null 安全）。错误路径由 errorExit 直接 exit，无需清理 */
    static void hideProgress(JDialog dialog) {
        if (dialog == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
    }

    /** 弹错误对话框后以非零码退出（headless 时打印到 stderr） */
    static void errorExit(String message) {
        if (headless()) {
            System.err.println(message);
        } else {
            JOptionPane.showMessageDialog(null, message, "升级程序", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
}
