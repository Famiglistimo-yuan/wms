package com.wms.wmsclient.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP 客户端骨架（FR-6 首个使用方；FR-4 登录切片在此扩展 POST/token 携带）。
 * 错误契约：HTTP 恒 200，业务成败看响应体 Result.code（TECHNICAL_DESIGN §6.1）。
 */
public final class ApiClient {

    /** 服务端地址：开发期固定本机；FR-4 落地时改为可配置（如 data/ 下配置文件） */
    public static final String BASE_URL = "http://localhost:8080";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private ApiClient() {
    }

    /** GET 请求，返回响应体字符串；网络/超时异常原样抛出，由调用方决定降级策略 */
    public static String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
