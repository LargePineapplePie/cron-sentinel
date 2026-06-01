package com.cronsentinel.sdk.config;

/**
 * Cron Sentinel SDK 的全局配置（不可变对象，通过 {@link Builder} 构建）。
 *
 * <p>示例：
 * <pre>{@code
 * CronSentinelConfig config = CronSentinelConfig.builder()
 *         .baseUrl("https://your-domain.com")
 *         .async(true)
 *         .maxRetries(2)
 *         .build();
 * }</pre>
 */
public final class CronSentinelConfig {

    /** 服务端地址，如 https://your-domain.com（必填，已去除末尾斜杠） */
    private final String baseUrl;

    /** 连接 / 读取超时（毫秒） */
    private final int timeoutMillis;

    /** 是否异步发送：true 不阻塞业务线程 */
    private final boolean async;

    /** 失败重试次数 */
    private final int maxRetries;

    /** 重试间隔（毫秒） */
    private final long retryBackoffMillis;

    private CronSentinelConfig(Builder b) {
        this.baseUrl = b.baseUrl;
        this.timeoutMillis = b.timeoutMillis;
        this.async = b.async;
        this.maxRetries = b.maxRetries;
        this.retryBackoffMillis = b.retryBackoffMillis;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public boolean isAsync() {
        return async;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "CronSentinelConfig{baseUrl='" + baseUrl + '\''
                + ", timeoutMillis=" + timeoutMillis
                + ", async=" + async
                + ", maxRetries=" + maxRetries
                + ", retryBackoffMillis=" + retryBackoffMillis
                + '}';
    }

    /**
     * 配置构建器，提供合理默认值。
     */
    public static final class Builder {
        private String baseUrl;
        private int timeoutMillis = 3000;
        private boolean async = true;
        private int maxRetries = 2;
        private long retryBackoffMillis = 1000L;

        private Builder() {
        }

        /** 服务端地址，如 https://your-domain.com（必填） */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** 连接 / 读取超时（毫秒），默认 3000 */
        public Builder timeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /** 是否异步发送，默认 true */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /** 失败重试次数，默认 2 */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /** 重试间隔（毫秒），默认 1000 */
        public Builder retryBackoffMillis(long retryBackoffMillis) {
            this.retryBackoffMillis = retryBackoffMillis;
            return this;
        }

        public CronSentinelConfig build() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl 不能为空");
            }
            // 去掉末尾斜杠，避免拼出 //ping
            String normalized = baseUrl.trim();
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            this.baseUrl = normalized;

            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("timeoutMillis 必须大于 0");
            }
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries 不能为负");
            }
            if (retryBackoffMillis < 0) {
                throw new IllegalArgumentException("retryBackoffMillis 不能为负");
            }
            return new CronSentinelConfig(this);
        }
    }
}
