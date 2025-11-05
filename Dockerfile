# 多阶段构建 Dockerfile for RebookTrade Backend

# Stage 1: Build stage
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (利用Docker缓存)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM amazoncorretto:17-alpine
WORKDIR /app

# 安装curl用于健康检查
RUN apk add --no-cache curl font-wqy-zenhei

# 创建非root用户
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

# 从构建阶段复制jar包
COPY --from=build /app/target/*.jar app.jar

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]

