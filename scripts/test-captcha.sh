#!/bin/bash
# 验证码接口诊断脚本

echo "=========================================="
echo "验证码接口诊断工具"
echo "=========================================="
echo ""

echo "1. 检查容器状态..."
docker ps --filter name=c2csectrade --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -10

echo -e "\n2. 测试后端验证码接口..."
echo "请求: http://localhost:8080/api/captcha/generate"
RESPONSE=$(curl -s -o /tmp/captcha_direct.png -w "%{http_code}|%{content_type}|%{size_download}" http://localhost:8080/api/captcha/generate)
HTTP_CODE=$(echo $RESPONSE | cut -d'|' -f1)
CONTENT_TYPE=$(echo $RESPONSE | cut -d'|' -f2)
SIZE=$(echo $RESPONSE | cut -d'|' -f3)

echo "HTTP状态码: $HTTP_CODE"
echo "Content-Type: $CONTENT_TYPE"
echo "文件大小: $SIZE bytes"
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ 后端接口正常"
    file /tmp/captcha_direct.png
else
    echo "❌ 后端接口异常，查看返回内容:"
    cat /tmp/captcha_direct.png
fi

echo -e "\n3. 测试通过 Nginx 代理的验证码接口..."
echo "请求: http://localhost/api/captcha/generate"
RESPONSE2=$(curl -s -o /tmp/captcha_nginx.png -w "%{http_code}|%{content_type}|%{size_download}" http://localhost/api/captcha/generate)
HTTP_CODE2=$(echo $RESPONSE2 | cut -d'|' -f1)
CONTENT_TYPE2=$(echo $RESPONSE2 | cut -d'|' -f2)
SIZE2=$(echo $RESPONSE2 | cut -d'|' -f3)

echo "HTTP状态码: $HTTP_CODE2"
echo "Content-Type: $CONTENT_TYPE2"
echo "文件大小: $SIZE2 bytes"
if [ "$HTTP_CODE2" = "200" ]; then
    echo "✅ Nginx 代理正常"
    file /tmp/captcha_nginx.png
else
    echo "❌ Nginx 代理异常，查看返回内容:"
    cat /tmp/captcha_nginx.png
fi

echo -e "\n4. 查看最近的后端日志（最后20行）..."
docker logs c2csectrade-backend --tail 20 2>&1 | tail -20

echo -e "\n5. 查看最近的前端 Nginx 日志（最后10行）..."
docker logs c2csectrade-frontend --tail 10 2>&1 | tail -10

echo -e "\n=========================================="
echo "诊断完成"
echo "=========================================="

