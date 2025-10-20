-- init.sql
-- 确保我们使用的是正确的数据库
USE trade;

-- 删除过时的表（开发环境中安全；如果您不想删除，请保持注释状态）
-- DROP TABLE IF EXISTS ums_user;

CREATE TABLE IF NOT EXISTS `users` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
                                       `username` VARCHAR(50) NOT NULL COMMENT '用户名，唯一',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '使用BCrypt哈希后的密码',
    `email` VARCHAR(100) NOT NULL COMMENT '用户邮箱，唯一',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username` ASC),
    UNIQUE INDEX `uk_email` (`email` ASC)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -----------------------------------------------------
-- Table `roles`
-- 存储系统中的所有角色
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
                                       `id` INT NOT NULL AUTO_INCREMENT COMMENT '角色唯一ID',
                                       `name` VARCHAR(50) NOT NULL COMMENT '角色名称，唯一',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_rolename` (`name` ASC)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- -----------------------------------------------------
-- Table `user_roles`
-- 用户与角色的多对多关联表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_roles` (
                                            `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                            `role_id` INT NOT NULL COMMENT '角色ID',
                                            PRIMARY KEY (`user_id`, `role_id`),
    INDEX `fk_user_roles_role_id_idx` (`role_id` ASC),
    CONSTRAINT `fk_user_roles_user_id`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    CONSTRAINT `fk_user_roles_role_id`
    FOREIGN KEY (`role_id`)
    REFERENCES `roles` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1. 插入基础角色
-- ID=1: 普通用户, ID=2: 管理员
INSERT INTO `roles` (`id`, `name`) VALUES (1, 'ROLE_USER') ON DUPLICATE KEY UPDATE name=VALUES(name);
INSERT INTO `roles` (`id`, `name`) VALUES (2, 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. 创建一个默认的管理员账户
-- 用户名: admin
-- 密码 (明文): admin123
-- 密码哈希值使用 BCrypt 加密 (strength=10)
INSERT INTO `users` (`id`, `username`, `password_hash`, `email`)
VALUES (1, 'admin', '$2a$12$EWNeBRifJ5dT/adROJet..8qlyKbuxOJxjOtQUbmNKFeYiqDZ/Xy2', 'admin@example.com')
    ON DUPLICATE KEY UPDATE username=VALUES(username), password_hash=VALUES(password_hash), email=VALUES(email);

-- 3. 为默认管理员分配管理员角色
-- 将 user_id=1 (admin) 与 role_id=2 (ROLE_ADMIN) 关联起来
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1, 2)
    ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), role_id=VALUES(role_id);

-- 4. 同时为管理员分配普通用户角色（可选，但通常是好的实践）
-- 这样管理员也拥有普通用户的所有功能
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1, 1)
    ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), role_id=VALUES(role_id);

-- -----------------------------------------------------
-- Table `chat_message`
-- 存储用户之间的聊天消息
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息唯一ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者用户ID',
    `recipient_id` BIGINT NOT NULL COMMENT '接收者用户ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '消息是否已读',
    PRIMARY KEY (`id`),
    INDEX `idx_sender_recipient` (`sender_id`, `recipient_id`),
    INDEX `idx_recipient_read` (`recipient_id`, `is_read`),
    INDEX `idx_timestamp` (`timestamp`),
    CONSTRAINT `fk_chat_message_sender`
        FOREIGN KEY (`sender_id`)
        REFERENCES `users` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT `fk_chat_message_recipient`
        FOREIGN KEY (`recipient_id`)
        REFERENCES `users` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';
