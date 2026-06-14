# 注意：禁止使用 # syntax=docker/dockerfile:xxx 语法
# 使用 Docker 原生分层缓存，确保 pom.xml 无变更时不重新下载 Maven 依赖

# ============================================================
# Stage 1: 依赖层（仅 pom.xml / settings.xml 变更时触发）
# ============================================================
ARG DOCKER_REGISTRY
FROM ${DOCKER_REGISTRY}/maven:3.9.6-eclipse-temurin-17 AS deps

WORKDIR /app

# 1. 首先拷贝 settings.xml（腾讯云镜像），保证即便 pom.xml 没变、
#    settings 更新时也能正确切换源
COPY settings.xml /usr/share/maven/ref/settings-docker.xml
COPY settings.xml /root/.m2/settings.xml

# 2. 仅拷贝 pom.xml，触发 Docker 分层缓存断点
#    只要 pom.xml 内容不变，这一层和依赖预取层都会命中缓存
COPY pom.xml ./

# 3. 预下载业务依赖，避免 go-offline 拉取 Maven site/reporting 的大依赖链
RUN mvn -f /app/pom.xml \
        -s /root/.m2/settings.xml \
        dependency:resolve \
        -B \
        -Dmaven.repo.local=/root/.m2/repository \
        -DdownloadSources=false \
        -DdownloadJavadocs=false

# ============================================================
# Stage 2: 构建层（仅源文件变更时触发，复用 deps 层 ~/.m2 仓库）
# ============================================================
FROM deps AS builder

WORKDIR /app

# 4. 依赖层已准备好本地仓库，此时只拷贝 src
#    —— 仅改业务代码时，前三个 layer 全部命中缓存
COPY src ./src
COPY settings.xml /root/.m2/settings.xml

# 5. 执行编译打包（跳过测试），仅做源码编译动作
#    此时 Maven 几乎不会再联网下载任何 jar，速度极快
RUN mvn -f /app/pom.xml \
        -s /root/.m2/settings.xml \
        clean package -DskipTests \
        -B \
        -Dmaven.repo.local=/root/.m2/repository \
        -Dmaven.test.skip=true \
        -Dcheckstyle.skip=true \
        -Dspotbugs.skip=true \
        -Dpmd.skip=true

# ============================================================
# Stage 3: 运行层（JRE，仅拷贝打包好的 jar）
# ============================================================
FROM ${DOCKER_REGISTRY}/eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# 6. Alpine 上补全 curl/wget 与时区，便于 healthcheck
RUN apk add --no-cache curl wget tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 7. 从构建层拷贝唯一 jar，命名为 app.jar，体积 ~ 80-120MB
COPY --from=builder /app/target/instrument-accessory-manager.jar /app/app.jar

# 8. 容器内 SpringBoot 统一监听 8080，宿主机映射由 docker-compose 管理
EXPOSE 8080

# 9. 健康检查（通过 SpringBoot 接口检测）
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/dashboard/stats >/dev/null 2>&1 || exit 1

# 10. JVM 调优参数 + 以 prod profile 启动
ENTRYPOINT ["java", \
            "-server", \
            "-Xms256m", "-Xmx512m", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=200", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dfile.encoding=UTF-8", \
            "-Duser.timezone=Asia/Shanghai", \
            "-Dspring.profiles.active=prod", \
            "-jar", "/app/app.jar"]
