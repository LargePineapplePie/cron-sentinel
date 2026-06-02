# Cron Sentinel（任务哨兵）

定时任务心跳监控系统 MVP，采用 **心跳上报 / 超时告警** 模式监控 crontab、Spring Boot `@Scheduled`、XXL-JOB、各类脚本是否按时执行。

## 核心原理

1. 在平台创建一个 **检查项(Check)**，系统生成唯一的 ping URL：`/ping/{token}`
2. 你的定时任务跑完后，请求一次该 URL 来"报平安"
3. 系统预期每隔固定周期收到一次 ping
4. 若超过 `预期周期 + 宽限期` 仍未收到 ping，判定故障并发邮件告警

不侵入用户服务器，只要对方能发 HTTP 请求即可监控；能发现"任务卡死、应用崩溃、cron 没触发"等传统 try-catch 监控不到的情况。

## 技术栈

JDK 17 · Spring Boot 3.0.2 · MySQL 8.x · MyBatis-Plus · Lombok · spring-boot-starter-mail · Thymeleaf · Maven

## 一、配置数据库

1. 启动 MySQL 8.x。
2. 执行建表脚本（已自动建库 `cron_sentinel`）：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

如果是从旧版本数据库升级，先备份数据，再按 `src/main/resources/migration-user-system.sql` 的说明做手动迁移。

3. 在 `src/main/resources/application.yml` 中配置数据源（或用环境变量覆盖）：

```yaml
spring:
  datasource:
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:your_password_here}
```

环境变量方式（推荐，避免明文密码进仓库）：

```powershell
$env:DB_USERNAME="root"; $env:DB_PASSWORD="你的密码"
```

## 二、配置邮件告警

`application.yml` 中的 `spring.mail.*`，以 QQ 邮箱为例（`password` 填**授权码**，不是登录密码）：

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: your_email@qq.com
    password: your_smtp_auth_code   # SMTP 授权码
```

用环境变量覆盖：

```powershell
$env:MAIL_USERNAME="your_email@qq.com"; $env:MAIL_PASSWORD="授权码"
```

> 本地调试若暂时不想发邮件，可设 `MAIL_ENABLED=false`（或 `application.yml` 里 `cron-sentinel.mail.enabled: false`），告警逻辑照常走，只是不真正发送。

## 三、启动

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package
java -jar target/cron-sentinel-0.0.1-SNAPSHOT.jar
```

启动后访问状态页：<http://localhost:8080/>

首次使用先访问 <http://localhost:8080/register> 注册账号，然后登录进入状态页。

> 检查项管理接口和页面需要登录；`/ping/{token}`、`/ping/{token}/start`、`/ping/{token}/fail` 继续免登录，方便 crontab、脚本和 SDK 上报心跳。

## 四、用 curl 跑通完整流程

### 1. 创建检查项

管理接口需要登录会话。MVP 阶段推荐先在网页端创建检查项；若使用 API，请携带登录后的 Cookie。

```bash
curl -X POST http://localhost:8080/api/checks \
  -H "Content-Type: application/json" \
  -d '{"name":"每日数据备份","periodSeconds":60,"graceSeconds":30,"alertEmail":"you@example.com"}'
```

返回中含 `pingUrl`，例如：`http://localhost:8080/ping/8f2c...`

> 这里把周期设成 60s、宽限 30s，方便快速验证超时。

### 2. 报平安（任务成功）

```bash
curl http://localhost:8080/ping/{token}
# 返回 OK，状态变为 UP
```

### 3. 停止 ping 等待超时

不再请求该 URL，等待 `60 + 30 = 90` 秒后，每分钟运行的扫描器会把它置为 **DOWN** 并发送告警邮件。打开状态页可看到状态变红。

### 4. 其它心跳类型

```bash
# 任务开始（仅记录 START 日志，不改状态）
curl http://localhost:8080/ping/{token}/start

# 任务失败（立即置 DOWN 并告警）
curl http://localhost:8080/ping/{token}/fail
```

恢复：DOWN 之后再次 `curl http://localhost:8080/ping/{token}` 会变回 UP 并发送恢复通知。

## 五、REST API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/ping/{token}` | 成功心跳，置 UP |
| GET/POST | `/ping/{token}/start` | 开始（记录日志） |
| GET/POST | `/ping/{token}/fail` | 失败，置 DOWN 并告警 |
| POST | `/api/checks` | 创建检查项 |
| GET | `/api/checks` | 列表 |
| GET | `/api/checks/{id}` | 详情 |
| PUT | `/api/checks/{id}` | 更新 |
| DELETE | `/api/checks/{id}` | 删除 |
| POST | `/api/checks/{id}/pause` | 暂停 |
| POST | `/api/checks/{id}/resume` | 恢复 |
| GET | `/` | 状态页 |

## 六、如何接入 Spring Boot `@Scheduled`

在你**被监控的应用**里，任务跑完后请求一次 ping URL 即可：

```java
@Component
public class BackupJob {

    private static final String PING_URL = "http://你的cron-sentinel地址:8080/ping/你的token";
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 点
    public void runBackup() {
        try {
            // ... 你的实际备份逻辑 ...
            doBackup();
            // 成功后报平安
            restTemplate.getForObject(PING_URL, String.class);
        } catch (Exception e) {
            // 失败上报，立即告警
            restTemplate.getForObject(PING_URL + "/fail", String.class);
        }
    }
}
```

接入 Linux crontab：

```bash
# 备份脚本成功后 curl 报平安
0 2 * * * /opt/backup.sh && curl -fsS http://你的地址:8080/ping/你的token
```

## 七、超时扫描逻辑

`TimeoutScanner` 每 **1 分钟** 执行一次：

1. 查出 `status ∈ {UP, NEW}` 的检查项（`PAUSED`/`DOWN` 不参与）
2. 计算最晚预期时间：`UP` 用 `next_expected_at`；`NEW` 用 `created_at + period + grace`
3. `now` 超过最晚预期时间 → 置 `DOWN` 并发送告警邮件（状态变化才发，避免重复轰炸）
4. `DOWN` 的检查项重新收到 ping → 变回 `UP`，发送恢复通知

## 八、目录结构

```
src/main/java/com/cronsentinel
├── CronSentinelApplication.java   启动类(@EnableScheduling, @MapperScan)
├── common/                        状态/类型常量
├── controller/                    PingController / CheckController / 状态页 / 异常处理
├── dto/                           请求与响应对象
├── entity/                        CheckItem / PingLog (MyBatis-Plus 实体)
├── mapper/                        BaseMapper 子接口
└── service/                       Ping / Check / Alert / TimeoutScanner
src/main/resources
├── application.yml                完整配置示例
├── schema.sql                     建表脚本
└── templates/index.html           Thymeleaf 状态页
```
