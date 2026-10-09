package com.wms.wmsclient.util;

import com.wms.common.dto.LoginResponse;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 登录态落盘（FR-4 复用 FR-6 定稿约定）：token 存 data/session.json。
 * 打包后 data/ 与 app/ 平级，升级只替换 app/ 内主 jar，data/ 从不触碰 → 升级后免重登。
 * IO 失败一律静默降级为「未保存」，绝不影响登录主流程。
 */
public final class TokenStore {

    private static final String FILE = "session.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TokenStore() {
    }

    /** 登录成功后保存登录态 */
    public static void save(LoginResponse session) {
        try {
            Path dataRoot = AppPaths.dataRoot();
            if (dataRoot == null) {
                return;
            }
            Files.writeString(dataRoot.resolve(FILE), MAPPER.writeValueAsString(session));
        } catch (Exception ignored) {
            // 落盘失败不影响本次登录，仅下次启动需重登
        }
    }

    /** 启动时恢复登录态；无存档或解析失败返回 null（回登录窗） */
    public static LoginResponse load() {
        try {
            Path dataRoot = AppPaths.dataRoot();
            if (dataRoot == null) {
                return null;
            }
            Path file = dataRoot.resolve(FILE);
            if (!Files.isRegularFile(file)) {
                return null;
            }
            return MAPPER.readValue(Files.readString(file), LoginResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 注销/登录态失效时清除存档 */
    public static void clear() {
        try {
            Path dataRoot = AppPaths.dataRoot();
            if (dataRoot == null) {
                return;
            }
            Files.deleteIfExists(dataRoot.resolve(FILE));
        } catch (Exception ignored) {
            // 清理失败无害：最坏情况是下次启动带着过期 token，401 后重登即可
        }
    }
}
