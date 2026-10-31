package com.wms.common.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加密工具（两端共享，仅"公钥加密"侧）。
 *
 * <p>填充方式：OAEPWithSHA-256AndMGF1Padding（PKCS#1 v2.1），
 * 比 PKCS#1 v1.5 更抗 Bleichenbacher 攻击。
 *
 * <p>长度限制：2048 位密钥 + OAEP(SHA-256) 单次明文上限 = 256 - 2*32 - 2 = 190 字节，
 * 装登录口令（≤ 20 字节）绰绰有余；若需加密长数据请走 AES 信封方案。
 *
 * <p>私钥解密侧仅在服务端实现（RsaKeyHolder），避免私钥逻辑泄漏到客户端。
 */
public final class RsaUtil {

    /** 算法常量：RSA + OAEP 填充 + SHA-256 MGF */
    private static final String TRANSFORMATION =
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private RsaUtil() {
    }

    /**
     * 用 Base64 编码的 X.509 公钥加密明文。
     *
     * @param base64PublicKey 服务端下发的公钥（Base64 字符串）
     * @param plaintext       明文（UTF-8）
     * @return 密文的 Base64 字符串
     */
    public static String encrypt(String base64PublicKey, String plaintext) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA 加密失败", e);
        }
    }
}
