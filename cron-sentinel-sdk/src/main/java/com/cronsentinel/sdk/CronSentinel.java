package com.cronsentinel.sdk;

import com.cronsentinel.sdk.config.CronSentinelConfig;
import com.cronsentinel.sdk.core.HeartbeatService;
import com.cronsentinel.sdk.core.PingType;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Cron Sentinel 客户端 SDK 的统一入口（静态门面）。
 *
 * <p>典型用法：
 * <pre>{@code
 * // 应用启动时一次性配置
 * CronSentinel.config(CronSentinelConfig.builder()
 *         .baseUrl("https://your-domain.com")
 *         .async(true)
 *         .maxRetries(2)
 *         .build());
 *
 * // 业务中上报
 * CronSentinel.ping("your-token");
 * }</pre>
 *
 * <p><b>安全保证：</b> {@link #ping}、{@link #start}、{@link #fail} 内部捕获所有异常，
 * 绝不会把异常抛给调用方，也不会阻塞业务线程（async=true 时）。
 */
public final class CronSentinel {

    private static final Logger LOG = Logger.getLogger("com.cronsentinel.sdk");

    /** 全局唯一的心跳服务实例，由 {@link #config} 初始化 */
    private static volatile HeartbeatService service;

    private CronSentinel() {
    }

    /**
     * 全局配置（一般在应用启动时调用一次）。重复调用会用新配置替换旧的，
     * 并关闭旧配置对应的线程池。
     *
     * @param config 配置对象
     */
    public static synchronized void config(CronSentinelConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config 不能为 null");
        }
        HeartbeatService old = service;
        service = new HeartbeatService(config);
        if (old != null) {
            old.shutdown();
        }
        LOG.info("[cron-sentinel] SDK 已配置: " + config);
    }

    /**
     * 上报成功心跳 -> GET /ping/{token}
     *
     * @param token 检查项 token
     */
    public static void ping(String token) {
        dispatch(token, PingType.SUCCESS);
    }

    /**
     * 上报开始 -> GET /ping/{token}/start
     *
     * @param token 检查项 token
     */
    public static void start(String token) {
        dispatch(token, PingType.START);
    }

    /**
     * 上报失败 -> GET /ping/{token}/fail
     *
     * @param token 检查项 token
     */
    public static void fail(String token) {
        dispatch(token, PingType.FAIL);
    }

    /**
     * 包裹执行业务逻辑（无返回值）：执行前上报 start，成功后上报 ping，
     * 抛异常则上报 fail 并把原异常重新抛出（不吞掉用户异常）。
     *
     * @param token 检查项 token
     * @param task  业务逻辑
     */
    public static void monitor(String token, Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task 不能为 null");
        }
        start(token);
        try {
            task.run();
            ping(token);
        } catch (RuntimeException | Error e) {
            fail(token);
            // 重新抛出用户的原始异常，绝不吞掉
            throw e;
        }
    }

    /**
     * 包裹执行业务逻辑（有返回值）：执行前上报 start，成功后上报 ping，
     * 抛异常则上报 fail 并把原异常重新抛出（不吞掉用户异常）。
     *
     * @param token 检查项 token
     * @param task  业务逻辑
     * @param <T>   返回值类型
     * @return 业务逻辑的返回值
     * @throws Exception 业务逻辑抛出的原始异常
     */
    public static <T> T monitor(String token, Callable<T> task) throws Exception {
        if (task == null) {
            throw new IllegalArgumentException("task 不能为 null");
        }
        start(token);
        try {
            T result = task.call();
            ping(token);
            return result;
        } catch (Exception e) {
            fail(token);
            // 重新抛出用户的原始异常，绝不吞掉
            throw e;
        }
    }

    /**
     * 优雅关闭内部线程池，建议在应用退出时调用。
     */
    public static synchronized void shutdown() {
        HeartbeatService s = service;
        if (s != null) {
            s.shutdown();
            service = null;
            LOG.info("[cron-sentinel] SDK 已关闭");
        }
    }

    private static void dispatch(String token, PingType type) {
        HeartbeatService s = service;
        if (s == null) {
            LOG.warning("[cron-sentinel] SDK 未初始化，请先调用 CronSentinel.config(...)。已忽略本次上报 type=" + type);
            return;
        }
        // send 内部已保证不抛异常，这里再兜底一层
        try {
            s.send(token, type);
        } catch (Throwable t) {
            LOG.warning("[cron-sentinel] 上报异常已被忽略: " + t.getMessage());
        }
    }
}
