# Cron Sentinel Client SDK

Cron Sentinel 的**客户端 SDK**，给"别人的项目"引入，用于把心跳上报到 Cron Sentinel 服务端。

## 特性

- **纯 JDK 实现，零运行时依赖**：HTTP 用 `java.net.HttpURLConnection`，日志用 `java.util.logging`，不引入任何 Spring / OkHttp / Hutool / slf4j。引入后不会和你项目里的依赖冲突。
- **JDK 8 编译**：可在 Spring Boot 2.7（JDK8/11）、Spring Boot 3.x（JDK17）以及普通 Java 项目中使用。
- **对业务绝对安全**：`ping/start/fail` 内部捕获所有异常，绝不抛出、绝不阻塞业务线程（异步模式）。服务端不可达也不影响你的业务。
- **异步 + 重试**：默认异步发送（守护线程池，小线程数 + 有界队列，满则丢弃记日志），失败可重试。

## 一、引入依赖

> 先在本地构建安装：`mvn clean install`（产物不携带任何传递依赖）。

Maven：

```xml
<dependency>
    <groupId>com.cronsentinel</groupId>
    <artifactId>cron-sentinel-sdk</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 二、配置（应用启动时一次性）

```java
import com.cronsentinel.sdk.CronSentinel;
import com.cronsentinel.sdk.config.CronSentinelConfig;

CronSentinel.config(CronSentinelConfig.builder()
        .baseUrl("https://your-domain.com") // 必填，Cron Sentinel 服务端地址
        .timeoutMillis(3000)                 // 默认 3000
        .async(true)                         // 默认 true（不阻塞业务线程）
        .maxRetries(2)                       // 默认 2
        .retryBackoffMillis(1000)            // 默认 1000
        .build());
```

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `baseUrl` | （必填） | 服务端地址，如 `https://your-domain.com` |
| `timeoutMillis` | 3000 | 连接 / 读取超时（毫秒） |
| `async` | true | 是否异步发送 |
| `maxRetries` | 2 | 失败重试次数 |
| `retryBackoffMillis` | 1000 | 重试间隔（毫秒） |

## 三、核心 API

```java
CronSentinel.ping("your-token");   // 成功 -> GET /ping/{token}
CronSentinel.start("your-token");  // 开始 -> GET /ping/{token}/start
CronSentinel.fail("your-token");   // 失败 -> GET /ping/{token}/fail
```

## 四、三种典型用法

### a) 普通 Java 项目

```java
public class Main {
    public static void main(String[] args) {
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("https://your-domain.com")
                .build());

        try {
            doBackup();
            CronSentinel.ping("your-token");   // 报平安
        } catch (Exception e) {
            CronSentinel.fail("your-token");   // 上报失败，立即告警
        }

        // 应用退出前
        CronSentinel.shutdown();
    }
}
```

### b) Spring Boot 2.7（JDK 8/11）的 `@Scheduled`

```java
@Component
public class BackupJob {

    @PostConstruct
    public void init() {
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("https://your-domain.com")
                .build());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void backup() {
        try {
            doBackup();
            CronSentinel.ping("your-token");
        } catch (Exception e) {
            CronSentinel.fail("your-token");
        }
    }

    @PreDestroy
    public void destroy() {
        CronSentinel.shutdown();
    }
}
```

> Spring Boot 2.7 用 `javax.annotation.PostConstruct` / `PreDestroy`。

### c) Spring Boot 3.x（JDK 17）的 `@Scheduled`

代码与 2.7 几乎相同，仅注解包名不同（Spring Boot 3 用 `jakarta.annotation.*`）：

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class BackupJob {

    @PostConstruct
    public void init() {
        CronSentinel.config(CronSentinelConfig.builder()
                .baseUrl("https://your-domain.com")
                .build());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void backup() {
        try {
            doBackup();
            CronSentinel.ping("your-token");
        } catch (Exception e) {
            CronSentinel.fail("your-token");
        }
    }

    @PreDestroy
    public void destroy() {
        CronSentinel.shutdown();
    }
}
```

## 五、`monitor(...)` 包裹用法（推荐）

自动在业务前后上报：执行前 `start()`，成功后 `ping()`，抛异常则 `fail()` 并**重新抛出原始异常**。

```java
// 无返回值（Runnable）
CronSentinel.monitor("your-token", () -> {
    doSomething();
});

// 有返回值（Callable）
String result = CronSentinel.monitor("your-token", () -> {
    return doSomethingAndReturn();
});
```

## 六、资源释放

```java
CronSentinel.shutdown(); // 优雅关闭内部守护线程池，建议在应用退出时调用
```

## 七、运行 Demo / 测试

- 单元测试（含"服务端不可达也不抛异常"的安全验证）：

```bash
mvn test
```

- 手动演示 `config -> ping`（可指向本地服务端 `localhost:8080`）：直接在 IDE 运行
  `src/test/java/com/cronsentinel/sdk/demo/CronSentinelDemo.java` 的 `main` 方法。
  即使服务端没启动，程序也不会报错（心跳上报对业务安全）。

## 八、目录结构

```
src/main/java/com/cronsentinel/sdk
├── CronSentinel.java              静态门面：config / ping / start / fail / monitor / shutdown
├── config/CronSentinelConfig.java 不可变配置 + Builder
├── core/PingType.java             心跳类型(SUCCESS/START/FAIL)
├── core/HeartbeatService.java     异步/同步发送 + 重试 + 守护线程池
├── http/HttpClient.java           基于 HttpURLConnection 的极简 GET
└── util/NamedDaemonThreadFactory.java  守护线程工厂
```

## 九、日志

SDK 使用 JUL（`java.util.logging`），Logger 名为 `com.cronsentinel.sdk`。
默认级别下仅在失败 / 队列满等情况记 `WARNING`；成功上报为 `FINE`（默认不输出）。
可在你项目的日志框架里桥接 JUL，或通过 `logging.properties` 调整级别。
