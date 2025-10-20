#!/usr/bin/env bash
set -euo pipefail

CONTAINER=${1:-c2c_mysql}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASS=${MYSQL_PASS:-Qsycl741}
DB=${DB:-trade}

docker exec -i "$CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" -D "$DB" <<'SQL'
-- Ensure base roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER') ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO roles (id, name) VALUES (2, 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Ensure admin exists (create if missing with a temp email)
INSERT INTO users (id, username, password_hash, email)
SELECT 1, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'admin@example.com'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin');

-- Reset admin password to admin123 (BCrypt) and email
UPDATE users SET password_hash='$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', email='admin@example.com' WHERE username='admin';

-- Grant admin roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 2 FROM users u WHERE u.username='admin'
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 1 FROM users u WHERE u.username='admin'
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);
SQL

echo "Patched admin user in container '$CONTAINER' (db: ${DB})."
