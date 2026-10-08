package com.wms.wmsserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "wms.jwt.secret=test-only-secret-0123456789abcdef")
// CI 无 application-local.yaml，JwtProperties 的 @NotBlank 会使上下文启动失败；
// 注入仅测试用的哑密钥，生产环境的真实 secret 由 local yaml 提供（不入库）
class WmsServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
