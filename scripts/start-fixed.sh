#!/bin/bash
# 完整的系统启动脚本 - 修复所有已知问题

set -e

echo "======================================"
echo "RebookTrade 完整启动脚本"
echo "======================================"

# 1. 检查并安装前端依赖
echo ""
echo "[1/5] 检查前端依赖..."
cd frontend
if [ ! -d "node_modules" ] || [ ! -f "node_modules/@stomp/stompjs/package.json" ]; then
    echo "安装前端依赖..."
    npm install
else
    echo "✓ 前端依赖已安装"
fi
cd ..

# 2. 构建后端
echo ""
echo "[2/5] 构建后端应用..."
mvn clean package -DskipTests

# 3. 构建前端
echo ""
echo "[3/5] 构建前端应用..."
cd frontend
npm run build
cd ..

# 4. 停止现有容器
echo ""
echo "[4/5] 停止现有Docker容器..."
docker compose down || true

# 5. 启动所有服务
echo ""
echo "[5/5] 启动所有服务..."
docker compose up -d --build

# 6. 等待服务启动
echo ""
echo "等待服务启动完成..."
sleep 10

# 7. 检查服务状态
echo ""
echo "======================================"
echo "服务状态："
echo "======================================"
docker compose ps

echo ""
echo "======================================"
echo "系统启动完成！"
echo "======================================"
echo ""
echo "访问地址："
echo "  前端: http://localhost"
echo "  后端API: http://localhost:8080"
echo "  RabbitMQ管理界面: http://localhost:15672 (admin/admin123)"
echo "  MinIO控制台: http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "默认管理员账户："
echo "  用户名: admin"
echo "  密码: admin123"
echo ""
echo "查看日志："
echo "  docker compose logs -f backend"
echo "  docker compose logs -f frontend"
echo ""

