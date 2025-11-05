# Docker 配置完整指南

## 📦 Docker 架构概览

本项目采用完整的微服务架构，包含以下服务：

### 核心服务
1. **MySQL 8.0** - 主数据库
2. **Redis 7** - 缓存层（验证码、会话等）
3. **RabbitMQ 3.12** - 消息队列
4. **Elasticsearch 8.11** - 搜索引擎
5. **MinIO** - 对象存储
6. **Spring Boot Backend** - 后端应用
7. **Vue.js Frontend** - 前端应用（Nginx）

---

## 📁 Dockerfile 详解

### 1. 后端 Dockerfile (`/Dockerfile`)

```dockerfile
# 多阶段构建优化（已优化为 Amazon Corretto）
Stage 1: Maven + Amazon Corretto JDK 17 (构建)
Stage 2: Amazon Corretto JRE 17 Alpine (运行)

特性：
✅ Maven 依赖缓存优化
✅ 非 root 用户运行（安全）
✅ JVM 容器化参数优化
✅ 健康检查配置
✅ 最小化镜像体积
✅ 更稳定的 Amazon Corretto 镜像
```

**关键配置：**
- 基础镜像：`amazoncorretto:17-alpine` (已优化，更稳定)
- 暴露端口：8080
- 健康检查：`/actuator/health`
- JVM 参数：容器感知，最大 RAM 75%
- 用户：非root用户 `appuser`

### 2. 前端 Dockerfile (`/frontend/Dockerfile`)

```dockerfile
# 多阶段构建
Stage 1: Node 18 Alpine (构建 Vue.js)
Stage 2: Nginx Alpine (静态文件服务)

特性：
✅ npm ci 精确依赖安装
✅ 生产环境 Nginx 优化
✅ SPA 路由支持
✅ API 反向代理配置
✅ WebSocket 支持
```

**关键配置：**
- 构建输出：`/app/dist`
- Nginx 配置：支持 Vue Router、API 代理、WebSocket
- 暴露端口：80

---

## 🔧 compose.yaml 配置

### 已移除问题
- ❌ 移除了过时的 `version: '3.8'` 字段
## 🔧 compose.yaml 配置

### 已移除问题
- ❌ 移除了过时的 `version: '3.8'` 字段
- ✅ 使用现代 Docker Compose 规范

### 服务依赖关系

```
frontend (80)
    ↓
backend (8080)
    ↓
├── mysql (3306)
├── redis (6379)
├── rabbitmq (5672, 15672)
├── elasticsearch (9200, 9300)
└── minio (9000, 9001)
```

### 健康检查机制

所有服务都配置了健康检查，确保依赖服务完全就绪后再启动：

| 服务 | 健康检查命令 | 间隔 | 重试 |
|------|-------------|------|------|
| MySQL | `mysqladmin ping` | 默认 | 10次 |
| Redis | `redis-cli ping` | 5秒 | 5次 |
| RabbitMQ | `rabbitmq-diagnostics ping` | 10秒 | 5次 |
| Elasticsearch | `curl /_cluster/health` | 30秒 | 5次 |
| MinIO | `curl /minio/health/live` | 30秒 | 3次 |
| Backend | `curl /actuator/health` | 30秒 | 3次 |

---

## 🚀 快速启动指南

### 方法 1：使用一键启动脚本（推荐）⭐

```bash
# 使用修复后的完整启动脚本
cd /Users/Kiyu/IdeaProjects/temaple/c2csectrade
./scripts/start-fixed.sh
```

这个脚本会自动：
1. ✅ 检查并安装前端依赖
2. ✅ 构建后端 Maven 项目
3. ✅ 构建前端 Vue.js 应用
4. ✅ 停止现有容器
5. ✅ 启动所有 Docker 服务
6. ✅ 显示服务状态和访问地址

### 方法 2：使用 Makefile

```bash
# 一键启动
make up

# 停止服务
make down

# 查看状态
make ps

# 查看日志
make logs
```

### 方法 3：手动启动

#### 3.1 构建并启动所有服务

```bash
# 构建镜像
docker compose build

# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f
```

#### 3.2 单独启动某个服务

```bash
# 只启动后端及其依赖
docker compose up -d backend

# 只启动前端
docker compose up -d frontend
```

#### 3.3 扩展后端实例（负载均衡）

```bash
# 启动 3 个后端实例
docker compose up -d --scale backend=3
```

