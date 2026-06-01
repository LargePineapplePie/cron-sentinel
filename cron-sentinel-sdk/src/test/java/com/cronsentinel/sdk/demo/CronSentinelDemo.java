package com.cronsentinel.sdk.demo;

import com.cronsentinel.sdk.CronSentinel;
import com.cronsentinel.sdk.config.CronSentinelConfig;

/**
 * 手动运行的演示程序：展示 config -> ping 的完整调用。
 *
 * <p>运行方式（指向本地服务端 localhost:8080）：
 * <pre>
 *   mvn -q -DskipTests test-compile exec... // 或在 IDE 中直接运行本 main 方法
 * </pre>
 * 即使服务端未启动，程序也不会报错退出（心跳上报对业务安全）。
 */
public class CronSentinelDemo {

    public static void main(String[] args) throws Exception {
        // 1. 全局配置（指向本地服务端，用同步模式便于在 demo 中立刻看到日志）
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("http://localhost:8080")
                .async(false)
                .timeoutMillis(3000)
                .maxRetries(2)
                .retryBackoffMillis(1000)
                .build());

        String token = args.length > 0 ? args[0] : "replace-with-your-token";

        // 2. 直接上报成功心跳
        System.out.println("==> ping");
        CronSentinel.ping(token);

        // 3. 用 monitor 包裹一段业务逻辑（成功路径）
        System.out.println("==> monitor success");
        String result = CronSentinel.monitor(token, () -> {
            // 模拟业务
            return "done";
        });
        System.out.println("monitor 返回: " + result);

        // 4. monitor 失败路径：会上报 fail 并重新抛出业务异常
        System.out.println("==> monitor failure");
        try {
            CronSentinel.monitor(token, (Runnable) () -> {
                throw new RuntimeException("模拟业务失败");
            });
        } catch (RuntimeException e) {
            System.out.println("业务异常已正常抛出: " + e.getMessage());
        }

        // 5. 退出前优雅关闭
        CronSentinel.shutdown();
        System.out.println("==> done");
    }
}
