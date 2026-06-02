# ============================================================
# Cron Sentinel 服务端 Dockerfile（多阶段构建）
# 阶段一：用 Maven + JDK17 编译打包；阶段二：用精简 JRE 运行
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷贝 pom 并预下载依赖，利用 Docker 层缓存（pom 不变时跳过重新下载）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码并打包（跳过测试以加快镜像构建）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区设为上海，保证日志/时间显示与业务一致
ENV TZ=Asia/Shanghai

# 拷贝构建产物（jar 名带版本号，用通配匹配）
COPY --from=build /build/target/cron-sentinel-*.jar app.jar

# 容器内应用监听端口（与 application.yml 的 server.port 对应）
EXPOSE 8099

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