注意：需要在 compose.yaml 中移除 `container_name` 并配置负载均衡器

---

## 🔍 端口映射

| 服务 | 内部端口 | 外部端口 | 说明 |
|------|---------|---------|------|
| Frontend | 80 | 80 | Web 界面 |
| Backend | 8080 | 8080 | REST API |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| RabbitMQ | 5672 | 5672 | AMQP |
| RabbitMQ UI | 15672 | 15672 | 管理界面 |
| Elasticsearch | 9200 | 9200 | REST API |
| Elasticsearch | 9300 | 9300 | 节点通信 |
| MinIO | 9000 | 9000 | S3 API |
| MinIO UI | 9001 | 9001 | 管理界面 |

---

## 🌐 访问地址

- **前端应用**: http://localhost
- **后端 API**: http://localhost:8080
- **RabbitMQ 管理**: http://localhost:15672 (admin/admin123)
- **MinIO 控制台**: http://localhost:9001 (minioadmin/minioadmin)
- **Elasticsearch**: http://localhost:9200

---

## 📊 数据持久化

所有数据通过 Docker Volumes 持久化：

```yaml
volumes:
  mysql-data         # MySQL 数据库文件
  redis-data         # Redis AOF 持久化文件
  rabbitmq-data      # RabbitMQ 消息队列数据
  elasticsearch-data # Elasticsearch 索引数据
```

MinIO 使用绑定挂载：`./minio/data`

---

## 🔧 环境变量配置

### Backend 环境变量

```yaml
SPRING_PROFILES_ACTIVE: docker
# 修复后的MySQL连接字符串（解决utf8mb4编码问题）
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/trade?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&useServerPrepStmts=true&cachePrepStmts=true&rewriteBatchedStatements=true
SPRING_DATASOURCE_USERNAME: tradeuser
SPRING_DATASOURCE_PASSWORD: tradepass
SPRING_DATA_REDIS_HOST: redis
SPRING_DATA_REDIS_PORT: 6379
SPRING_RABBITMQ_HOST: rabbitmq
SPRING_RABBITMQ_PORT: 5672
SPRING_RABBITMQ_USERNAME: admin
SPRING_RABBITMQ_PASSWORD: admin123
SPRING_ELASTICSEARCH_URIS: http://elasticsearch:9200
MINIO_ENDPOINT: http://minio:9000
MINIO_ACCESS_KEY: minioadmin
MINIO_SECRET_KEY: minioadmin
```

### 服务间通信

所有服务在 `c2csectrade-network` 桥接网络中通信，使用服务名作为主机名。

---

## 🛠️ 常用命令

### 查看服务状态
```bash
docker compose ps
```

### 查看实时日志
```bash
docker compose logs -f backend
docker compose logs -f frontend
```

### 重启服务
```bash
docker compose restart backend
```

### 停止并删除所有容器
```bash
docker compose down
```

### 停止并删除包括数据卷
```bash
docker compose down -v
```

### 进入容器调试
```bash
docker compose exec backend sh
docker compose exec mysql mysql -utradeuser -ptradepass trade
docker compose exec redis redis-cli
```

---

## 🐛 故障排查

### 1. MySQL 字符编码错误 ⭐（已修复）

**问题**: `Unsupported character encoding 'utf8mb4'`

**原因**: MySQL Connector/J 8.0.33 驱动与字符集配置的兼容性问题

**解决方案**:
已在 `application.properties` 和 `compose.yaml` 中修复：
```properties
# 使用 characterEncoding=UTF-8 替代 useUnicode=true
spring.datasource.url=jdbc:mysql://mysql:3306/trade?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&useServerPrepStmts=true&cachePrepStmts=true&rewriteBatchedStatements=true
```

### 2. 验证码不显示或不刷新

**问题**: 前端验证码图片无法加载

**可能原因**:
1. Redis 服务未启动或连接失败
2. 后端服务未完全启动
3. CORS 配置问题
4. 浏览器缓存问题

**解决步骤**:
```bash
# 1. 检查 Redis 服务
docker compose ps redis
docker compose logs redis

# 2. 测试 Redis 连接
docker compose exec redis redis-cli ping
# 应该返回: PONG

# 3. 检查后端日志
docker compose logs backend | grep -i "redis\|captcha"

# 4. 手动测试验证码API
curl -i http://localhost:8080/api/captcha/generate
# 检查响应头中是否有 Captcha-ID

# 5. 清理浏览器缓存并强制刷新 (Ctrl+Shift+R / Cmd+Shift+R)
```

