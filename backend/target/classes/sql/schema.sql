-- ============================================================================
-- 驾校学员培训与考试管理系统 · 建库建表脚本
-- 依据《需求文档》第 6 章「数据库设计（详细）」
-- 数据库：MySQL 8.0 / MariaDB 10.6+，字符集 utf8mb4，引擎 InnoDB
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `driving_school`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `driving_school`;

-- 先删旧表（按外键依赖逆序），方便重复执行
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `payment`;
DROP TABLE IF EXISTS `exam_record`;
DROP TABLE IF EXISTS `exam_plan`;
DROP TABLE IF EXISTS `hours_record`;
DROP TABLE IF EXISTS `booking`;
DROP TABLE IF EXISTS `coach_schedule`;
DROP TABLE IF EXISTS `train_course`;
DROP TABLE IF EXISTS `vehicle`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `coach`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 6.3.1 sys_user 登录用户
-- ---------------------------------------------------------------------------
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    `name`        VARCHAR(50)  NOT NULL COMMENT '显示姓名',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT 'admin / coach / student',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 正常 / 1 删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录用户';

-- ---------------------------------------------------------------------------
-- 6.3.3 coach 教练档案
-- ---------------------------------------------------------------------------
CREATE TABLE `coach` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT      NOT NULL COMMENT '关联登录账号',
    `code`          VARCHAR(20) NOT NULL COMMENT '教练工号',
    `phone`         VARCHAR(20)          DEFAULT NULL COMMENT '联系电话',
    `license_types` VARCHAR(50) NOT NULL COMMENT '准教车型，逗号分隔',
    `status`        VARCHAR(10) NOT NULL DEFAULT '在职' COMMENT '在职 / 停用',
    `del_flag`      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`),
    UNIQUE KEY `uk_code` (`code`),
    CONSTRAINT `fk_coach_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教练档案';

-- ---------------------------------------------------------------------------
-- 6.3.4 vehicle 教练车辆
-- ---------------------------------------------------------------------------
CREATE TABLE `vehicle` (
    `id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plate_no` VARCHAR(20) NOT NULL COMMENT '车牌号',
    `model`    VARCHAR(50)          DEFAULT NULL COMMENT '车型',
    `coach_id` BIGINT               DEFAULT NULL COMMENT '所属教练',
    `status`   VARCHAR(10) NOT NULL DEFAULT '可用' COMMENT '可用 / 维修 / 停用',
    `del_flag` TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plate` (`plate_no`),
    CONSTRAINT `fk_vehicle_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教练车辆';

-- ---------------------------------------------------------------------------
-- 6.3.2 student 学员档案
-- ---------------------------------------------------------------------------
CREATE TABLE `student` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT      NOT NULL COMMENT '关联登录账号',
    -- AES-GCM 密文形如 v1:base64(iv+ct)，18 位明文加密后约 67 字符，故留 128 余量
    `id_card`      VARCHAR(128) NOT NULL COMMENT '身份证号（AES 密文）',
    `phone`        VARCHAR(20) NOT NULL COMMENT '联系电话',
    `license_type` VARCHAR(10) NOT NULL COMMENT '准驾车型',
    `enroll_date`  DATE                 DEFAULT NULL COMMENT '报名日期',
    `status`       VARCHAR(10) NOT NULL DEFAULT '待审核' COMMENT '待审核 / 在训 / 结业 / 退学',
    `coach_id`     BIGINT               DEFAULT NULL COMMENT '固定带教教练',
    `vehicle_id`   BIGINT               DEFAULT NULL COMMENT '固定带教车辆',
    `del_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`),
    UNIQUE KEY `uk_id_card` (`id_card`),
    KEY `idx_status` (`status`),
    KEY `idx_coach` (`coach_id`),
    CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_student_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`),
    CONSTRAINT `fk_student_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学员档案';

