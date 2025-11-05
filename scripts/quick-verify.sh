#!/bin/bash
echo "等待容器启动完成..."
sleep 30

echo -e "\n=== 测试验证码接口 ==="
curl -v http://localhost:8080/api/captcha/generate 2>&1 | head -20

echo -e "\n\n=== 后端日志 ==="
docker logs c2csectrade-backend --tail 30 2>&1 | tail -20

