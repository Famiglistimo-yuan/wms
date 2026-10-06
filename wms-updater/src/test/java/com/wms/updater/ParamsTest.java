package com.wms.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 升级参数解析测试（FR-6）：五参数齐备有效，缺失/非法拒绝。
 */
class ParamsTest {

    private static final String[] FULL = {
            "--url=http://localhost:8080/download/wms-client.jar",
            "--md5=d41d8cd98f00b204e9800998ecf8427e",
            "--target=/opt/WMS/app/wms-client.jar",
            "--pid=12345",
            "--launch=/opt/WMS/WMS.exe"
    };

    @Test
    void parsesAllFiveParams() {
        Params p = Params.parse(FULL);
        assertEquals("http://localhost:8080/download/wms-client.jar", p.url());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", p.md5());
        assertEquals("/opt/WMS/app/wms-client.jar", p.target().toString());
        assertEquals(12345, p.pid());
        assertEquals("/opt/WMS/WMS.exe", p.launch().toString());
    }

    @Test
    void rejectsMissingParam() {
        for (int i = 0; i < FULL.length; i++) {
            String[] incomplete = new String[4];
            System.arraycopy(FULL, 0, incomplete, 0, i);
            System.arraycopy(FULL, i + 1, incomplete, i, 4 - i);
            assertThrows(IllegalArgumentException.class, () -> Params.parse(incomplete),
                    "缺第 " + i + " 个参数应拒绝");
        }
    }

    @Test
    void rejectsBadPidAndUnknownArg() {
        String[] badPid = FULL.clone();
        badPid[3] = "--pid=abc";
        assertThrows(IllegalArgumentException.class, () -> Params.parse(badPid));

        String[] unknown = {FULL[0], "--verbose"};
        assertThrows(IllegalArgumentException.class, () -> Params.parse(unknown));
    }
}
