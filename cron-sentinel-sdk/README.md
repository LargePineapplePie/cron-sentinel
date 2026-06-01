# Cron Sentinel Client SDK

Cron Sentinel 的**客户端 SDK**，给"别人的项目"引入，用于把心跳上报到 Cron Sentinel 服务端。

## 特性

- **纯 JDK 实现，零运行时依赖**：HTTP 用 `java.net.HttpURLConnection`，日志用 `java.util.logging`，不引入任何 Spring / OkHttp / Hutool / slf4j。引入后不会和你项目里的依赖冲突。
- **JDK 8 编译**：可在 Spring Boot 2.7（JDK8/11）、Spring Boot 3.x（JDK17）以及普通 Java 项目中使用。
- **对业务绝对安全**：`ping/start/fail` 内部捕获所有异常，绝不抛出、绝不阻塞业务线程（异步模式）。服务端不可达也不影响你的业务。
- **异步 + 重试**：默认异步发送（守护线程池，小线程数 + 有界队列，满则丢弃记日志），失败可重试。

---

# 第一部分：打包与安装

SDK 是一个**独立的 Maven 工程**（自己的 `pom.xml`、坐标 `com.cronsentinel:cron-sentinel-sdk`、`packaging=jar`），打包就是标准 Maven 流程。

> 构建需要 **JDK 8+**（编译目标是 Java 8 字节码，major version 52）。
> 下面命令里的 `mvn` 换成你本机的 Maven 即可（本仓库验证时用的是免安装工具链
> `E:\hbl-project\.toolchain\mvn\apache-maven-3.9.16\bin\mvn.cmd`）。

## 1. 打 jar 包

```bash
mvn clean package
```

产物：`target/cron-sentinel-sdk-0.0.1-SNAPSHOT.jar`（约 15KB）。

> **不需要 shade / assembly 这类"打胖包"插件**。因为 SDK 运行时零依赖，这个 jar 本身就是完整可用的；
> 别人引入后不会再拉进任何传递依赖。胖包插件是用来把第三方依赖塞进去的，本项目没有依赖，故不需要。

## 2. 安装到本地 Maven 仓库（.m2）

让本机其他 Maven 项目能依赖它。两种方式任选其一：

### 方式 A：直接 install（推荐，会重新构建再安装）

```bash
mvn clean install
# 想跳过测试更快： mvn clean install -DskipTests
```

### 方式 B：把已打好的 jar 直接装进去（不重新构建）

```bash
mvn install:install-file -Dfile=target/cron-sentinel-sdk-0.0.1-SNAPSHOT.jar -DpomFile=pom.xml
```

（`-DpomFile=pom.xml` 让它沿用工程里的 groupId/artifactId/version，无需手填。）

### 安装到哪里？怎么验证？

安装路径取决于你的 Maven `settings.xml` 里 `localRepository` 的配置：

- **默认**：`C:\Users\<用户名>\.m2\repository`（Windows）/ `~/.m2/repository`（Linux/Mac）
- **本机当前实际路径**（已自定义）：`D:\apache-maven-3.8.5\maven-repository`

安装成功后会在对应仓库下出现：

```
<本地仓库>\com\cronsentinel\cron-sentinel-sdk\0.0.1-SNAPSHOT\
    ├── cron-sentinel-sdk-0.0.1-SNAPSHOT.jar
    └── cron-sentinel-sdk-0.0.1-SNAPSHOT.pom
```

验证（Windows PowerShell，按你的实际仓库路径）：

```powershell
Get-ChildItem "D:\apache-maven-3.8.5\maven-repository\com\cronsentinel\cron-sentinel-sdk" -Recurse
```

> 注意：使用方项目必须用**同一套** Maven / `settings.xml`（即指向同一个本地仓库），才能解析到刚装好的依赖。

## 3. 发布到私服 / 中央仓库（多人或线上用，可选）

```bash
mvn clean deploy
```

需要先在 `pom.xml` 配 `<distributionManagement>` 并在 `settings.xml` 配好私服（Nexus/Artifactory）凭据。
个人/单机自测用第 2 步的 `install` 即可，无需 deploy。

