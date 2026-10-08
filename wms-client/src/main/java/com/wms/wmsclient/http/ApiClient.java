package com.wms.wmsclient.http;

import com.wms.common.Result;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP 客户端：服务端通信统一入口。
 *
 * <p>错误契约：HTTP 恒 200，业务成败看响应体 Result.code。code != 0 时抛 ApiException，
 * 由调用方（Controller）负责 UI 反馈；网络异常原样抛出。
 *
 * <p>鉴权：ApiClient.auth(token) 设置后，每次请求自动带 Authorization: Bearer 头；
 * ApiClient.clearAuth() 清除。
 */
public final class ApiClient {

    public static final String BASE_URL =
            System.getProperty("wms.server.url", "http://localhost:8080");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 当前登录 token（静态全局，SessionContext 里保存但也同步到此，ApiClient 零依赖 SessionContext） */
    private static volatile String bearerToken;

    private ApiClient() {
    }

    /** 设置 Bearer token，后续请求自动携带 */
    public static void auth(String token) {
        bearerToken = token;
    }

    /** 清除 token（注销时调） */
    public static void clearAuth() {
        bearerToken = null;
    }

    /** GET，带 query params */
    public static String get(String path, Map<String, String> params) throws IOException, InterruptedException {
        String fullPath = buildPath(path, params);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + fullPath))
                .timeout(Duration.ofSeconds(10))
                .GET();
        applyAuth(builder);
        return sendAndGetBody(builder.build());
    }

    /** GET，无 query params */
    public static String get(String path) throws IOException, InterruptedException {
        return get(path, null);
    }

    /** POST，JSON body */
    public static String post(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        applyAuth(builder);
        return sendAndGetBody(builder.build());
    }

    /**
     * 发送请求并解析 Result。code != 0 抛 ApiException，HTTP 非 200 也抛 ApiException。
     * 返回 data 字段（已按 targetClass 反序列化）。
     */
    public static <T> T getResultData(String jsonResponse, Class<T> targetClass) throws IOException {
        Result<T> result = MAPPER.readValue(jsonResponse,
                MAPPER.getTypeFactory().constructParametricType(Result.class, targetClass));
        if (result.getCode() != 0) {
            throw new ApiException(result.getCode(), result.getMessage());
        }
        return result.getData();
    }

    /** 同上，但 data 是字符串类型（list 场景） */
    public static <T> java.util.List<T> getResultList(String jsonResponse, Class<T> elementClass) throws IOException {
        Result<?> result = MAPPER.readValue(jsonResponse, Result.class);
        if (result.getCode() != 0) {
            throw new ApiException(result.getCode(), result.getMessage());
        }
        if (result.getData() == null) {
            return java.util.List.of();
        }
        // 通用 List 反序列化
        return MAPPER.convertValue(result.getData(),
                MAPPER.getTypeFactory().constructCollectionType(java.util.List.class, elementClass));
    }

    /** 仅校验 code == 0，data 丢弃（如 logout） */
    public static void checkSuccess(String jsonResponse) throws IOException {
        Result<?> result = MAPPER.readValue(jsonResponse, Result.class);
        if (result.getCode() != 0) {
            throw new ApiException(result.getCode(), result.getMessage());
        }
    }

    private static void applyAuth(HttpRequest.Builder builder) {
        String token = bearerToken;
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private static String sendAndGetBody(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new ApiException(response.statusCode(), "服务端返回 HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String buildPath(String path, Map<String, String> params) {
        if (params == null || params.isEmpty()) return path;
        StringBuilder sb = new StringBuilder(path);
        sb.append(path.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
