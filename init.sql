-- =================================================================
-- c2csectrade - Complete Database Initialization Script
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
DROP TABLE IF EXISTS `bargain_help`;
DROP TABLE IF EXISTS `bargain_activity`;
DROP TABLE IF EXISTS `shopping_cart`;
DROP TABLE IF EXISTS `user_favorites`;
DROP TABLE IF EXISTS `pms_product_media`;
DROP TABLE IF EXISTS `pms_report`;
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
    `payment_password_hash` VARCHAR(255) NULL COMMENT 'BCrypt hashed payment password (6 digits)',
    `email` VARCHAR(100) NOT NULL COMMENT 'User email, unique',
    `avatar_url` VARCHAR(255) NULL COMMENT 'User avatar URL',
    `balance` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'User balance',
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
    `category` VARCHAR(100) NULL DEFAULT 'other' COMMENT 'Product category',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 1=For Sale, 2=Sold, 0=Delisted',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_price` (`price`),
    INDEX `idx_category` (`category`),
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

-- Table `pms_report`
CREATE TABLE `pms_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Report ID',
    `product_id` BIGINT NOT NULL COMMENT 'Reported Product ID',
    `reporter_id` BIGINT NOT NULL COMMENT 'Reporter User ID',
    `reason` VARCHAR(255) NOT NULL COMMENT 'Reason for reporting',
    `description` TEXT NULL COMMENT 'Detailed description of the report',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT 'Report status',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_reporter_id` (`reporter_id`),
    CONSTRAINT `fk_report_product` FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product reports table';

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

-- 订单表
CREATE TABLE `orders` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                          `user_id` BIGINT NOT NULL COMMENT '用户ID',
                          `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '总金额',
                          `status` VARCHAR(20) NOT NULL COMMENT '订单状态 (PENDING, PAID, SHIPPED, DELIVERED, CANCELED, EXPIRED)',
                          `payment_method` VARCHAR(50) NULL COMMENT '支付方式 (alipay, wechat, bank, balance)',
                          `expire_time` TIMESTAMP NULL COMMENT '订单过期时间（15分钟后）',
                          `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`),
                          FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
                          INDEX `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单项表
CREATE TABLE `order_items` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
                               `order_id` BIGINT NOT NULL COMMENT '订单ID',
                               `product_id` BIGINT NOT NULL COMMENT '商品ID',
                               `quantity` INT NOT NULL COMMENT '数量',
                               `price` DECIMAL(10, 2) NOT NULL COMMENT '单价',
                               `from_cart` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否来自购物车',
                               PRIMARY KEY (`id`),
                               FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
                               FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 交易表 (用于模拟支付)
CREATE TABLE `transactions` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '交易ID',
                                `order_id` BIGINT NOT NULL COMMENT '订单ID',
                                `amount` DECIMAL(10, 2) NOT NULL COMMENT '交易金额',
                                `payment_method` VARCHAR(50) NULL COMMENT '支付方式 (alipay, wechat, bank, balance)',
                                `transaction_type` VARCHAR(20) NOT NULL COMMENT '交易类型 (PAYMENT, REFUND)',
                                `status` VARCHAR(20) NOT NULL COMMENT '交易状态 (SUCCESS, FAILED)',
                                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`),
                                FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 砍价活动表
CREATE TABLE `bargain_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '砍价活动ID',
    `user_id` BIGINT NOT NULL COMMENT '发起用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `original_price` DECIMAL(10, 2) NOT NULL COMMENT '原价',
    `target_price` DECIMAL(10, 2) NOT NULL COMMENT '目标价格（最低价）',
    `current_price` DECIMAL(10, 2) NOT NULL COMMENT '当前价格',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-进行中, SUCCESS-成功, EXPIRED-已过期',
    `expire_time` TIMESTAMP NOT NULL COMMENT '过期时间',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='砍价活动表';

-- 砍价助力记录表
CREATE TABLE `bargain_help` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '助力记录ID',
    `bargain_id` BIGINT NOT NULL COMMENT '砍价活动ID',
    `helper_id` BIGINT NULL COMMENT '助力用户ID（可为空，支持游客助力）',
    `helper_name` VARCHAR(100) NULL COMMENT '助力用户昵称（游客时使用）',
    `cut_amount` DECIMAL(10, 2) NOT NULL COMMENT '砍掉的金额',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '助力时间',
    PRIMARY KEY (`id`),
    INDEX `idx_bargain_id` (`bargain_id`),
    INDEX `idx_helper_id` (`helper_id`),
    FOREIGN KEY (`bargain_id`) REFERENCES `bargain_activity`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='砍价助力记录表';

-- 评价表
CREATE TABLE `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id` INT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `buyer_id` INT NOT NULL COMMENT '买家ID',
    `seller_id` INT NOT NULL COMMENT '卖家ID',
    `product_rating` INT NOT NULL COMMENT '商品评分 1-5',
    `seller_rating` INT NOT NULL COMMENT '卖家评分 1-5',
    `comment` TEXT NULL COMMENT '评价内容',
    `review_images` TEXT NULL COMMENT '评价图片，多个用逗号分隔',
    `is_anonymous` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否匿名评价',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_buyer_id` (`buyer_id`),
    INDEX `idx_seller_id` (`seller_id`),
    CONSTRAINT `fk_review_product` FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 信用积分表
CREATE TABLE `credit_score` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '信用积分ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_score` INT NOT NULL DEFAULT 100 COMMENT '总信用分',
    `level` INT NOT NULL DEFAULT 1 COMMENT '信用等级 1-5',
    `total_sales` INT NOT NULL DEFAULT 0 COMMENT '总销售数',
    `total_purchases` INT NOT NULL DEFAULT 0 COMMENT '总购买数',
    `average_seller_rating` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '平均卖家评分',
    `positive_reviews` INT NOT NULL DEFAULT 0 COMMENT '好评数',
    `neutral_reviews` INT NOT NULL DEFAULT 0 COMMENT '中评数',
    `negative_reviews` INT NOT NULL DEFAULT 0 COMMENT '差评数',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id` (`user_id`),
    CONSTRAINT `fk_credit_score_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用积分表';

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
ALTER TABLE chat_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;