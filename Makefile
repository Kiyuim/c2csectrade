.PHONY: up down ps logs build backend frontend clean

# One-click: build backend+frontend locally, then compose up -d
up:
	./scripts/run-all.sh

# Stop and remove containers, networks, and volumes
down:
	docker compose down -v

# Show running services
ps:
	docker compose ps

# Tail logs of backend
logs:
	docker compose logs -f springboot-app

# Build images only (assumes local build outputs already exist)
build:
	docker compose build

# Build backend jar locally
backend:
	mvn clean package -DskipTests

# Build frontend dist locally
frontend:
	cd frontend && (npm ci || npm install) && npm run build

# Clean docker resources (dangling)
clean:
	docker system prune -f

