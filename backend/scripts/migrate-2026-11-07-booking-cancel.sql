-- 2026-11-07 学员端增强：预约取消原因 + 取消时间
-- 适用于已存在旧库的环境；全新导入可直接使用 schema.sql / driving_school_full.sql。
ALTER TABLE `booking`
    ADD COLUMN `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因' AFTER `create_time`,
    ADD COLUMN `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间' AFTER `cancel_reason`;
