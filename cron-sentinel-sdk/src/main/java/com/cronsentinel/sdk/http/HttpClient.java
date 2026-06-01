package com.cronsentinel.sdk.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 极简 HTTP 客户端，仅基于 JDK 自带的 {@link HttpURLConnection}，零第三方依赖。
 */
public final class HttpClient {

    private HttpClient() {
    }

    /**
     * 发送一次 GET 请求。
     *
     * @param url           完整 URL
     * @param timeoutMillis 连接 / 读取超时
     * @throws IOException 网络错误，或服务端返回非 2xx 状态码
     */
    public static void get(String url, int timeoutMillis) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "cron-sentinel-sdk");

            int code = conn.getResponseCode();
            // 读尽响应体，便于连接复用并释放底层资源
            drainAndClose(safeStream(conn, code));

            if (code < 200 || code >= 300) {
                throw new IOException("非 2xx 响应: HTTP " + code + " (" + url + ")");
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static InputStream safeStream(HttpURLConnection conn, int code) {
        try {
            return (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        } catch (IOException e) {
            return conn.getErrorStream();
        }
    }

    private static void drainAndClose(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            byte[] buf = new byte[1024];
            while (in.read(buf) != -1) {
                // 丢弃响应体内容，仅需把流读完
            }
        } catch (IOException ignored) {
            // 读取响应体失败不重要，忽略
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
