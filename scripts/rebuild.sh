#!/bin/bash

# 清理并重新编译项目的脚本

echo "🧹 清理 Maven 缓存..."
mvn clean

echo "📦 重新编译项目（跳过测试）..."
mvn compile -DskipTests

echo "✅ 编译完成！"
echo ""
echo "现在可以运行以下命令启动项目："
echo "  mvn spring-boot:run"

