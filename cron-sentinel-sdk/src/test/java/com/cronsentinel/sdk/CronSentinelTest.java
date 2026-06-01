package com.cronsentinel.sdk;

import com.cronsentinel.sdk.config.CronSentinelConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 SDK 的核心契约：
 * 1. config -> ping 完整调用链可用；
 * 2. 即使服务端不可达，ping/start/fail 也绝不抛异常（业务安全）；
 * 3. monitor 成功返回值正确，失败时重新抛出原始异常。
 */
class CronSentinelTest {

    @AfterEach
    void tearDown() {
        CronSentinel.shutdown();
    }

    @Test
    void config_then_ping_should_not_throw_even_if_server_unreachable() {
        // 指向一个不可达端口，模拟服务端宕机；同步模式以便确实走到网络发送
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("http://127.0.0.1:1")
                .async(false)
                .timeoutMillis(500)
                .maxRetries(1)
                .retryBackoffMillis(10)
                .build());

        assertDoesNotThrow(() -> CronSentinel.ping("demo-token"));
        assertDoesNotThrow(() -> CronSentinel.start("demo-token"));
        assertDoesNotThrow(() -> CronSentinel.fail("demo-token"));
    }

    @Test
    void ping_before_config_should_not_throw() {
        // 未初始化时调用，应安全忽略
        assertDoesNotThrow(() -> CronSentinel.ping("demo-token"));
    }

    @Test
    void monitor_should_return_value_on_success() throws Exception {
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("http://127.0.0.1:1")
                .async(true)
                .build());

        int result = CronSentinel.monitor("demo-token", () -> 42);
        assertEquals(42, result);
    }

    @Test
    void monitor_should_rethrow_business_exception() {
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("http://127.0.0.1:1")
                .async(true)
                .build());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                CronSentinel.monitor("demo-token", (Runnable) () -> {
                    throw new IllegalStateException("业务异常");
                }));
        assertEquals("业务异常", ex.getMessage());
    }

    @Test
    void builder_should_reject_blank_baseUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                CronSentinelConfig.builder().baseUrl("  ").build());
    }
}
