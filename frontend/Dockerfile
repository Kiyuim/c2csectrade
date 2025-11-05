# 多阶段构建 Dockerfile for RebookTrade Frontend

# Stage 1: Build stage
FROM node:18-alpine AS build
WORKDIR /app

# 复制package文件并安装依赖
COPY package*.json ./
RUN npm ci

# 复制源代码并构建
COPY . .
RUN npm run build

# Stage 2: Production stage with Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# 安装curl用于健康检查
RUN apk add --no-cache curl

# 删除nginx默认页面
RUN rm -rf ./*

# 从构建阶段复制构建产物
COPY --from=build /app/dist .

# 复制nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查（使用curl）
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:80 || exit 1

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]

