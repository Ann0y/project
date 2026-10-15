-- 建库建表脚本（在 MySQL 里执行一次即可）
CREATE DATABASE IF NOT EXISTS `driving_school` DEFAULT CHARACTER SET utf8mb4;
USE `driving_school`;

CREATE TABLE IF NOT EXISTS `user` (
    `id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50)  NOT NULL COMMENT '登录名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `role`     VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT '角色：admin/student',
    `name`     VARCHAR(50)           DEFAULT NULL COMMENT '显示名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 说明：演示账号由后端启动时的 DataInitializer 自动写入（密码用 BCrypt 现场加密），
-- 因此这里不需要手动 INSERT。
