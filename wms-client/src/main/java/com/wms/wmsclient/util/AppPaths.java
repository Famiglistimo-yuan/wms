package com.wms.wmsclient.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 客户端运行环境路径解析（FR-6 定稿约定，FR-4 的 token 落盘复用同一约定）：
 * 打包后数据根 = 应用根下 data/（与 app/ 平级，升级只替换 app/ 内主 jar，data/ 从不触碰）；
 * dev 模式（IDE 直跑，无 app-image 结构）退化为工作目录下 data/。
 */
public final class AppPaths {

    private AppPaths() {
    }

    /**
     * 应用根目录（app-image 结构）：Mac 为 WMS.app/Contents，Windows 为 WMS/。
     * 判定依据 = 本类所在 jar 位于名为 app/ 的目录、且其兄弟存在 runtime/；
     * dev 模式 class 位于 classes 目录，结构不匹配，返回 null。
     */
    public static Path appRoot() {
        try {
            Path jar = Paths.get(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path appDir = jar.getParent();
            if (appDir != null
                    && "app".equals(appDir.getFileName().toString())
                    && Files.isDirectory(appDir.resolveSibling("runtime"))) {
                return appDir.getParent();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 数据根目录：不存在则创建；创建失败（如只读目录）返回 null，调用方降级提示 */
    public static Path dataRoot() {
        Path appRoot = appRoot();
        Path data = appRoot != null ? appRoot.resolve("data") : Paths.get("data");
        try {
            Files.createDirectories(data);
            return data;
        } catch (IOException e) {
            return null;
        }
    }
}
