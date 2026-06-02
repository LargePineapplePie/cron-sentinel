# Cron Sentinel 服务端 · Docker 部署文档

本文档介绍用 Docker 部署 Cron Sentinel 服务端。直接用 JDK 部署见 [DEPLOY_LINUX.md](./DEPLOY_LINUX.md)。

> 服务默认端口 **8099**。项目根目录已提供 `Dockerfile`、`docker-compose.yml`、`.dockerignore`。

---

## 一、环境要求

- Docker 20+
- Docker Compose v2（`docker compose` 命令；老版本是 `docker-compose`）

检查：

```bash
docker -v
docker compose version
```

---

## 二、方式 A：一键起 App + MySQL（推荐）

`docker-compose.yml` 已编排好两个服务：
- `mysql`：MySQL 8，首次启动**自动执行 `schema.sql` 建库建表**，数据持久化到命名卷 `mysql-data`
- `app`：本项目，自动多阶段构建（Maven 编译 → JRE 运行），通过内网连接 `mysql`

### 1. 配置（可选）

默认开箱即用（不发邮件）。如需自定义，在项目根目录建一个 `.env` 文件：

```bash
# 数据库 root 密码
MYSQL_ROOT_PASSWORD=请改成强密码

# 邮件（要真正发告警时设置）
MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=你的SMTP授权码
```

> compose 会自动读取同目录的 `.env`。不建此文件则使用 `docker-compose.yml` 里的默认值（`MAIL_ENABLED=false`，仅跑通流程不发信）。

### 2. 构建并启动

```bash
docker compose up -d --build
```

首次会拉取镜像 + 编译，耗时稍长。完成后：

```bash
docker compose ps          # 查看容器状态
docker compose logs -f app # 跟踪应用日志
```

### 3. 访问

浏览器打开 `http://服务器IP:8099/`。

### 4. 验证

```bash
# 创建检查项
curl -X POST http://127.0.0.1:8099/api/checks \
  -H "Content-Type: application/json" \
  -d '{"name":"测试任务","periodSeconds":60,"graceSeconds":30,"alertEmail":"you@example.com"}'

# 报平安
curl http://127.0.0.1:8099/ping/<返回的token>
```

### 5. 停止 / 重启 / 清理

```bash
docker compose stop            # 停止（保留数据）
docker compose start           # 再启动
docker compose down            # 停止并删除容器（数据卷 mysql-data 保留）
docker compose down -v         # 连数据卷一起删除（数据清空，慎用）
```

---

## 三、方式 B：只跑 App 容器，连接已有的外部 MySQL

如果你已有独立的 MySQL，只想容器化应用本身：

### 1. 先在你的 MySQL 上建库建表

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 2. 构建镜像

```bash
docker build -t cron-sentinel:latest .
```

### 3. 运行容器（用环境变量传配置）

```bash
docker run -d --name cron-sentinel \
  -p 8099:8099 \
  -e DB_URL="jdbc:mysql://数据库IP:3306/cron_sentinel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true" \
  -e DB_USERNAME=cron \
  -e DB_PASSWORD=强密码 \
  -e MAIL_ENABLED=true \
  -e MAIL_HOST=smtp.qq.com \
  -e MAIL_PORT=465 \
  -e MAIL_USERNAME=your_email@qq.com \
  -e MAIL_PASSWORD=你的SMTP授权码 \
  --restart unless-stopped \
  cron-sentinel:latest
```

> 注意：`DB_URL` 里的主机名要写 MySQL 实际可达的地址。容器里的 `localhost` 指的是容器自身，**不是宿主机**；连宿主机 MySQL 可用 `host.docker.internal`（部分 Linux 需加 `--add-host=host.docker.internal:host-gateway`）。

---

## 四、环境变量一览

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | 数据库 JDBC URL | compose 中指向 `mysql` 服务 |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `123456` |
| `MYSQL_ROOT_PASSWORD` | （仅方式A）MySQL root 密码 | `123456` |
| `MAIL_ENABLED` | 是否真正发邮件 | 方式A默认 `false` |
| `MAIL_HOST` / `MAIL_PORT` | SMTP 服务器 / 端口 | `smtp.qq.com` / `465` |
| `MAIL_USERNAME` | 发件邮箱 | - |
| `MAIL_PASSWORD` | SMTP 授权码 | - |
| `SERVER_PORT` | 应用端口 | `8099` |
| `TZ` | 时区 | `Asia/Shanghai` |

---

## 五、数据持久化

- 方式 A 的 MySQL 数据存放在命名卷 `mysql-data`，`docker compose down` 不会删除，升级应用不丢数据。
- 查看卷：`docker volume ls | grep mysql-data`
- 备份数据库：

```bash
docker exec cron-sentinel-mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" cron_sentinel' > backup.sql
```

---

## 六、升级应用

```bash
git pull                       # 拉取新代码
docker compose up -d --build   # 重新构建并滚动更新 app（mysql 数据保留）
```

---

## 七、常见问题

- **app 启动后连不上数据库**：方式 A 里 compose 已配置 app 等待 mysql 健康检查通过再启动；若仍失败，`docker compose logs mysql` 看 MySQL 是否就绪，确认 `DB_PASSWORD` 与 `MYSQL_ROOT_PASSWORD` 一致。
- **schema.sql 没生效**：MySQL 镜像**只在数据卷为空（首次初始化）时**执行 `/docker-entrypoint-initdb.d` 里的脚本。如果之前已初始化过，改了脚本需要 `docker compose down -v` 清空卷再重来（会丢数据）。
- **端口被占用**：改 `docker-compose.yml` 里 `ports` 左边的宿主端口，如 `"9000:8099"`。
- **收不到邮件**：确认 `MAIL_ENABLED=true` 且 `MAIL_PASSWORD` 为 SMTP 授权码；`docker compose logs -f app` 看发送报错。
- **构建慢**：首次构建要下载 Maven 依赖；`Dockerfile` 已用分层缓存，`pom.xml` 不变时后续构建会复用依赖层。
