#!/usr/bin/env bash
set -euo pipefail

CONTAINER=${1:-c2c_mysql}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASS=${MYSQL_PASS:-Qsycl741}
DB=${DB:-trade}

run(){
  docker exec -i "$CONTAINER" mysql -u${MYSQL_USER} -p${MYSQL_PASS} -e "$1"
}

echo "=== Users (latest 10) ==="
run "USE ${DB}; SELECT id, username, email, created_at FROM users ORDER BY id DESC LIMIT 10;"

echo "\n=== Roles ==="
run "USE ${DB}; SELECT id, name FROM roles ORDER BY id;"

echo "\n=== User Roles (latest 20) ==="
run "USE ${DB}; SELECT ur.user_id, u.username, ur.role_id FROM user_roles ur JOIN users u ON u.id=ur.user_id ORDER BY ur.user_id DESC, ur.role_id DESC LIMIT 20;"

echo "\nTip: You can pass a different container name as first arg if needed (default: c2c_mysql)."