-- ---------------------------------------------------------------------------
-- 6.3.5 train_course 培训课程 / 科目学时配置
-- ---------------------------------------------------------------------------
CREATE TABLE `train_course` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `license_type`   VARCHAR(10)   NOT NULL COMMENT '准驾车型',
    `subject_code`   VARCHAR(10)   NOT NULL COMMENT '科目代码 1/2/3/4',
    `subject_name`   VARCHAR(50)   NOT NULL COMMENT '科目名称',
    `required_hours` DECIMAL(5, 1) NOT NULL COMMENT '大纲要求学时',
    `del_flag`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_subject` (`license_type`, `subject_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训课程 / 科目学时配置';

-- ---------------------------------------------------------------------------
-- 6.3.6 coach_schedule 教练排课时段
-- ---------------------------------------------------------------------------
CREATE TABLE `coach_schedule` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `coach_id`      BIGINT      NOT NULL COMMENT '教练',
    `vehicle_id`    BIGINT               DEFAULT NULL COMMENT '关联车辆',
    `schedule_date` DATE        NOT NULL COMMENT '排课日期',
    `start_time`    TIME        NOT NULL COMMENT '开始时间',
    `end_time`      TIME        NOT NULL COMMENT '结束时间',
    `capacity`      INT         NOT NULL DEFAULT 1 COMMENT '容量（可约人数）',
    `booked_count`  INT         NOT NULL DEFAULT 0 COMMENT '已约人数（冗余缓存）',
    `status`        VARCHAR(10) NOT NULL DEFAULT '可约' COMMENT '可约 / 已满 / 停用',
    `del_flag`      TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_coach_date` (`coach_id`, `schedule_date`),
    CONSTRAINT `fk_schedule_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`),
    CONSTRAINT `fk_schedule_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '教练排课时段';

-- ---------------------------------------------------------------------------
-- 6.3.7 booking 培训预约
-- ---------------------------------------------------------------------------
CREATE TABLE `booking` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`  BIGINT      NOT NULL COMMENT '学员',
    `schedule_id` BIGINT      NOT NULL COMMENT '预约时段',
    `status`      VARCHAR(10) NOT NULL DEFAULT '待确认' COMMENT '待确认 / 已确认 / 已完成 / 已取消 / 爽约',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约提交时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_schedule` (`student_id`, `schedule_id`),
    KEY `idx_schedule_status` (`schedule_id`, `status`),
    CONSTRAINT `fk_booking_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_booking_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `coach_schedule` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训预约';

-- ---------------------------------------------------------------------------
-- 6.3.8 hours_record 学时记录
-- ---------------------------------------------------------------------------
CREATE TABLE `hours_record` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`    BIGINT        NOT NULL COMMENT '学员',
    `coach_id`      BIGINT        NOT NULL COMMENT '登记教练',
    `booking_id`    BIGINT                 DEFAULT NULL COMMENT '关联预约',
    `subject_code`  VARCHAR(10)   NOT NULL COMMENT '科目代码',
    `hours`         DECIMAL(4, 1) NOT NULL COMMENT '本次学时',
    `train_date`    DATE          NOT NULL COMMENT '培训日期',
    `audit_status`  VARCHAR(10)   NOT NULL DEFAULT '待审核' COMMENT '待审核 / 已通过 / 已驳回',
    `auditor_id`    BIGINT                 DEFAULT NULL COMMENT '审核人',
    `audit_opinion` VARCHAR(255)           DEFAULT NULL COMMENT '审核意见',
    `audit_time`    DATETIME               DEFAULT NULL COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_subject` (`student_id`, `subject_code`),
    CONSTRAINT `fk_hours_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_hours_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`),
    CONSTRAINT `fk_hours_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`),
    CONSTRAINT `fk_hours_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学时记录';

