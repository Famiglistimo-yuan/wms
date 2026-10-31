package com.wms.wmsserver.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 服务端 RSA 密钥对持有者。
 *
 * <p>密钥来源（按优先级）：
 * <ol>
 *   <li>配置 wms.rsa.private-key-base64（PKCS#8 Base64）：生产推荐，密钥固化不随重启变化；
 *       公钥从私钥 modulus 推导（标准 RSA 公钥指数 e=65537）。</li>
 *   <li>启动时临时生成 2048 位密钥对：开发默认，重启后公钥变化，客户端需重拉。</li>
 * </ol>
 *
 * <p>私钥绝不通过 API 暴露；公钥通过 GET /api/auth/public-key 下发。
 */
@Slf4j
@Component
public class RsaKeyHolder {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    /** 标准 RSA 公钥指数（KeyPairGenerator 默认值；用于从私钥 modulus 重建公钥） */
    private static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

    private final KeyPair keyPair;
    private final String publicKeyBase64;

    public RsaKeyHolder(@Value("${wms.rsa.private-key-base64:}") String configuredPrivateKey) {
        if (configuredPrivateKey == null || configuredPrivateKey.isBlank()) {
            this.keyPair = generateTemporary();
            log.warn("未配置 wms.rsa.private-key-base64，使用临时生成的 RSA 密钥对（重启后公钥变化，仅开发适用）");
        } else {
            this.keyPair = loadFromPrivate(configuredPrivateKey);
            log.info("RSA 密钥对从配置加载（wms.rsa.private-key-base64）");
        }
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        log.info("RSA 公钥就绪：指纹={}", fingerprint(keyPair.getPublic()));
    }

    public String publicKeyBase64() {
        return publicKeyBase64;
    }

    /** 用私钥解密客户端发来的密文（Base64） */
    public String decrypt(String base64Cipher) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(base64Cipher));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA 解密失败", e);
        }
    }

    /** 临时生成 2048 位密钥对 */
    private static KeyPair generateTemporary() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 无 RSA Provider", e);
        }
    }

    /** 从 PKCS#8 Base64 私钥加载，公钥从 modulus 推导（假设 e=65537） */
    private static KeyPair loadFromPrivate(String base64PrivateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
            PrivateKey priv = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            RSAPrivateKey rsaPriv = (RSAPrivateKey) priv;
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(rsaPriv.getModulus(), PUBLIC_EXPONENT));
            return new KeyPair(pub, priv);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("加载配置私钥失败", e);
        }
    }

    /** 公钥指纹：SHA-256 前 16 字节十六进制（日志辨识用，非安全用途） */
    private static String fingerprint(PublicKey pub) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(pub.getEncoded());
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "<unavailable>";
        }
    }
}
