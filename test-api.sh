#!/bin/bash

# API测试脚本 - 验证所有功能是否正常

echo "=========================================="
echo "  API功能测试脚本"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080/api"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_endpoint() {
    local name=$1
    local url=$2
    local method=$3
    local expected_status=$4

    echo -n "测试 $name ... "

    if [ "$method" == "GET" ]; then
        status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    else
        status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url")
    fi

    if [ "$status" == "$expected_status" ]; then
        echo -e "${GREEN}✓ 成功${NC} (HTTP $status)"
    else
        echo -e "${RED}✗ 失败${NC} (期望 $expected_status, 实际 $status)"
    fi
}

echo "1. 测试基础服务..."
echo "----------------------------"
test_endpoint "健康检查" "$BASE_URL/../actuator/health" "GET" "200"
test_endpoint "获取验证码" "$BASE_URL/captcha/image" "GET" "200"

echo ""
echo "2. 测试商品API..."
echo "----------------------------"
test_endpoint "获取商品列表" "$BASE_URL/products" "GET" "200"
test_endpoint "搜索商品" "$BASE_URL/products/search" "GET" "200"

echo ""
echo "3. 测试外部服务..."
echo "----------------------------"
echo -n "测试 MySQL ... "
if docker exec c2c_mysql mysql -uroot -pQsycl741 -e "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 连接成功${NC}"
else
    echo -e "${RED}✗ 连接失败${NC}"
fi

echo -n "测试 Redis ... "
if docker exec c2c_redis redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 连接成功${NC}"
else
    echo -e "${RED}✗ 连接失败${NC}"
fi

echo -n "测试 MinIO ... "
minio_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9000/minio/health/live")
if [ "$minio_status" == "200" ]; then
    echo -e "${GREEN}✓ 运行正常${NC}"
else
    echo -e "${RED}✗ 连接失败${NC}"
fi

echo -n "测试 Elasticsearch ... "
es_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9200")
if [ "$es_status" == "200" ]; then
    echo -e "${GREEN}✓ 运行正常${NC}"
    # 检查索引
    echo -n "  - 检查products索引 ... "
    index_exists=$(curl -s "http://localhost:9200/products" | grep -o "products")
    if [ -n "$index_exists" ]; then
        echo -e "${GREEN}✓ 索引存在${NC}"
    else
        echo -e "${YELLOW}⚠ 索引不存在（首次运行正常）${NC}"
    fi
else
    echo -e "${RED}✗ 连接失败${NC}"
fi

echo ""
echo "4. 测试数据库表..."
echo "----------------------------"
echo -n "测试 users 表 ... "
if docker exec c2c_mysql mysql -uroot -pQsycl741 trade -e "SELECT COUNT(*) FROM users" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 表存在${NC}"
else
    echo -e "${RED}✗ 表不存在${NC}"
fi

echo -n "测试 pms_product 表 ... "
if docker exec c2c_mysql mysql -uroot -pQsycl741 trade -e "SELECT COUNT(*) FROM pms_product" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 表存在${NC}"
else
    echo -e "${RED}✗ 表不存在${NC}"
fi

echo -n "测试 pms_product_media 表 ... "
if docker exec c2c_mysql mysql -uroot -pQsycl741 trade -e "SELECT COUNT(*) FROM pms_product_media" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 表存在${NC}"
else
    echo -e "${RED}✗ 表不存在${NC}"
fi

echo ""
echo "5. 前端服务..."
echo "----------------------------"
echo -n "测试前端 ... "
frontend_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8081")
if [ "$frontend_status" == "200" ]; then
    echo -e "${GREEN}✓ 运行正常${NC}"
else
    echo -e "${YELLOW}⚠ 未运行或未完全启动${NC}"
fi

echo ""
echo "=========================================="
echo "  测试完成！"
echo "=========================================="
echo ""
echo "📋 快速访问链接："
echo "  - 前端: http://localhost:8081"
echo "  - 后端: http://localhost:8080"
echo "  - MinIO: http://localhost:9001"
echo ""
echo "👤 默认账户: admin / admin123"
echo ""

