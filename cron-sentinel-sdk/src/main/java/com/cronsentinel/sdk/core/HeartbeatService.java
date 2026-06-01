package com.cronsentinel.sdk.core;

import com.cronsentinel.sdk.config.CronSentinelConfig;
import com.cronsentinel.sdk.http.HttpClient;
import com.cronsentinel.sdk.util.NamedDaemonThreadFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 心跳发送的核心实现：负责拼 URL、异步 / 同步发送、失败重试。
 *
 * <p>对外提供的 {@link #send(String, PingType)} 方法【绝对安全】：
 * 内部所有异常都会被捕获并仅记录 JUL 日志，不会抛给调用方。
 */
public final class HeartbeatService {

    private static final Logger LOG = Logger.getLogger("com.cronsentinel.sdk");

    /** 异步线程数，保持很小，避免拖垮用户应用 */
    private static final int ASYNC_THREADS = 2;

    /** 有界队列容量，队列满时丢弃并记日志 */
    private static final int QUEUE_CAPACITY = 1000;

    private final CronSentinelConfig config;

    /** 仅在 async=true 时创建 */
    private final ThreadPoolExecutor executor;

    public HeartbeatService(CronSentinelConfig config) {
        this.config = config;
        if (config.isAsync()) {
            this.executor = new ThreadPoolExecutor(
                    ASYNC_THREADS,
                    ASYNC_THREADS,
                    60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                    new NamedDaemonThreadFactory("cron-sentinel-sdk"),
                    new DiscardAndLogPolicy());
            this.executor.allowCoreThreadTimeOut(true);
        } else {
            this.executor = null;
        }
    }

    /**
     * 发送一次心跳。绝不抛异常。
     *
     * @param token 检查项 token
     * @param type  心跳类型
     */
    public void send(String token, PingType type) {
        if (token == null || token.trim().isEmpty()) {
            LOG.warning("[cron-sentinel] token 为空，已忽略本次上报");
            return;
        }
        final String url = buildUrl(token, type);

        if (config.isAsync() && executor != null) {
            try {
                executor.execute(() -> sendWithRetry(url, type));
            } catch (Throwable t) {
                // 理论上被拒绝策略处理；这里再兜底一层，绝不外抛
                LOG.log(Level.WARNING, "[cron-sentinel] 异步提交失败: " + t.getMessage(), t);
            }
        } else {
            sendWithRetry(url, type);
        }
    }

    /**
     * 同步发送 + 重试。捕获所有异常，仅记日志。
     */
    private void sendWithRetry(String url, PingType type) {
        int attempts = config.getMaxRetries() + 1; // 首次 + 重试
        for (int i = 1; i <= attempts; i++) {
            try {
                HttpClient.get(url, config.getTimeoutMillis());
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[cron-sentinel] 上报成功 type=" + type + " url=" + url
                            + (i > 1 ? " (第 " + i + " 次尝试)" : ""));
                }
                return;
            } catch (Throwable t) {
                boolean lastAttempt = (i == attempts);
                LOG.log(lastAttempt ? Level.WARNING : Level.INFO,
                        "[cron-sentinel] 上报失败 type=" + type + " 第 " + i + "/" + attempts
                                + " 次: " + t.getMessage());
                if (lastAttempt) {
                    return;
                }
                sleepQuietly(config.getRetryBackoffMillis());
            }
        }
    }

    private String buildUrl(String token, PingType type) {
        return config.getBaseUrl() + "/ping/" + encode(token) + type.getPathSuffix();
    }

    private String encode(String token) {
        try {
            // token 一般是十六进制，编码以防特殊字符；保留常规字符可读性即可
            return URLEncoder.encode(token, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return token;
        }
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 优雅关闭线程池，最多等待 5 秒。
     */
    public void shutdown() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 拒绝策略：队列满时丢弃任务并记日志，绝不阻塞或抛异常给业务线程。
     */
    private static final class DiscardAndLogPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LOG.warning("[cron-sentinel] 异步队列已满，丢弃一次心跳上报（请检查服务端可达性或上报频率）");
        }
    }
}
