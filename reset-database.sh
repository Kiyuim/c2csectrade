#!/bin/bash

# Reset database script for both local and Docker MySQL

echo "================================================"
echo "RebookTrade - Database Reset Script"
echo "================================================"

# MySQL credentials
LOCAL_MYSQL_USER="root"
LOCAL_MYSQL_PASS="Qsycl741"
DB_NAME="trade"

echo ""
echo "Step 1: Resetting LOCAL MySQL database..."
echo "================================================"

# Drop and recreate the local database completely
mysql -u${LOCAL_MYSQL_USER} -p${LOCAL_MYSQL_PASS} -e "DROP DATABASE IF EXISTS ${DB_NAME};" 2>&1 | grep -v "Using a password"
mysql -u${LOCAL_MYSQL_USER} -p${LOCAL_MYSQL_PASS} -e "CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | grep -v "Using a password"

echo "✓ Local database dropped and recreated"

# Import init.sql
echo ""
echo "Importing init.sql to local MySQL..."
mysql -u${LOCAL_MYSQL_USER} -p${LOCAL_MYSQL_PASS} ${DB_NAME} < init.sql 2>&1 | grep -v "Using a password"

if [ $? -eq 0 ]; then
    echo "✓ init.sql imported successfully"
else
    echo "✗ Error importing init.sql"
    exit 1
fi

# Import test-data.sql
echo ""
echo "Importing test-data.sql to local MySQL..."
mysql -u${LOCAL_MYSQL_USER} -p${LOCAL_MYSQL_PASS} ${DB_NAME} < test-data.sql 2>&1 | grep -v "Using a password"

if [ $? -eq 0 ]; then
    echo "✓ test-data.sql imported successfully"
else
    echo "✗ Error importing test-data.sql"
    exit 1
fi

# Verify local data
echo ""
echo "Verifying local database..."
mysql -u${LOCAL_MYSQL_USER} -p${LOCAL_MYSQL_PASS} ${DB_NAME} -e "
SELECT 'Users:' as Info, COUNT(*) as Count FROM users
UNION ALL
SELECT 'Products:', COUNT(*) FROM pms_product
UNION ALL
SELECT 'Product Media:', COUNT(*) FROM pms_product_media;
" 2>&1 | grep -v "Using a password"

echo ""
echo "Step 2: Resetting DOCKER MySQL database..."
echo "================================================"

# Check if docker container is running
if ! docker ps | grep -q mysql; then
    echo "⚠ MySQL Docker container is not running. Skipping Docker reset."
    echo "To reset Docker database later, run: docker-compose restart mysql"
else
    # Drop and recreate the Docker database
    docker exec -i rebooktrade-mysql-1 mysql -uroot -prootpassword -e "DROP DATABASE IF EXISTS ${DB_NAME};" 2>&1 | grep -v "Using a password"
    docker exec -i rebooktrade-mysql-1 mysql -uroot -prootpassword -e "CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | grep -v "Using a password"

    echo "✓ Docker database dropped and recreated"

    # Import init.sql to Docker
    echo ""
    echo "Importing init.sql to Docker MySQL..."
    docker exec -i rebooktrade-mysql-1 mysql -uroot -prootpassword ${DB_NAME} < init.sql 2>&1 | grep -v "Using a password"

    if [ $? -eq 0 ]; then
        echo "✓ init.sql imported to Docker successfully"
    else
        echo "✗ Error importing init.sql to Docker"
        exit 1
    fi

    # Import test-data.sql to Docker
    echo ""
    echo "Importing test-data.sql to Docker MySQL..."
    docker exec -i rebooktrade-mysql-1 mysql -uroot -prootpassword ${DB_NAME} < test-data.sql 2>&1 | grep -v "Using a password"

    if [ $? -eq 0 ]; then
        echo "✓ test-data.sql imported to Docker successfully"
    else
        echo "✗ Error importing test-data.sql to Docker"
        exit 1
    fi

    # Verify Docker data
    echo ""
    echo "Verifying Docker database..."
    docker exec -i rebooktrade-mysql-1 mysql -uroot -prootpassword ${DB_NAME} -e "
    SELECT 'Users:' as Info, COUNT(*) as Count FROM users
    UNION ALL
    SELECT 'Products:', COUNT(*) FROM pms_product
    UNION ALL
    SELECT 'Product Media:', COUNT(*) FROM pms_product_media;
    " 2>&1 | grep -v "Using a password"
fi

echo ""
echo "================================================"
echo "✓ Database reset completed successfully!"
echo "================================================"
echo ""
echo "Summary:"
echo "  - Local MySQL: Reset and data imported"
echo "  - Docker MySQL: $(docker ps | grep -q mysql && echo 'Reset and data imported' || echo 'Skipped (container not running)')"
echo ""

