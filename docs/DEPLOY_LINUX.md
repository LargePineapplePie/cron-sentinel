# Cron Sentinel 服务端 · Linux 部署文档

本文档介绍在 Linux 服务器上**直接用 JDK 运行**（非 Docker）部署 Cron Sentinel 服务端。
Docker 部署见 [DEPLOY_DOCKER.md](./DEPLOY_DOCKER.md)。

> 服务默认端口 **8099**（见 `application.yml` 的 `server.port`）。

---

## 一、环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 运行 jar 需要 |
| MySQL | 8.x | 存储检查项与心跳记录 |
| Maven | 3.6+ | 仅"在服务器上构建"时需要；也可在本地打好 jar 上传 |

检查 JDK：

```bash
java -version   # 需为 17+
```

CentOS/RHEL 安装 JDK 17（示例）：

```bash
sudo yum install -y java-17-openjdk
```

Ubuntu/Debian：

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk
```

---

## 二、准备数据库

1. 登录 MySQL：

```bash
mysql -u root -p
```

2. 执行项目自带的建表脚本（已包含 `CREATE DATABASE cron_sentinel`）：

```bash
mysql -u root -p < /opt/cron-sentinel/schema.sql
```

> `schema.sql` 在源码 `src/main/resources/schema.sql`，部署时把它一并传到服务器。

3.（推荐）为应用创建专用数据库账号，避免直接用 root：

```sql
CREATE USER 'cron'@'%' IDENTIFIED BY '强密码';
GRANT ALL PRIVILEGES ON cron_sentinel.* TO 'cron'@'%';
FLUSH PRIVILEGES;
```

---

## 三、获取可运行的 jar

### 方式 A：本地打包后上传（推荐，服务器无需装 Maven）

在开发机执行：

```bash
mvn clean package -DskipTests
# 产物：target/cron-sentinel-0.0.1-SNAPSHOT.jar
```

上传到服务器：

```bash
scp target/cron-sentinel-0.0.1-SNAPSHOT.jar user@server:/opt/cron-sentinel/app.jar
scp src/main/resources/schema.sql user@server:/opt/cron-sentinel/schema.sql
```

### 方式 B：在服务器上构建

```bash
git clone <你的仓库地址> && cd cron-sentinel
mvn clean package -DskipTests
cp target/cron-sentinel-0.0.1-SNAPSHOT.jar /opt/cron-sentinel/app.jar
```

---

## 四、配置（用环境变量，不改 jar）

应用支持用环境变量覆盖配置，**敏感信息不要写进代码/仓库**：

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | 数据库 JDBC URL | 指向 `localhost:3306/cron_sentinel` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `123456` |
| `MAIL_ENABLED` | 是否真正发邮件 | `true` |
| `MAIL_HOST` | SMTP 服务器 | `smtp.qq.com` |
| `MAIL_PORT` | SMTP 端口 | `465` |
| `MAIL_USERNAME` | 发件邮箱 | - |
| `MAIL_PASSWORD` | SMTP 授权码（非登录密码）| - |
| `SERVER_PORT` | 服务端口 | `8099` |

把它们集中写到一个 env 文件 `/opt/cron-sentinel/cron-sentinel.env`：

```bash
DB_URL=jdbc:mysql://localhost:3306/cron_sentinel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=cron
DB_PASSWORD=强密码
MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=你的SMTP授权码
```

> 保护好它：`chmod 600 /opt/cron-sentinel/cron-sentinel.env`

---

## 五、启动方式

### 方式 A：快速试跑（前台）

```bash
cd /opt/cron-sentinel
set -a; source cron-sentinel.env; set +a
java -jar app.jar
```

访问 `http://服务器IP:8099/` 验证。`Ctrl+C` 退出。

### 方式 B：后台运行（nohup，简单）

```bash
cd /opt/cron-sentinel
set -a; source cron-sentinel.env; set +a
nohup java -jar app.jar > app.log 2>&1 &
```

查看日志：`tail -f /opt/cron-sentinel/app.log`

### 方式 C：systemd 托管（推荐，开机自启 + 崩溃自动重启）

创建 `/etc/systemd/system/cron-sentinel.service`：

```ini
[Unit]
Description=Cron Sentinel Server
After=network.target mysql.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/cron-sentinel
EnvironmentFile=/opt/cron-sentinel/cron-sentinel.env
ExecStart=/usr/bin/java -jar /opt/cron-sentinel/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启用并启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable cron-sentinel
sudo systemctl start cron-sentinel

# 查看状态与日志
sudo systemctl status cron-sentinel
sudo journalctl -u cron-sentinel -f
```

---

## 六、开放端口 / 反向代理

### 直接放行端口

```bash
# firewalld (CentOS)
sudo firewall-cmd --permanent --add-port=8099/tcp && sudo firewall-cmd --reload
# ufw (Ubuntu)
sudo ufw allow 8099/tcp
```

### （推荐）用 Nginx 反代到 80/443

`/etc/nginx/conf.d/cron-sentinel.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8099;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo nginx -t && sudo systemctl reload nginx
```

> 应用会优先读取 `X-Forwarded-For` / `X-Real-IP` 记录心跳来源 IP，所以反代后日志里的 IP 仍是真实客户端 IP。

---

## 七、验证完整流程

```bash
# 1. 打开状态页
curl http://127.0.0.1:8099/

# 2. 创建检查项（拿到 token）
curl -X POST http://127.0.0.1:8099/api/checks \
  -H "Content-Type: application/json" \
  -d '{"name":"测试任务","periodSeconds":60,"graceSeconds":30,"alertEmail":"you@example.com"}'

# 3. 报平安（状态变 UP）
curl http://127.0.0.1:8099/ping/<上一步返回的token>
```

停止 ping，等 `周期+宽限` 超时后，每分钟的扫描器会把它置为 DOWN 并发告警邮件。

---

## 八、升级 / 回滚

```bash
# 升级：上传新 jar 覆盖后重启
cp app.jar app.jar.bak            # 备份旧版本
cp new-app.jar app.jar            # 替换
sudo systemctl restart cron-sentinel

# 回滚
cp app.jar.bak app.jar
sudo systemctl restart cron-sentinel
```

---

## 九、常见问题

- **启动报连接数据库失败**：检查 `DB_URL/DB_USERNAME/DB_PASSWORD` 是否正确、MySQL 是否允许该账号远程登录、`cron_sentinel` 库与表是否已建（执行过 `schema.sql`）。
- **收不到告警邮件**：确认 `MAIL_ENABLED=true`，`MAIL_PASSWORD` 填的是 SMTP **授权码**而非登录密码；查看应用日志里邮件发送的报错。本地调试可先设 `MAIL_ENABLED=false` 跑通主流程。
- **端口冲突**：用 `SERVER_PORT=xxxx` 改端口，并同步改防火墙/反代配置。
- **时间不对/超时判断异常**：确认服务器时区为 `Asia/Shanghai`（`timedatectl set-timezone Asia/Shanghai`），JDBC URL 里也带了 `serverTimezone=Asia/Shanghai`。
