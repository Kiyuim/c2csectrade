-- =================================================================
-- RebookTrade - Complete Database Initialization Script
-- Version: 2.0
-- Last Updated: 2025-10-21
-- =================================================================
-- This script handles:
-- 1. Database creation with UTF-8mb4 character set.
-- 2. Dropping and recreating all tables for a clean slate.
-- 3. Seeding essential data (roles, admin user).
-- 4. Seeding realistic test data for users and products (books).
-- =================================================================

-- Set session variables for character encoding
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Create the database if it doesn't exist, ensuring correct character set
CREATE DATABASE IF NOT EXISTS trade CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trade;

-- Disable foreign key checks to allow clean drop
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables in reverse order of dependency to avoid foreign key errors
DROP TABLE IF EXISTS `shopping_cart`;
DROP TABLE IF EXISTS `user_favorites`;
DROP TABLE IF EXISTS `pms_product_media`;
DROP TABLE IF EXISTS `pms_product`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `users`;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- =================================================================
-- Table Structure Definitions
-- =================================================================

-- Table `users`
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User unique ID',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username, unique',
    `display_name` VARCHAR(100) NULL COMMENT 'Display name for the user',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed password',
    `email` VARCHAR(100) NOT NULL COMMENT 'User email, unique',
    `avatar_url` VARCHAR(255) NULL COMMENT 'User avatar URL',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username`),
    UNIQUE INDEX `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Users table';

-- Table `roles`
CREATE TABLE `roles` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT 'Role unique ID',
    `name` VARCHAR(50) NOT NULL COMMENT 'Role name, unique',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_rolename` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roles table';

-- Table `user_roles`
CREATE TABLE `user_roles` (
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `role_id` INT NOT NULL COMMENT 'Role ID',
    PRIMARY KEY (`user_id`, `role_id`),
    INDEX `fk_user_roles_role_id_idx` (`role_id`),
    CONSTRAINT `fk_user_roles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_user_roles_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role association table';

-- Table `chat_message`
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Message unique ID',
    `sender_id` BIGINT NULL COMMENT 'Sender user ID (NULL for system messages)',
    `recipient_id` BIGINT NOT NULL COMMENT 'Recipient user ID',
    `content` TEXT NOT NULL COMMENT 'Message content',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Message sent time',
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Is the message read',
    `is_system_message` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Is it a system message',
    PRIMARY KEY (`id`),
    INDEX `idx_sender_recipient` (`sender_id`, `recipient_id`),
    INDEX `idx_recipient_read` (`recipient_id`, `is_read`),
    CONSTRAINT `fk_chat_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_chat_message_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat messages table';

-- Table `pms_product`
CREATE TABLE `pms_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Product ID',
    `user_id` BIGINT NOT NULL COMMENT 'Seller user ID',
    `name` VARCHAR(255) NOT NULL COMMENT 'Product name',
    `description` TEXT NULL COMMENT 'Product description',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'Price',
    `stock` INT NOT NULL DEFAULT 1 COMMENT 'Stock (default 1 for used items)',
    `condition_level` INT NOT NULL DEFAULT 9 COMMENT 'Condition (1-10)',
    `location` VARCHAR(255) NULL COMMENT 'Location',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 1=For Sale, 2=Sold, 0=Delisted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_price` (`price`),
    CONSTRAINT `fk_product_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Products table';

-- Table `pms_product_media`
CREATE TABLE `pms_product_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Media ID',
    `product_id` BIGINT NOT NULL COMMENT 'Product ID',
    `url` VARCHAR(1024) NOT NULL COMMENT 'Media URL',
    `media_type` TINYINT NOT NULL COMMENT 'Media type: 1=Image, 2=Video',
    `sort_order` INT NULL DEFAULT 0 COMMENT 'Sort order',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    CONSTRAINT `fk_media_product` FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product media table';

-- Table `user_favorites`
CREATE TABLE `user_favorites` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Favorite ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `product_id` BIGINT NOT NULL COMMENT 'Product ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User favorites table';

-- Table `shopping_cart`
CREATE TABLE `shopping_cart` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Cart ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `product_id` BIGINT NOT NULL COMMENT 'Product ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT 'Quantity',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Shopping cart table';

-- =================================================================
-- Seeding Essential Data
-- =================================================================

-- Insert base roles
INSERT INTO `roles` (`id`, `name`) VALUES (1, 'ROLE_USER'), (2, 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Create a default admin account (username: admin, password: admin123)
-- BCrypt hash for 'admin123'
INSERT INTO `users` (`id`, `username`, `display_name`, `password_hash`, `email`, `avatar_url`)
VALUES (1, 'admin', '系统管理员', '$2a$12$9l1r7OVMYW3xsv/JQchZKutlvkIJgopmbNC3jEA3hUkNbN/ivzMn2', 'admin@rebook.trade', 'https://i.pravatar.cc/150?u=admin')
ON DUPLICATE KEY UPDATE username=VALUES(username), display_name=VALUES(display_name), password_hash=VALUES(password_hash);

-- Assign roles to admin user
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1, 1), (1, 2)
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- =================================================================
-- End of Script
-- =================================================================
SELECT 'Database structure and essential data seeded successfully.' AS status;

-- 修改数据库字符集
ALTER DATABASE trade CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改 users 表字符集
ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改 pms_product 表字符集
ALTER TABLE pms_product CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;



-- 检查所有表的字符集
SELECT
    TABLE_NAME,
    TABLE_COLLATION
FROM
    information_schema.TABLES
WHERE
    TABLE_SCHEMA = 'trade';

-- 检查商品数量
SELECT COUNT(*) as total_products FROM pms_product;

-- 显示前20条商品数据（检查乱码）
SELECT
    id,
    name,
    description,
    location,
    HEX(name) as name_hex,
    CHAR_LENGTH(name) as name_length
FROM
    pms_product
        LIMIT 20;

-- 修改 chat_message 表字符集
ALTER TABLE chat_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;