# Frontend Dockerfile (Vue 3 + Vue CLI)
# Runtime-only: copy prebuilt dist into Nginx
FROM nginx:alpine
WORKDIR /
# Copy built assets from local 'dist' (build outside the container)
COPY dist /usr/share/nginx/html
# Nginx config with API proxy to backend service name inside compose
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