-- ---------------------------------------------------------------------------
-- 6.3.9 exam_plan 考试计划
-- ---------------------------------------------------------------------------
CREATE TABLE `exam_plan` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_code`   VARCHAR(10)  NOT NULL COMMENT '科目代码',
    `exam_date`      DATE         NOT NULL COMMENT '考试日期',
    `exam_time`      TIME                  DEFAULT NULL COMMENT '考试时间',
    `venue`          VARCHAR(100) NOT NULL COMMENT '考场',
    `capacity`       INT          NOT NULL COMMENT '容量',
    `enrolled_count` INT          NOT NULL DEFAULT 0 COMMENT '已报名人数（冗余缓存）',
    `status`         VARCHAR(10)  NOT NULL DEFAULT '报名中' COMMENT '报名中 / 已截止 / 已完成',
    `del_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考试计划';

-- ---------------------------------------------------------------------------
-- 6.3.10 exam_record 考试报名与成绩
-- ---------------------------------------------------------------------------
CREATE TABLE `exam_record` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`       BIGINT        NOT NULL COMMENT '考试计划',
    `student_id`    BIGINT        NOT NULL COMMENT '学员',
    `register_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `attempt_no`    INT           NOT NULL DEFAULT 1 COMMENT '第几次（补考递增）',
    `score`         DECIMAL(4, 1)          DEFAULT NULL COMMENT '成绩',
    `status`        VARCHAR(10)   NOT NULL DEFAULT '待考' COMMENT '待考 / 合格 / 不合格 / 补考中',
    `exam_date`     DATE                   DEFAULT NULL COMMENT '实际考试日期',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_student` (`plan_id`, `student_id`),
    CONSTRAINT `fk_exam_plan` FOREIGN KEY (`plan_id`) REFERENCES `exam_plan` (`id`),
    CONSTRAINT `fk_exam_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考试报名与成绩';

-- ---------------------------------------------------------------------------
-- 6.3.11 payment 缴费记录
-- ---------------------------------------------------------------------------
CREATE TABLE `payment` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`  BIGINT         NOT NULL COMMENT '学员',
    `item`        VARCHAR(20)    NOT NULL COMMENT '培训费 / 补考费 / 其它',
    `amount`      DECIMAL(10, 2) NOT NULL COMMENT '金额',
    `pay_time`    DATETIME                DEFAULT NULL COMMENT '缴费时间',
    `pay_method`  VARCHAR(20)             DEFAULT NULL COMMENT '现金 / 微信 / 支付宝 / 银行',
    `status`      VARCHAR(10)    NOT NULL DEFAULT '未缴' COMMENT '未缴 / 已缴 / 退费',
    `operator_id` BIGINT                  DEFAULT NULL COMMENT '经办人',
    PRIMARY KEY (`id`),
    KEY `idx_pay_student` (`student_id`),
    CONSTRAINT `fk_pay_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`),
    CONSTRAINT `fk_pay_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '缴费记录';

-- ---------------------------------------------------------------------------
-- 科目学时配置初始数据（train_course，按准驾车型 × 科目）
-- ---------------------------------------------------------------------------
INSERT INTO `train_course` (`license_type`, `subject_code`, `subject_name`, `required_hours`) VALUES
('C1', '1', '科目一 · 道路交通安全法律法规', 12.0),
('C1', '2', '科目二 · 场地驾驶技能',          26.0),
('C1', '3', '科目三 · 道路驾驶技能',          24.0),
('C1', '4', '科目四 · 安全文明驾驶常识',      10.0),
('C2', '1', '科目一 · 道路交通安全法律法规', 12.0),
('C2', '2', '科目二 · 场地驾驶技能',          14.0),
('C2', '3', '科目三 · 道路驾驶技能',          24.0),
('C2', '4', '科目四 · 安全文明驾驶常识',      10.0),
('B2', '1', '科目一 · 道路交通安全法律法规', 12.0),
('B2', '2', '科目二 · 场地驾驶技能',          32.0),
('B2', '3', '科目三 · 道路驾驶技能',          36.0),
('B2', '4', '科目四 · 安全文明驾驶常识',      16.0),
('A2', '1', '科目一 · 道路交通安全法律法规', 12.0),
('A2', '2', '科目二 · 场地驾驶技能',          30.0),
('A2', '3', '科目三 · 道路驾驶技能',          34.0),
('A2', '4', '科目四 · 安全文明驾驶常识',      14.0);