**验证码相关配置**:
- 验证码过期时间: 300秒（5分钟）
- 存储位置: Redis (key: captcha:{captchaId})
- 验证码长度: 4位数字+字母

### 3. 后端无法连接数据库

**问题**: `Connection refused` 或 `Unknown database`

**解决**:
```bash
# 检查 MySQL 健康状态
docker compose ps mysql

# 查看 MySQL 日志
docker compose logs mysql

# 手动初始化数据库
docker compose exec mysql mysql -uroot -prootpassword < init.sql

# 测试数据库连接
docker compose exec mysql mysql -utradeuser -ptradepass -e "SELECT 1"
```

### 4. 前端无法访问后端 API

**问题**: `502 Bad Gateway`

**解决**:
```bash
# 检查 nginx.conf 中的代理地址是否为 http://backend:8080
docker compose exec frontend cat /etc/nginx/conf.d/default.conf

# 确认后端服务健康
curl http://localhost:8080/actuator/health

# 查看 Nginx 日志
docker compose logs frontend
```

### 5. Redis 连接失败

**问题**: `Unable to connect to Redis`

**解决**:
```bash
# 测试 Redis 连接
docker compose exec redis redis-cli ping

# 检查后端配置
docker compose exec backend env | grep REDIS

# 查看 Redis 日志
docker compose logs redis

# 重启 Redis 服务
docker compose restart redis
```

### 6. Docker 镜像拉取失败 ⭐（已修复）

**问题**: `eclipse-temurin:17-jre-jammy: not found`

**解决方案**:
已将基础镜像更换为更稳定的 Amazon Corretto:
```dockerfile
FROM amazoncorretto:17-alpine
```

### 7. 构建失败

**问题**: Maven 依赖下载失败

**解决**:
```bash
# 清理并重新构建
docker compose build --no-cache backend

# 清理 Maven 缓存
rm -rf ~/.m2/repository

# 使用国内 Maven 镜像（修改 pom.xml）
```

### 8. 端口被占用

**问题**: `Bind for 0.0.0.0:6379 failed: port is already allocated`

**解决**:
```bash
# 查找占用端口的进程
lsof -i :6379  # macOS/Linux
netstat -ano | findstr :6379  # Windows

# 停止本地 Redis 服务
brew services stop redis  # macOS
sudo systemctl stop redis  # Linux

# 或修改 compose.yaml 中的端口映射
ports:
  - "6380:6379"  # 使用不同的外部端口
```

---

## 🎯 最佳实践

### 1. 开发环境
```bash
# 使用 docker-compose.override.yml 覆盖配置
# 挂载源码目录实现热重载
```

### 2. 生产环境
- 使用 Docker Swarm 或 Kubernetes
- 配置外部数据库和缓存
- 启用 TLS/SSL
- 配置资源限制（CPU、内存）
- 使用 secrets 管理敏感信息

### 3. 性能优化
```yaml
# 为服务配置资源限制
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 2G
    reservations:
      cpus: '1'
      memory: 1G
```

---

## 📝 版本信息

- **Docker Compose**: 无需 version 字段（现代规范）
- **MySQL**: 8.0
- **Redis**: 7-alpine
- **RabbitMQ**: 3.12-management-alpine
- **Elasticsearch**: 8.11.0
- **MinIO**: latest
- **Java**: Eclipse Temurin 17
- **Node.js**: 18-alpine
- **Nginx**: alpine

---

## 🔐 安全建议

1. **修改默认密码**
   - MySQL root: `rootpassword`
   - MySQL user: `tradepass`
   - RabbitMQ: `admin123`
   - MinIO: `minioadmin`

2. **使用 Docker Secrets**
   ```yaml
   secrets:
     db_password:
       file: ./secrets/db_password.txt
   ```

3. **限制端口暴露**
   ```yaml
   # 仅在内部网络暴露
   expose:
     - "8080"
   # 不使用 ports
   ```

4. **启用网络隔离**
   ```yaml
   networks:
     frontend-network:
     backend-network:
   ```

---

## 📚 相关文档

- [部署与使用指南](./部署与使用指南.md)
- [项目概述](./项目概述.md)
- [分布式系统架构文档](./分布式系统架构文档.md)

