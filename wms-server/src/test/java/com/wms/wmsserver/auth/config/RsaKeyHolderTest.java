package com.wms.wmsserver.auth.config;

import com.wms.common.crypto.RsaUtil;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RSA 登录加密链路测试（ADR-011）：RsaUtil 加密 ↔ RsaKeyHolder 解密往返，
 * 覆盖临时密钥与配置私钥（含公钥从 modulus 推导）两种模式。
 */
class RsaKeyHolderTest {

    private static final String PASSWORD = "admin123";

    @Test
    void roundTripWithTemporaryKey() {
        RsaKeyHolder holder = new RsaKeyHolder("");
        String cipher = RsaUtil.encrypt(holder.publicKeyBase64(), PASSWORD);
        assertEquals(PASSWORD, holder.decrypt(cipher));
    }

    @Test
    void configuredPrivateKeyLoadsAndDerivesPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        RsaKeyHolder holder = new RsaKeyHolder(privateKeyBase64);

        // 公钥由私钥 modulus + e=65537 推导，应与原公钥的 X.509 编码一致
        assertEquals(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                holder.publicKeyBase64());
        assertEquals(PASSWORD, holder.decrypt(RsaUtil.encrypt(holder.publicKeyBase64(), PASSWORD)));
    }

    @Test
    void undecryptableCipherThrows() {
        RsaKeyHolder holder = new RsaKeyHolder("");
        String garbage = Base64.getEncoder().encodeToString("not-a-cipher".getBytes());
        assertThrows(IllegalStateException.class, () -> holder.decrypt(garbage));
    }
}