## 4. 可选：附带源码包与 Javadoc 包

若要作为正式对外发布的 SDK，建议同时产出 `-sources.jar` 和 `-javadoc.jar`，方便使用方在 IDE 里看源码与注释。
在 `pom.xml` 的 `<build><plugins>` 中加入：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-source-plugin</artifactId>
    <version>3.2.1</version>
    <executions>
        <execution>
            <id>attach-sources</id>
            <goals><goal>jar</goal></goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    <version>3.6.3</version>
    <executions>
        <execution>
            <id>attach-javadocs</id>
            <goals><goal>jar</goal></goals>
        </execution>
    </executions>
</plugin>
```

加完后 `mvn install` 会一并安装 sources/javadoc 包。

---

# 第二部分：在别的项目中使用

## 前提：先在服务端创建检查项，拿到 token

不管哪种语言/方式，都要先有一个 token：在服务端状态页（如 `http://localhost:8080/`）用表单创建一个检查项，复制它的 `token`（一长串十六进制）。这个 token 就是该任务的唯一身份。

> 如果"别的项目"在另一台机器/服务器上，`baseUrl` 不能写 `localhost`，要写服务端**实际可被访问**的地址（局域网 IP 或域名），并确保网络可达。

## 1. 引入依赖

先按"第一部分第 2 步"把 SDK `install` 到本地仓库，然后在目标项目的 `pom.xml` 加：

```xml
<dependency>
    <groupId>com.cronsentinel</groupId>
    <artifactId>cron-sentinel-sdk</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 2. 配置（应用启动时一次性）

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

## 3. 核心 API

```java
CronSentinel.ping("your-token");   // 成功 -> GET /ping/{token}
CronSentinel.start("your-token");  // 开始 -> GET /ping/{token}/start
CronSentinel.fail("your-token");   // 失败 -> GET /ping/{token}/fail
```

## 4. 三种典型用法

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
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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

## 5. `monitor(...)` 包裹用法（推荐）

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

## 6. 资源释放

```java
CronSentinel.shutdown(); // 优雅关闭内部守护线程池，建议在应用退出时调用
```

## 7. 非 Java 项目 / 脚本（无需 SDK）

SDK 本质上就是发一个 HTTP GET，任何能发 HTTP 请求的环境都能直接用：

```bash
# Linux crontab：备份脚本成功后 curl 报平安
0 2 * * * /opt/backup.sh && curl -fsS https://your-domain.com/ping/your-token

# 失败上报
curl -fsS https://your-domain.com/ping/your-token/fail
```

```powershell
# Windows PowerShell / 计划任务
Invoke-RestMethod -Uri "https://your-domain.com/ping/your-token"
```

```python
# Python
import urllib.request
urllib.request.urlopen("https://your-domain.com/ping/your-token", timeout=3)
```

---

# 第三部分：开发与参考

## 运行 Demo / 测试

- 单元测试（含"服务端不可达也不抛异常"的安全验证）：

```bash
mvn test
```

- 手动演示 `config -> ping`（可指向本地服务端 `localhost:8080`）：直接在 IDE 运行
  `src/test/java/com/cronsentinel/sdk/demo/CronSentinelDemo.java` 的 `main` 方法。
  即使服务端没启动，程序也不会报错（心跳上报对业务安全）。

## 目录结构

```
src/main/java/com/cronsentinel/sdk
├── CronSentinel.java              静态门面：config / ping / start / fail / monitor / shutdown
├── config/CronSentinelConfig.java 不可变配置 + Builder
├── core/PingType.java             心跳类型(SUCCESS/START/FAIL)
├── core/HeartbeatService.java     异步/同步发送 + 重试 + 守护线程池
├── http/HttpClient.java           基于 HttpURLConnection 的极简 GET
└── util/NamedDaemonThreadFactory.java  守护线程工厂
```

## 日志

SDK 使用 JUL（`java.util.logging`），Logger 名为 `com.cronsentinel.sdk`。
默认级别下仅在失败 / 队列满等情况记 `WARNING`；成功上报为 `FINE`（默认不输出）。
可在你项目的日志框架里桥接 JUL，或通过 `logging.properties` 调整级别。
