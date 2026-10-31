package com.wms.wmsclient.http;

import com.wms.common.Result;
import com.wms.common.crypto.RsaUtil;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 服务端 RSA 公钥缓存与口令加密。
 *
 * <p>App 启动时异步调用 {@link #refresh()} 预热；首次登录前未预热时
 * {@link #encryptPassword(String)} 会同步拉取一次。
 *
 * <p>公钥缓存为 volatile 单例，服务端重启换钥后客户端需重拉——
 * 登录解密失败（服务端返回"用户名或口令错误"）时可手动调 refresh 重试。
 */
public final class RsaClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static volatile String publicKey;

    private RsaClient() {
    }

    /** 拉取并缓存公钥（App 启动时异步调用） */
    @SuppressWarnings("unchecked")
    public static void refresh() throws Exception {
        String json = ApiClient.get("/api/auth/public-key");
        Result<Map<String, String>> r = MAPPER.readValue(json,
                MAPPER.getTypeFactory().constructParametricType(Result.class, Map.class));
        if (r.getCode() != 0) {
            throw new ApiException(r.getMessage());
        }
        String key = r.getData().get("publicKey");
        if (key == null || key.isBlank()) {
            throw new ApiException("服务端公钥响应缺 publicKey 字段");
        }
        publicKey = key;
    }

    /** 加密口令：用缓存公钥做 RSA 加密，未缓存则同步拉取一次 */
    public static String encryptPassword(String plainPassword) throws Exception {
        String key = publicKey;
        if (key == null || key.isBlank()) {
            refresh();
            key = publicKey;
        }
        return RsaUtil.encrypt(key, plainPassword);
    }
}
