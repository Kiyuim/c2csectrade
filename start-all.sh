#!/bin/bash

# 二手物品交易平台启动脚本
# 此脚本将启动所有必需的服务：MySQL, Redis, MinIO, Elasticsearch, 后端和前端

echo "=========================================="
echo "  二手物品交易平台 - 启动脚本"
echo "=========================================="

# 设置 Java 17 环境（避免 Java 25 的兼容性问题）
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
echo "✓ 使用 Java 17: $JAVA_HOME"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker Desktop"
    exit 1
fi
echo "✓ Docker 已运行"

# 停止并清理旧容器
echo ""
echo "正在清理旧容器..."
docker-compose down

# 启动所有服务
echo ""
echo "正在启动服务容器（MySQL, Redis, MinIO, Elasticsearch）..."
docker-compose up -d mysql-db redis-cache minio elasticsearch

# 等待数据库和 Elasticsearch 启动
echo ""
echo "等待服务启动（30秒）..."
sleep 30

# 检查服务状态
echo ""
echo "检查服务状态..."
docker-compose ps

# 构建并启动后端
echo ""
echo "=========================================="
echo "正在构建并启动后端服务..."
echo "=========================================="
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "✓ 后端构建成功"
    echo "正在启动 Spring Boot 应用..."
    JAVA_HOME=$(/usr/libexec/java_home -v 17) java -jar target/c2c-platform-0.0.1-SNAPSHOT.jar &
    BACKEND_PID=$!
    echo "✓ 后端已启动 (PID: $BACKEND_PID)"
else
    echo "❌ 后端构建失败"
    exit 1
fi

# 等待后端启动
echo "等待后端完全启动（15秒）..."
sleep 15

# 启动前端开发服务器
echo ""
echo "=========================================="
echo "正在启动前端开发服务器..."
echo "=========================================="
cd frontend
npm run serve &
FRONTEND_PID=$!
echo "✓ 前端已启动 (PID: $FRONTEND_PID)"

# 显示访问信息
echo ""
echo "=========================================="
echo "  ✅ 所有服务已启动！"
echo "=========================================="
echo ""
echo "📋 服务访问地址："
echo "  - 前端应用:        http://localhost:8081"
echo "  - 后端API:         http://localhost:8080"
echo "  - MinIO控制台:     http://localhost:9001"
echo "  - Elasticsearch:   http://localhost:9200"
echo ""
echo "📋 默认账户："
echo "  - 管理员: admin / admin123"
echo "  - MinIO:  minioadmin / minioadmin"
echo ""
echo "📋 进程ID："
echo "  - 后端 PID: $BACKEND_PID"
echo "  - 前端 PID: $FRONTEND_PID"
echo ""
echo "🛑 停止服务："
echo "  - 按 Ctrl+C 停止此脚本"
echo "  - 或运行: docker-compose down"
echo "  - 或运行: kill $BACKEND_PID $FRONTEND_PID"
echo ""
echo "=========================================="

# 等待用户中断
wait

