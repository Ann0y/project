/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.20-13.0.2-MariaDB, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: driving_school
-- ------------------------------------------------------
-- Server version	13.0.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `booking`
--

DROP TABLE IF EXISTS `booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) NOT NULL COMMENT '学员',
  `schedule_id` bigint(20) NOT NULL COMMENT '预约时段',
  `status` varchar(10) NOT NULL DEFAULT '待确认' COMMENT '待确认 / 已确认 / 已完成 / 已取消 / 爽约',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '预约提交时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_schedule` (`student_id`,`schedule_id`),
  KEY `idx_schedule_status` (`schedule_id`,`status`),
  CONSTRAINT `fk_booking_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `coach_schedule` (`id`),
  CONSTRAINT `fk_booking_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='培训预约';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `booking`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `booking` DISABLE KEYS */;
INSERT INTO `booking` (`id`, `student_id`, `schedule_id`, `status`, `create_time`) VALUES (1,1,1,'已取消','2026-10-17 15:13:42'),
(2,1,3,'已完成','2026-10-17 15:11:43'),
(3,1,11,'已完成','2026-10-17 15:19:48'),
(4,2,13,'已取消','2026-10-17 15:11:43'),
(5,3,20,'已确认','2026-10-17 15:11:43'),
(6,3,22,'爽约','2026-10-17 15:11:43'),
(7,3,30,'已完成','2026-10-17 15:11:43'),
(8,4,32,'已完成','2026-10-17 15:11:43'),
(9,7,39,'已确认','2026-10-17 15:11:43'),
(10,7,41,'已取消','2026-10-17 15:11:43'),
(11,7,48,'爽约','2026-10-17 15:11:43'),
(12,8,50,'爽约','2026-10-17 15:11:43'),
(13,9,58,'已确认','2026-10-17 15:11:43'),
(14,9,60,'已完成','2026-10-17 15:11:43'),
(15,9,67,'已取消','2026-10-17 15:11:43'),
(16,11,69,'已取消','2026-10-17 15:11:43'),
(17,12,77,'已确认','2026-10-17 15:11:43'),
(18,12,79,'爽约','2026-10-17 15:11:43'),
(19,12,86,'已完成','2026-10-17 15:11:43'),
(20,13,88,'已完成','2026-10-17 15:11:43'),
(21,14,95,'已确认','2026-10-17 15:11:43'),
(22,14,97,'已取消','2026-10-17 15:11:43'),
(23,14,105,'爽约','2026-10-17 15:11:43'),
(24,15,107,'爽约','2026-10-17 15:11:43'),
(26,1,4,'已完成','2026-10-17 15:18:58'),
(27,1,12,'已完成','2026-10-17 15:19:51'),
(28,1,13,'已确认','2026-10-17 15:28:22');
/*!40000 ALTER TABLE `booking` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `coach`
--

DROP TABLE IF EXISTS `coach`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `coach` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '关联登录账号',
  `code` varchar(20) NOT NULL COMMENT '教练工号',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `license_types` varchar(50) NOT NULL COMMENT '准教车型，逗号分隔',
  `status` varchar(10) NOT NULL DEFAULT '在职' COMMENT '在职 / 停用',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  UNIQUE KEY `uk_code` (`code`),
  CONSTRAINT `fk_coach_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='教练档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coach`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `coach` DISABLE KEYS */;
INSERT INTO `coach` (`id`, `user_id`, `code`, `phone`, `license_types`, `status`, `del_flag`) VALUES (1,2,'C001','13800001001','C1,C2','在职',0),
(2,4,'C002','13800001002','C1','在职',0),
(3,5,'C003','13800001003','C1,B2','在职',0);
/*!40000 ALTER TABLE `coach` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `coach_schedule`
--

DROP TABLE IF EXISTS `coach_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `coach_schedule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `coach_id` bigint(20) NOT NULL COMMENT '教练',
  `vehicle_id` bigint(20) DEFAULT NULL COMMENT '关联车辆',
  `schedule_date` date NOT NULL COMMENT '排课日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `capacity` int(11) NOT NULL DEFAULT 1 COMMENT '容量（可约人数）',
  `booked_count` int(11) NOT NULL DEFAULT 0 COMMENT '已约人数（冗余缓存）',
  `status` varchar(10) NOT NULL DEFAULT '可约' COMMENT '可约 / 已满 / 停用',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_coach_date` (`coach_id`,`schedule_date`),
  KEY `fk_schedule_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_schedule_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`),
  CONSTRAINT `fk_schedule_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='教练排课时段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coach_schedule`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `coach_schedule` DISABLE KEYS */;
INSERT INTO `coach_schedule` (`id`, `coach_id`, `vehicle_id`, `schedule_date`, `start_time`, `end_time`, `capacity`, `booked_count`, `status`, `del_flag`) VALUES (1,1,1,'2026-10-17','08:00:00','10:00:00',1,0,'可约',0),
(2,1,2,'2026-10-17','10:00:00','12:00:00',1,0,'可约',0),
(3,1,1,'2026-10-17','13:30:00','15:30:00',1,0,'可约',0),
(4,1,2,'2026-10-17','15:30:00','17:30:00',1,1,'已满',0),
(5,2,4,'2026-10-17','08:00:00','10:00:00',1,0,'可约',0),
(6,2,4,'2026-10-17','10:00:00','12:00:00',1,0,'可约',0),
(7,2,4,'2026-10-17','15:30:00','17:30:00',1,0,'可约',0),
(8,3,5,'2026-10-17','08:00:00','10:00:00',1,0,'可约',0),
(9,3,5,'2026-10-17','13:30:00','15:30:00',1,0,'可约',0),
(10,3,5,'2026-10-17','15:30:00','17:30:00',1,0,'可约',0),
(11,1,1,'2026-10-19','08:00:00','10:00:00',1,1,'已满',0),
(12,1,2,'2026-10-19','10:00:00','12:00:00',1,1,'已满',0),
(13,1,1,'2026-10-19','13:30:00','15:30:00',1,1,'已满',0),
(14,1,2,'2026-10-19','15:30:00','17:30:00',1,0,'可约',0),
(15,2,4,'2026-10-19','10:00:00','12:00:00',1,0,'可约',0),
(16,2,4,'2026-10-19','13:30:00','15:30:00',1,0,'可约',0),
(17,3,5,'2026-10-19','08:00:00','10:00:00',1,0,'可约',0),
(18,3,5,'2026-10-19','10:00:00','12:00:00',1,0,'可约',0),
(19,3,5,'2026-10-19','15:30:00','17:30:00',1,0,'可约',0),
(20,1,1,'2026-10-20','08:00:00','10:00:00',1,1,'已满',0),
(21,1,2,'2026-10-20','10:00:00','12:00:00',1,0,'可约',0),
(22,1,1,'2026-10-20','13:30:00','15:30:00',1,0,'可约',0),
(23,1,2,'2026-10-20','15:30:00','17:30:00',1,0,'可约',0),
(24,2,4,'2026-10-20','08:00:00','10:00:00',1,0,'可约',0),
(25,2,4,'2026-10-20','10:00:00','12:00:00',1,0,'可约',0),
(26,2,4,'2026-10-20','15:30:00','17:30:00',1,0,'可约',0),
(27,3,5,'2026-10-20','08:00:00','10:00:00',1,0,'可约',0),
(28,3,5,'2026-10-20','13:30:00','15:30:00',1,0,'可约',0),
(29,3,5,'2026-10-20','15:30:00','17:30:00',1,0,'可约',0),
(30,1,1,'2026-10-21','08:00:00','10:00:00',1,0,'可约',0),
(31,1,2,'2026-10-21','10:00:00','12:00:00',1,0,'可约',0),
(32,1,1,'2026-10-21','13:30:00','15:30:00',1,0,'可约',0),
(33,1,2,'2026-10-21','15:30:00','17:30:00',1,0,'可约',0),
(34,2,4,'2026-10-21','08:00:00','10:00:00',1,0,'可约',0),
(35,2,4,'2026-10-21','13:30:00','15:30:00',1,0,'可约',0),
(36,2,4,'2026-10-21','15:30:00','17:30:00',1,0,'可约',0),
(37,3,5,'2026-10-21','10:00:00','12:00:00',1,0,'可约',0),
(38,3,5,'2026-10-21','13:30:00','15:30:00',1,0,'可约',0),
(39,1,1,'2026-10-22','08:00:00','10:00:00',1,1,'已满',0),
(40,1,2,'2026-10-22','10:00:00','12:00:00',1,0,'可约',0),
(41,1,1,'2026-10-22','13:30:00','15:30:00',1,0,'可约',0),
(42,1,2,'2026-10-22','15:30:00','17:30:00',1,0,'可约',0),
(43,2,4,'2026-10-22','10:00:00','12:00:00',1,0,'可约',0),
(44,2,4,'2026-10-22','13:30:00','15:30:00',1,0,'可约',0),
(45,3,5,'2026-10-22','08:00:00','10:00:00',1,0,'可约',0),
(46,3,5,'2026-10-22','10:00:00','12:00:00',1,0,'可约',0),
(47,3,5,'2026-10-22','15:30:00','17:30:00',1,0,'可约',0),
(48,1,1,'2026-10-23','08:00:00','10:00:00',1,0,'可约',0),
(49,1,2,'2026-10-23','10:00:00','12:00:00',1,0,'可约',0),
(50,1,1,'2026-10-23','13:30:00','15:30:00',1,0,'可约',0),
(51,1,2,'2026-10-23','15:30:00','17:30:00',1,0,'可约',0),
(52,2,4,'2026-10-23','08:00:00','10:00:00',1,0,'可约',0),
(53,2,4,'2026-10-23','10:00:00','12:00:00',1,0,'可约',0),
(54,2,4,'2026-10-23','15:30:00','17:30:00',1,0,'可约',0),
(55,3,5,'2026-10-23','08:00:00','10:00:00',1,0,'可约',0),
(56,3,5,'2026-10-23','13:30:00','15:30:00',1,0,'可约',0),
(57,3,5,'2026-10-23','15:30:00','17:30:00',1,0,'可约',0),
(58,1,1,'2026-10-24','08:00:00','10:00:00',1,1,'已满',0),
(59,1,2,'2026-10-24','10:00:00','12:00:00',1,0,'可约',0),
(60,1,1,'2026-10-24','13:30:00','15:30:00',1,0,'可约',0),
(61,1,2,'2026-10-24','15:30:00','17:30:00',1,0,'可约',0),
(62,2,4,'2026-10-24','08:00:00','10:00:00',1,0,'可约',0),
(63,2,4,'2026-10-24','13:30:00','15:30:00',1,0,'可约',0),
(64,2,4,'2026-10-24','15:30:00','17:30:00',1,0,'可约',0),
(65,3,5,'2026-10-24','10:00:00','12:00:00',1,0,'可约',0),
(66,3,5,'2026-10-24','13:30:00','15:30:00',1,0,'可约',0),
(67,1,1,'2026-10-26','08:00:00','10:00:00',1,0,'可约',0),
(68,1,2,'2026-10-26','10:00:00','12:00:00',1,0,'可约',0),
(69,1,1,'2026-10-26','13:30:00','15:30:00',1,0,'可约',0),
(70,1,2,'2026-10-26','15:30:00','17:30:00',1,0,'可约',0),
(71,2,4,'2026-10-26','08:00:00','10:00:00',1,0,'可约',0),
(72,2,4,'2026-10-26','10:00:00','12:00:00',1,0,'可约',0),
(73,2,4,'2026-10-26','15:30:00','17:30:00',1,0,'可约',0),
(74,3,5,'2026-10-26','08:00:00','10:00:00',1,0,'可约',0),
(75,3,5,'2026-10-26','13:30:00','15:30:00',1,0,'可约',0),
(76,3,5,'2026-10-26','15:30:00','17:30:00',1,0,'可约',0),
(77,1,1,'2026-10-27','08:00:00','10:00:00',1,1,'已满',0),
(78,1,2,'2026-10-27','10:00:00','12:00:00',1,0,'可约',0),
(79,1,1,'2026-10-27','13:30:00','15:30:00',1,0,'可约',0),
(80,1,2,'2026-10-27','15:30:00','17:30:00',1,0,'可约',0),
(81,2,4,'2026-10-27','08:00:00','10:00:00',1,0,'可约',0),
(82,2,4,'2026-10-27','13:30:00','15:30:00',1,0,'可约',0),
(83,2,4,'2026-10-27','15:30:00','17:30:00',1,0,'可约',0),
(84,3,5,'2026-10-27','10:00:00','12:00:00',1,0,'可约',0),
(85,3,5,'2026-10-27','13:30:00','15:30:00',1,0,'可约',0),
(86,1,1,'2026-10-28','08:00:00','10:00:00',1,0,'可约',0),
(87,1,2,'2026-10-28','10:00:00','12:00:00',1,0,'可约',0),
(88,1,1,'2026-10-28','13:30:00','15:30:00',1,0,'可约',0),
(89,1,2,'2026-10-28','15:30:00','17:30:00',1,0,'可约',0),
(90,2,4,'2026-10-28','10:00:00','12:00:00',1,0,'可约',0),
(91,2,4,'2026-10-28','13:30:00','15:30:00',1,0,'可约',0),
(92,3,5,'2026-10-28','08:00:00','10:00:00',1,0,'可约',0),
(93,3,5,'2026-10-28','10:00:00','12:00:00',1,0,'可约',0),
(94,3,5,'2026-10-28','15:30:00','17:30:00',1,0,'可约',0),
(95,1,1,'2026-10-29','08:00:00','10:00:00',1,1,'已满',0),
(96,1,2,'2026-10-29','10:00:00','12:00:00',1,0,'可约',0),
(97,1,1,'2026-10-29','13:30:00','15:30:00',1,0,'可约',0),
(98,1,2,'2026-10-29','15:30:00','17:30:00',1,0,'可约',0),
(99,2,4,'2026-10-29','08:00:00','10:00:00',1,0,'可约',0),
(100,2,4,'2026-10-29','10:00:00','12:00:00',1,0,'可约',0),
(101,2,4,'2026-10-29','15:30:00','17:30:00',1,0,'可约',0),
(102,3,5,'2026-10-29','08:00:00','10:00:00',1,0,'可约',0),
(103,3,5,'2026-10-29','13:30:00','15:30:00',1,0,'可约',0),
(104,3,5,'2026-10-29','15:30:00','17:30:00',1,0,'可约',0),
(105,1,1,'2026-10-30','08:00:00','10:00:00',1,0,'可约',0),
(106,1,2,'2026-10-30','10:00:00','12:00:00',1,0,'可约',0),
(107,1,1,'2026-10-30','13:30:00','15:30:00',1,0,'可约',0),
(108,1,2,'2026-10-30','15:30:00','17:30:00',1,0,'可约',0),
(109,2,4,'2026-10-30','08:00:00','10:00:00',1,0,'可约',0),
(110,2,4,'2026-10-30','13:30:00','15:30:00',1,0,'可约',0),
(111,2,4,'2026-10-30','15:30:00','17:30:00',1,0,'可约',0),
(112,3,5,'2026-10-30','10:00:00','12:00:00',1,0,'可约',0),
(113,3,5,'2026-10-30','13:30:00','15:30:00',1,0,'可约',0),
(114,1,1,'2026-10-31','08:00:00','10:00:00',1,0,'可约',0),
(115,1,2,'2026-10-31','10:00:00','12:00:00',1,0,'可约',0),
(116,1,1,'2026-10-31','13:30:00','15:30:00',1,0,'可约',0),
(117,1,2,'2026-10-31','15:30:00','17:30:00',1,0,'可约',0),
(118,2,4,'2026-10-31','10:00:00','12:00:00',1,0,'可约',0),
(119,2,4,'2026-10-31','13:30:00','15:30:00',1,0,'可约',0),
(120,3,5,'2026-10-31','08:00:00','10:00:00',1,0,'可约',0),
(121,3,5,'2026-10-31','10:00:00','12:00:00',1,0,'可约',0),
(122,3,5,'2026-10-31','15:30:00','17:30:00',1,0,'可约',0);
/*!40000 ALTER TABLE `coach_schedule` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `exam_plan`
--

DROP TABLE IF EXISTS `exam_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_code` varchar(10) NOT NULL COMMENT '科目代码',
  `exam_date` date NOT NULL COMMENT '考试日期',
  `exam_time` time DEFAULT NULL COMMENT '考试时间',
  `venue` varchar(100) NOT NULL COMMENT '考场',
  `capacity` int(11) NOT NULL COMMENT '容量',
  `enrolled_count` int(11) NOT NULL DEFAULT 0 COMMENT '已报名人数（冗余缓存）',
  `status` varchar(10) NOT NULL DEFAULT '报名中' COMMENT '报名中 / 已截止 / 已完成',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='考试计划';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_plan`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `exam_plan` DISABLE KEYS */;
INSERT INTO `exam_plan` (`id`, `subject_code`, `exam_date`, `exam_time`, `venue`, `capacity`, `enrolled_count`, `status`, `del_flag`) VALUES (1,'1','2026-09-07','09:00:00','第一理论考场',60,6,'已完成',0),
(2,'2','2026-10-02','08:30:00','场地驾驶考场 A',30,5,'已完成',0),
(3,'1','2026-10-23','09:00:00','第一理论考场',60,4,'报名中',0),
(4,'2','2026-10-26','08:30:00','场地驾驶考场 A',30,1,'报名中',0),
(5,'3','2026-10-31','08:00:00','道路驾驶考场',40,1,'报名中',0),
(6,'4','2026-11-06','10:00:00','第二理论考场',50,1,'报名中',0);
/*!40000 ALTER TABLE `exam_plan` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `exam_record`
--

DROP TABLE IF EXISTS `exam_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id` bigint(20) NOT NULL COMMENT '考试计划',
  `student_id` bigint(20) NOT NULL COMMENT '学员',
  `register_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '报名时间',
  `attempt_no` int(11) NOT NULL DEFAULT 1 COMMENT '第几次（补考递增）',
  `score` decimal(4,1) DEFAULT NULL COMMENT '成绩',
  `status` varchar(10) NOT NULL DEFAULT '待考' COMMENT '待考 / 合格 / 不合格 / 补考中',
  `exam_date` date DEFAULT NULL COMMENT '实际考试日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_student` (`plan_id`,`student_id`),
  KEY `fk_exam_student` (`student_id`),
  CONSTRAINT `fk_exam_plan` FOREIGN KEY (`plan_id`) REFERENCES `exam_plan` (`id`),
  CONSTRAINT `fk_exam_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='考试报名与成绩';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `exam_record` DISABLE KEYS */;
INSERT INTO `exam_record` (`id`, `plan_id`, `student_id`, `register_time`, `attempt_no`, `score`, `status`, `exam_date`) VALUES (1,1,1,'2026-10-17 15:11:43',1,96.0,'合格','2026-09-07'),
(2,2,1,'2026-10-17 15:11:43',1,92.0,'合格','2026-10-02'),
(3,1,2,'2026-10-17 15:11:43',1,88.0,'合格','2026-09-07'),
(4,2,2,'2026-10-17 15:11:43',1,71.0,'补考中','2026-10-02'),
(5,4,2,'2026-10-17 15:11:43',2,NULL,'待考','2026-10-26'),
(6,1,3,'2026-10-17 15:11:43',1,95.0,'合格','2026-09-07'),
(7,2,3,'2026-10-17 15:11:43',1,91.0,'合格','2026-10-02'),
(8,1,4,'2026-10-17 15:11:43',1,82.0,'合格','2026-09-07'),
(9,1,5,'2026-10-17 15:11:43',1,94.0,'合格','2026-09-07'),
(10,2,5,'2026-10-17 15:11:43',1,90.0,'合格','2026-10-02'),
(11,5,5,'2026-10-17 15:11:43',1,88.0,'合格','2026-10-31'),
(12,6,5,'2026-10-17 15:11:43',1,93.0,'合格','2026-11-06'),
(13,1,11,'2026-10-17 15:11:43',1,97.0,'合格','2026-09-07'),
(14,2,11,'2026-10-17 15:11:43',1,85.0,'合格','2026-10-02'),
(15,3,3,'2026-10-17 15:11:43',1,NULL,'待考',NULL),
(16,3,9,'2026-10-17 15:11:43',1,NULL,'待考',NULL),
(17,3,12,'2026-10-17 15:11:43',1,NULL,'待考',NULL),
(18,3,15,'2026-10-17 15:11:43',1,NULL,'待考',NULL);
/*!40000 ALTER TABLE `exam_record` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `hours_record`
--

DROP TABLE IF EXISTS `hours_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hours_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) NOT NULL COMMENT '学员',
  `coach_id` bigint(20) NOT NULL COMMENT '登记教练',
  `booking_id` bigint(20) DEFAULT NULL COMMENT '关联预约',
  `subject_code` varchar(10) NOT NULL COMMENT '科目代码',
  `hours` decimal(4,1) NOT NULL COMMENT '本次学时',
  `train_date` date NOT NULL COMMENT '培训日期',
  `audit_status` varchar(10) NOT NULL DEFAULT '待审核' COMMENT '待审核 / 已通过 / 已驳回',
  `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人',
  `audit_opinion` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_student_subject` (`student_id`,`subject_code`),
  KEY `fk_hours_coach` (`coach_id`),
  KEY `fk_hours_booking` (`booking_id`),
  KEY `fk_hours_auditor` (`auditor_id`),
  CONSTRAINT `fk_hours_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_hours_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`),
  CONSTRAINT `fk_hours_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`),
  CONSTRAINT `fk_hours_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=226 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='学时记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hours_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `hours_record` DISABLE KEYS */;
INSERT INTO `hours_record` (`id`, `student_id`, `coach_id`, `booking_id`, `subject_code`, `hours`, `train_date`, `audit_status`, `auditor_id`, `audit_opinion`, `audit_time`) VALUES (1,1,1,NULL,'1',2.0,'2026-08-28','已通过',NULL,NULL,NULL),
(2,1,1,NULL,'1',2.0,'2026-08-31','已通过',NULL,NULL,NULL),
(3,1,1,NULL,'1',2.0,'2026-09-03','已通过',NULL,NULL,NULL),
(4,1,1,NULL,'1',2.0,'2026-09-06','已通过',NULL,NULL,NULL),
(5,1,1,NULL,'1',2.0,'2026-09-09','已通过',NULL,NULL,NULL),
(6,1,1,NULL,'1',2.0,'2026-09-12','已通过',NULL,NULL,NULL),
(7,1,1,NULL,'2',2.0,'2026-09-27','已通过',NULL,NULL,NULL),
(8,1,1,NULL,'2',2.0,'2026-09-30','已通过',NULL,NULL,NULL),
(9,1,1,NULL,'2',2.0,'2026-10-03','已通过',NULL,NULL,NULL),
(10,1,1,NULL,'2',2.0,'2026-10-06','已通过',NULL,NULL,NULL),
(11,1,1,NULL,'2',2.0,'2026-10-09','已通过',NULL,NULL,NULL),
(12,1,1,NULL,'2',2.0,'2026-10-12','已通过',NULL,NULL,NULL),
(13,1,1,NULL,'2',2.0,'2026-10-15','已通过',NULL,NULL,NULL),
(14,1,1,NULL,'2',2.0,'2026-10-18','已通过',NULL,NULL,NULL),
(15,1,1,NULL,'2',2.0,'2026-10-21','已通过',NULL,NULL,NULL),
(16,1,1,NULL,'2',2.0,'2026-10-24','已通过',NULL,NULL,NULL),
(17,1,1,NULL,'3',2.0,'2026-10-07','已通过',NULL,NULL,NULL),
(18,1,1,NULL,'3',2.0,'2026-10-10','已通过',NULL,NULL,NULL),
(19,1,1,NULL,'3',2.0,'2026-10-13','已通过',NULL,NULL,NULL),
(20,1,1,NULL,'3',2.0,'2026-10-16','已通过',NULL,NULL,NULL),
(21,1,1,NULL,'3',2.0,'2026-10-19','已通过',NULL,NULL,NULL),
(22,1,1,NULL,'3',2.0,'2026-10-16','待审核',NULL,NULL,NULL),
(23,2,1,NULL,'1',2.0,'2026-08-20','已通过',NULL,NULL,NULL),
(24,2,1,NULL,'1',2.0,'2026-08-23','已通过',NULL,NULL,NULL),
(25,2,1,NULL,'1',2.0,'2026-08-26','已通过',NULL,NULL,NULL),
(26,2,1,NULL,'1',0.5,'2026-08-29','已通过',NULL,NULL,NULL),
(27,2,1,NULL,'2',2.0,'2026-09-01','已通过',NULL,NULL,NULL),
(28,2,1,NULL,'2',2.0,'2026-09-04','已通过',NULL,NULL,NULL),
(29,2,1,NULL,'2',2.0,'2026-09-07','已通过',NULL,NULL,NULL),
(30,2,1,NULL,'2',2.0,'2026-09-10','已通过',NULL,NULL,NULL),
(31,2,1,NULL,'2',2.0,'2026-09-13','已通过',NULL,NULL,NULL),
(32,2,1,NULL,'2',2.0,'2026-09-16','已通过',NULL,NULL,NULL),
(33,2,1,NULL,'2',2.0,'2026-09-19','已通过',NULL,NULL,NULL),
(34,2,1,NULL,'2',1.0,'2026-09-22','已通过',NULL,NULL,NULL),
(35,2,1,NULL,'3',2.0,'2026-09-13','已通过',NULL,NULL,NULL),
(36,2,1,NULL,'3',2.0,'2026-09-16','已通过',NULL,NULL,NULL),
(37,2,1,NULL,'3',2.0,'2026-09-19','已通过',NULL,NULL,NULL),
(38,2,1,NULL,'3',2.0,'2026-09-22','已通过',NULL,NULL,NULL),
(39,2,1,NULL,'3',2.0,'2026-09-25','已通过',NULL,NULL,NULL),
(40,2,1,NULL,'3',2.0,'2026-09-28','已通过',NULL,NULL,NULL),
(41,2,1,NULL,'3',2.0,'2026-10-01','已通过',NULL,NULL,NULL),
(42,2,1,NULL,'4',2.0,'2026-09-24','已通过',NULL,NULL,NULL),
(43,2,1,NULL,'4',2.0,'2026-09-27','已通过',NULL,NULL,NULL),
(44,2,1,NULL,'4',2.0,'2026-09-30','已通过',NULL,NULL,NULL),
(45,3,1,NULL,'1',2.0,'2026-08-26','已通过',NULL,NULL,NULL),
(46,3,1,NULL,'1',2.0,'2026-08-29','已通过',NULL,NULL,NULL),
(47,3,1,NULL,'1',1.5,'2026-09-01','已通过',NULL,NULL,NULL),
(48,3,1,NULL,'2',2.0,'2026-09-06','已通过',NULL,NULL,NULL),
(49,3,1,NULL,'2',2.0,'2026-09-09','已通过',NULL,NULL,NULL),
(50,3,1,NULL,'2',2.0,'2026-09-12','已通过',NULL,NULL,NULL),
(51,3,1,NULL,'2',2.0,'2026-09-15','已通过',NULL,NULL,NULL),
(52,3,1,NULL,'2',2.0,'2026-09-18','已通过',NULL,NULL,NULL),
(53,3,1,NULL,'2',2.0,'2026-09-21','已通过',NULL,NULL,NULL),
(54,3,1,NULL,'2',1.5,'2026-09-24','已通过',NULL,NULL,NULL),
(55,3,1,NULL,'3',2.0,'2026-09-16','已通过',NULL,NULL,NULL),
(56,3,1,NULL,'3',2.0,'2026-09-19','已通过',NULL,NULL,NULL),
(57,3,1,NULL,'3',2.0,'2026-09-22','已通过',NULL,NULL,NULL),
(58,3,1,NULL,'3',2.0,'2026-09-25','已通过',NULL,NULL,NULL),
(59,3,1,NULL,'3',2.0,'2026-09-28','已通过',NULL,NULL,NULL),
(60,3,1,NULL,'3',2.0,'2026-10-01','已通过',NULL,NULL,NULL),
(61,3,1,NULL,'3',0.5,'2026-10-04','已通过',NULL,NULL,NULL),
(62,3,1,NULL,'4',2.0,'2026-09-27','已通过',NULL,NULL,NULL),
(63,3,1,NULL,'4',2.0,'2026-09-30','已通过',NULL,NULL,NULL),
(64,3,1,NULL,'4',1.0,'2026-10-03','已通过',NULL,NULL,NULL),
(65,4,1,NULL,'1',2.0,'2026-09-02','已通过',NULL,NULL,NULL),
(66,4,1,NULL,'1',2.0,'2026-09-05','已通过',NULL,NULL,NULL),
(67,4,1,NULL,'1',1.0,'2026-09-08','已通过',NULL,NULL,NULL),
(68,4,1,NULL,'2',2.0,'2026-09-11','已通过',NULL,NULL,NULL),
(69,4,1,NULL,'2',2.0,'2026-09-14','已通过',NULL,NULL,NULL),
(70,4,1,NULL,'2',2.0,'2026-09-17','已通过',NULL,NULL,NULL),
(71,4,1,NULL,'2',2.0,'2026-09-20','已通过',NULL,NULL,NULL),
(72,4,1,NULL,'2',2.0,'2026-09-23','已通过',NULL,NULL,NULL),
(73,4,1,NULL,'2',1.5,'2026-09-26','已通过',NULL,NULL,NULL),
(74,4,1,NULL,'3',2.0,'2026-09-20','已通过',NULL,NULL,NULL),
(75,4,1,NULL,'3',2.0,'2026-09-23','已通过',NULL,NULL,NULL),
(76,4,1,NULL,'3',2.0,'2026-09-26','已通过',NULL,NULL,NULL),
(77,4,1,NULL,'3',2.0,'2026-09-29','已通过',NULL,NULL,NULL),
(78,4,1,NULL,'3',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(79,4,1,NULL,'3',1.0,'2026-10-05','已通过',NULL,NULL,NULL),
(80,4,1,NULL,'4',2.0,'2026-09-30','已通过',NULL,NULL,NULL),
(81,4,1,NULL,'4',2.0,'2026-10-03','已通过',NULL,NULL,NULL),
(82,4,1,NULL,'4',0.5,'2026-10-06','已通过',NULL,NULL,NULL),
(83,4,1,NULL,'2',2.0,'2026-10-16','待审核',NULL,NULL,NULL),
(84,5,1,NULL,'1',2.0,'2026-06-19','已通过',NULL,NULL,NULL),
(85,5,1,NULL,'1',2.0,'2026-06-22','已通过',NULL,NULL,NULL),
(86,5,1,NULL,'1',2.0,'2026-06-25','已通过',NULL,NULL,NULL),
(87,5,1,NULL,'1',2.0,'2026-06-28','已通过',NULL,NULL,NULL),
(88,5,1,NULL,'1',2.0,'2026-07-01','已通过',NULL,NULL,NULL),
(89,5,1,NULL,'1',2.0,'2026-07-04','已通过',NULL,NULL,NULL),
(90,5,1,NULL,'2',2.0,'2026-07-13','已通过',NULL,NULL,NULL),
(91,5,1,NULL,'2',2.0,'2026-07-16','已通过',NULL,NULL,NULL),
(92,5,1,NULL,'2',2.0,'2026-07-19','已通过',NULL,NULL,NULL),
(93,5,1,NULL,'2',2.0,'2026-07-22','已通过',NULL,NULL,NULL),
(94,5,1,NULL,'2',2.0,'2026-07-25','已通过',NULL,NULL,NULL),
(95,5,1,NULL,'2',2.0,'2026-07-28','已通过',NULL,NULL,NULL),
(96,5,1,NULL,'2',2.0,'2026-07-31','已通过',NULL,NULL,NULL),
(97,5,1,NULL,'2',2.0,'2026-08-03','已通过',NULL,NULL,NULL),
(98,5,1,NULL,'2',2.0,'2026-08-06','已通过',NULL,NULL,NULL),
(99,5,1,NULL,'2',2.0,'2026-08-09','已通过',NULL,NULL,NULL),
(100,5,1,NULL,'2',2.0,'2026-08-12','已通过',NULL,NULL,NULL),
(101,5,1,NULL,'2',2.0,'2026-08-15','已通过',NULL,NULL,NULL),
(102,5,1,NULL,'2',2.0,'2026-08-18','已通过',NULL,NULL,NULL),
(103,5,1,NULL,'3',2.0,'2026-08-06','已通过',NULL,NULL,NULL),
(104,5,1,NULL,'3',2.0,'2026-08-09','已通过',NULL,NULL,NULL),
(105,5,1,NULL,'3',2.0,'2026-08-12','已通过',NULL,NULL,NULL),
(106,5,1,NULL,'3',2.0,'2026-08-15','已通过',NULL,NULL,NULL),
(107,5,1,NULL,'3',2.0,'2026-08-18','已通过',NULL,NULL,NULL),
(108,5,1,NULL,'3',2.0,'2026-08-21','已通过',NULL,NULL,NULL),
(109,5,1,NULL,'3',2.0,'2026-08-24','已通过',NULL,NULL,NULL),
(110,5,1,NULL,'3',2.0,'2026-08-27','已通过',NULL,NULL,NULL),
(111,5,1,NULL,'3',2.0,'2026-08-30','已通过',NULL,NULL,NULL),
(112,5,1,NULL,'3',2.0,'2026-09-02','已通过',NULL,NULL,NULL),
(113,5,1,NULL,'3',2.0,'2026-09-05','已通过',NULL,NULL,NULL),
(114,5,1,NULL,'3',2.0,'2026-09-08','已通过',NULL,NULL,NULL),
(115,5,1,NULL,'4',2.0,'2026-08-31','已通过',NULL,NULL,NULL),
(116,5,1,NULL,'4',2.0,'2026-09-03','已通过',NULL,NULL,NULL),
(117,5,1,NULL,'4',2.0,'2026-09-06','已通过',NULL,NULL,NULL),
(118,5,1,NULL,'4',2.0,'2026-09-09','已通过',NULL,NULL,NULL),
(119,5,1,NULL,'4',2.0,'2026-09-12','已通过',NULL,NULL,NULL),
(120,7,2,NULL,'1',2.0,'2026-09-07','已通过',NULL,NULL,NULL),
(121,7,2,NULL,'1',2.0,'2026-09-10','已通过',NULL,NULL,NULL),
(122,7,2,NULL,'1',0.5,'2026-09-13','已通过',NULL,NULL,NULL),
(123,7,2,NULL,'2',2.0,'2026-09-15','已通过',NULL,NULL,NULL),
(124,7,2,NULL,'2',2.0,'2026-09-18','已通过',NULL,NULL,NULL),
(125,7,2,NULL,'2',2.0,'2026-09-21','已通过',NULL,NULL,NULL),
(126,7,2,NULL,'2',2.0,'2026-09-24','已通过',NULL,NULL,NULL),
(127,7,2,NULL,'2',2.0,'2026-09-27','已通过',NULL,NULL,NULL),
(128,7,2,NULL,'2',0.5,'2026-09-30','已通过',NULL,NULL,NULL),
(129,7,2,NULL,'3',2.0,'2026-09-23','已通过',NULL,NULL,NULL),
(130,7,2,NULL,'3',2.0,'2026-09-26','已通过',NULL,NULL,NULL),
(131,7,2,NULL,'3',2.0,'2026-09-29','已通过',NULL,NULL,NULL),
(132,7,2,NULL,'3',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(133,7,2,NULL,'3',1.5,'2026-10-05','已通过',NULL,NULL,NULL),
(134,7,2,NULL,'4',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(135,7,2,NULL,'4',2.0,'2026-10-05','已通过',NULL,NULL,NULL),
(136,8,2,NULL,'1',2.0,'2026-09-12','已通过',NULL,NULL,NULL),
(137,8,2,NULL,'1',2.0,'2026-09-15','已通过',NULL,NULL,NULL),
(138,8,2,NULL,'2',2.0,'2026-09-19','已通过',NULL,NULL,NULL),
(139,8,2,NULL,'2',2.0,'2026-09-22','已通过',NULL,NULL,NULL),
(140,8,2,NULL,'2',2.0,'2026-09-25','已通过',NULL,NULL,NULL),
(141,8,2,NULL,'2',2.0,'2026-09-28','已通过',NULL,NULL,NULL),
(142,8,2,NULL,'2',1.0,'2026-10-01','已通过',NULL,NULL,NULL),
(143,8,2,NULL,'3',2.0,'2026-09-26','已通过',NULL,NULL,NULL),
(144,8,2,NULL,'3',2.0,'2026-09-29','已通过',NULL,NULL,NULL),
(145,8,2,NULL,'3',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(146,8,2,NULL,'3',2.0,'2026-10-05','已通过',NULL,NULL,NULL),
(147,8,2,NULL,'3',0.5,'2026-10-08','已通过',NULL,NULL,NULL),
(148,8,2,NULL,'4',2.0,'2026-10-04','已通过',NULL,NULL,NULL),
(149,8,2,NULL,'4',1.5,'2026-10-07','已通过',NULL,NULL,NULL),
(150,8,2,NULL,'2',2.0,'2026-10-16','待审核',NULL,NULL,NULL),
(151,9,3,NULL,'1',2.0,'2026-09-17','已通过',NULL,NULL,NULL),
(152,9,3,NULL,'1',1.0,'2026-09-20','已通过',NULL,NULL,NULL),
(153,9,3,NULL,'2',2.0,'2026-09-23','已通过',NULL,NULL,NULL),
(154,9,3,NULL,'2',2.0,'2026-09-26','已通过',NULL,NULL,NULL),
(155,9,3,NULL,'2',2.0,'2026-09-29','已通过',NULL,NULL,NULL),
(156,9,3,NULL,'2',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(157,9,3,NULL,'3',2.0,'2026-09-29','已通过',NULL,NULL,NULL),
(158,9,3,NULL,'3',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(159,9,3,NULL,'3',2.0,'2026-10-05','已通过',NULL,NULL,NULL),
(160,9,3,NULL,'3',1.0,'2026-10-08','已通过',NULL,NULL,NULL),
(161,9,3,NULL,'4',2.0,'2026-10-06','已通过',NULL,NULL,NULL),
(162,9,3,NULL,'4',1.0,'2026-10-09','已通过',NULL,NULL,NULL),
(163,11,1,NULL,'1',2.0,'2026-09-22','已通过',NULL,NULL,NULL),
(164,11,1,NULL,'1',0.5,'2026-09-25','已通过',NULL,NULL,NULL),
(165,11,1,NULL,'2',2.0,'2026-09-27','已通过',NULL,NULL,NULL),
(166,11,1,NULL,'2',2.0,'2026-09-30','已通过',NULL,NULL,NULL),
(167,11,1,NULL,'2',2.0,'2026-10-03','已通过',NULL,NULL,NULL),
(168,11,1,NULL,'2',0.5,'2026-10-06','已通过',NULL,NULL,NULL),
(169,11,1,NULL,'3',2.0,'2026-10-02','已通过',NULL,NULL,NULL),
(170,11,1,NULL,'3',2.0,'2026-10-05','已通过',NULL,NULL,NULL),
(171,11,1,NULL,'3',2.0,'2026-10-08','已通过',NULL,NULL,NULL),
(172,11,1,NULL,'4',2.0,'2026-10-08','已通过',NULL,NULL,NULL),
(173,11,1,NULL,'4',0.5,'2026-10-11','已通过',NULL,NULL,NULL),
(174,12,3,NULL,'1',2.0,'2026-08-08','已通过',NULL,NULL,NULL),
(175,12,3,NULL,'1',2.0,'2026-08-11','已通过',NULL,NULL,NULL),
(176,12,3,NULL,'1',2.0,'2026-08-14','已通过',NULL,NULL,NULL),
(177,12,3,NULL,'1',1.5,'2026-08-17','已通过',NULL,NULL,NULL),
(178,12,3,NULL,'2',2.0,'2026-08-22','已通过',NULL,NULL,NULL),
(179,12,3,NULL,'2',2.0,'2026-08-25','已通过',NULL,NULL,NULL),
(180,12,3,NULL,'2',2.0,'2026-08-28','已通过',NULL,NULL,NULL),
(181,12,3,NULL,'2',2.0,'2026-08-31','已通过',NULL,NULL,NULL),
(182,12,3,NULL,'2',2.0,'2026-09-03','已通过',NULL,NULL,NULL),
(183,12,3,NULL,'2',2.0,'2026-09-06','已通过',NULL,NULL,NULL),
(184,12,3,NULL,'2',2.0,'2026-09-09','已通过',NULL,NULL,NULL),
(185,12,3,NULL,'2',2.0,'2026-09-12','已通过',NULL,NULL,NULL),
(186,12,3,NULL,'2',2.0,'2026-09-15','已通过',NULL,NULL,NULL),
(187,12,3,NULL,'3',2.0,'2026-09-05','已通过',NULL,NULL,NULL),
(188,12,3,NULL,'3',2.0,'2026-09-08','已通过',NULL,NULL,NULL),
(189,12,3,NULL,'3',2.0,'2026-09-11','已通过',NULL,NULL,NULL),
(190,12,3,NULL,'3',2.0,'2026-09-14','已通过',NULL,NULL,NULL),
(191,12,3,NULL,'3',2.0,'2026-09-17','已通过',NULL,NULL,NULL),
(192,12,3,NULL,'3',2.0,'2026-09-20','已通过',NULL,NULL,NULL),
(193,12,3,NULL,'3',2.0,'2026-09-23','已通过',NULL,NULL,NULL),
(194,12,3,NULL,'3',2.0,'2026-09-26','已通过',NULL,NULL,NULL),
(195,12,3,NULL,'3',1.0,'2026-09-29','已通过',NULL,NULL,NULL),
(196,12,3,NULL,'4',2.0,'2026-09-20','已通过',NULL,NULL,NULL),
(197,12,3,NULL,'4',2.0,'2026-09-23','已通过',NULL,NULL,NULL),
(198,12,3,NULL,'4',2.0,'2026-09-26','已通过',NULL,NULL,NULL),
(199,12,3,NULL,'4',1.0,'2026-09-29','已通过',NULL,NULL,NULL),
(200,12,3,NULL,'2',2.0,'2026-10-16','待审核',NULL,NULL,NULL),
(201,13,1,NULL,'1',2.0,'2026-09-29','已通过',NULL,NULL,NULL),
(202,13,1,NULL,'2',2.0,'2026-10-03','已通过',NULL,NULL,NULL),
(203,13,1,NULL,'2',2.0,'2026-10-06','已通过',NULL,NULL,NULL),
(204,13,1,NULL,'2',0.5,'2026-10-09','已通过',NULL,NULL,NULL),
(205,13,1,NULL,'3',2.0,'2026-10-07','已通过',NULL,NULL,NULL),
(206,13,1,NULL,'3',2.0,'2026-10-10','已通过',NULL,NULL,NULL),
(207,13,1,NULL,'3',0.5,'2026-10-13','已通过',NULL,NULL,NULL),
(208,13,1,NULL,'4',2.0,'2026-10-10','已通过',NULL,NULL,NULL),
(209,14,2,NULL,'1',1.5,'2026-10-05','已通过',NULL,NULL,NULL),
(210,14,2,NULL,'2',2.0,'2026-10-08','已通过',NULL,NULL,NULL),
(211,14,2,NULL,'2',1.0,'2026-10-11','已通过',NULL,NULL,NULL),
(212,14,2,NULL,'3',2.0,'2026-10-10','已通过',NULL,NULL,NULL),
(213,14,2,NULL,'3',1.0,'2026-10-13','已通过',NULL,NULL,NULL),
(214,14,2,NULL,'4',1.0,'2026-10-13','已通过',NULL,NULL,NULL),
(215,15,3,NULL,'1',1.0,'2026-10-09','已通过',NULL,NULL,NULL),
(216,15,3,NULL,'2',2.0,'2026-10-11','已通过',NULL,NULL,NULL),
(217,15,3,NULL,'3',2.0,'2026-10-13','已通过',NULL,NULL,NULL),
(218,15,3,NULL,'4',1.0,'2026-10-14','已通过',NULL,NULL,NULL),
(219,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:18:58'),
(220,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:19:34'),
(221,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:19:41'),
(222,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:19:42'),
(223,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:19:48'),
(224,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:19:52'),
(225,1,1,NULL,'3',2.0,'2026-10-15','已通过',1,'','2026-10-17 15:28:22');
/*!40000 ALTER TABLE `hours_record` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` bigint(20) NOT NULL COMMENT '学员',
  `item` varchar(20) NOT NULL COMMENT '培训费 / 补考费 / 其它',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `pay_time` datetime DEFAULT NULL COMMENT '缴费时间',
  `pay_method` varchar(20) DEFAULT NULL COMMENT '现金 / 微信 / 支付宝 / 银行',
  `status` varchar(10) NOT NULL DEFAULT '未缴' COMMENT '未缴 / 已缴 / 退费',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '经办人',
  PRIMARY KEY (`id`),
  KEY `idx_pay_student` (`student_id`),
  KEY `fk_pay_operator` (`operator_id`),
  CONSTRAINT `fk_pay_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_pay_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='缴费记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
INSERT INTO `payment` (`id`, `student_id`, `item`, `amount`, `pay_time`, `pay_method`, `status`, `operator_id`) VALUES (1,1,'培训费',5800.00,'2026-09-16 15:11:43','支付宝','已缴',1),
(2,2,'培训费',5800.00,'2026-09-15 15:11:43','银行','已缴',1),
(3,3,'培训费',5800.00,'2026-09-14 15:11:43','现金','已缴',1),
(4,4,'培训费',5800.00,'2026-09-13 15:11:43','微信','已缴',1),
(5,5,'培训费',5800.00,'2026-09-12 15:11:43','支付宝','已缴',1),
(6,6,'培训费',5800.00,NULL,NULL,'未缴',1),
(7,7,'培训费',5800.00,'2026-09-10 15:11:43','现金','已缴',1),
(8,8,'培训费',5800.00,'2026-09-09 15:11:43','微信','已缴',1),
(9,9,'培训费',5800.00,'2026-09-08 15:11:43','支付宝','已缴',1),
(10,10,'培训费',8800.00,'2026-07-19 15:11:43','银行','退费',1),
(11,11,'培训费',5800.00,'2026-09-06 15:11:43','现金','已缴',1),
(12,12,'培训费',5800.00,'2026-09-05 15:11:43','微信','已缴',1),
(13,13,'培训费',5800.00,'2026-09-04 15:11:43','支付宝','已缴',1),
(14,14,'培训费',5800.00,'2026-09-03 15:11:43','银行','已缴',1),
(15,15,'培训费',5800.00,'2026-09-02 15:11:43','现金','已缴',1),
(16,16,'培训费',5800.00,NULL,NULL,'未缴',1);
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '关联登录账号',
  `id_card` varchar(128) NOT NULL COMMENT '身份证号（AES 密文）',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `license_type` varchar(10) NOT NULL COMMENT '准驾车型',
  `enroll_date` date DEFAULT NULL COMMENT '报名日期',
  `status` varchar(10) NOT NULL DEFAULT '待审核' COMMENT '待审核 / 在训 / 结业 / 退学',
  `coach_id` bigint(20) DEFAULT NULL COMMENT '固定带教教练',
  `vehicle_id` bigint(20) DEFAULT NULL COMMENT '固定带教车辆',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`),
  KEY `idx_status` (`status`),
  KEY `idx_coach` (`coach_id`),
  KEY `fk_student_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_student_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`),
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_student_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='学员档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` (`id`, `user_id`, `id_card`, `phone`, `license_type`, `enroll_date`, `status`, `coach_id`, `vehicle_id`, `del_flag`) VALUES (1,3,'v1:TuWbGOxq8H6eAp9PfG+Yz80cUHbVRhf2rzqgN4p0y91kUr+uwuGMHyVBUaQvnQ==','13900002001','C1','2026-08-18','在训',1,1,0),
(2,6,'v1:7OqBpPIeRj//AuDSaiiFnspWnSwXR7jI/j0d4VnB+dJaQbYoa/HB5DlSVB7kCw==','13900002002','C1','2026-08-20','在训',1,1,0),
(3,7,'v1:zjcNQJOiyzuBB61FDfoweB3aUPVbst63vND/A8jvYhK0bxcUrH2OLZhfB+SPhg==','13900002003','C2','2026-08-26','在训',1,1,0),
(4,8,'v1:LO4xOQtRR+5MTIPpw3MqoOsrpkQkSR++MHwkhCWW7p2kagZVdIVahGdm8EqqaQ==','13900002004','C1','2026-09-02','在训',1,1,0),
(5,9,'v1:FGPCRkVvbtpBcqeUKTtvupqbhpZaFl6xwsh2XESUKLVHo4dnoV6NLMVaEGSjjw==','13900002005','C1','2026-06-19','结业',1,1,0),
(6,10,'v1:ZI0NmbSx6klP3f+jxHu4gP6c53WLn03tIy+nNYsAuIY8ajve1/fUaWtADS5Jxg==','13900002006','B2','2026-10-14','待审核',1,1,0),
(7,11,'v1:gvzTi6D+WA3dmxQzMVxzwRSoJYhpDnq9l97dWH085HJEXOtbdsRZEMTLY2GK3w==','13900002007','C1','2026-09-07','在训',2,4,0),
(8,12,'v1:mMMafJWFP8TXlgbnJozn7n4zyiQvH2XREdJEoxs55rZjXH3PSaB1jrTOdXF0cw==','13900002008','C2','2026-09-12','在训',2,4,0),
(9,13,'v1:ytW2H1kapUSL9Xa5GFuaUTwXKOZi41DQldXajZNSAQi0IARnyr/KAjTa06qXGQ==','13900002009','C1','2026-09-17','在训',3,5,0),
(10,14,'v1:V43s107E6lqnH50CGXmF2PKJ5HHl8Bd+Bo6wAKtCpLBmYvnMOWqhGJsKaPf8/A==','13900002010','B2','2026-07-19','退学',3,5,0),
(11,15,'v1:6NOVd6Ecgf4rm6IWn1SsyWw4FwujW+MD01J9E2qnPznRlqVhHVBpuMuoY8/zEg==','13900002011','C1','2026-09-22','在训',1,1,0),
(12,16,'v1:+nTwyafFmmlslSYok7m+aF5dxr0yDIG9MNR5PsltsoZ7GjLUHDXSJyoVfmV7BQ==','13900002012','A2','2026-08-08','在训',3,5,0),
(13,17,'v1:U3e3dsKdSswiMU3Sx/CevbhzKK9F78W1hTHYSIR1CgzSzL/nRBfZOUwReFfqjg==','13900002013','C1','2026-09-29','在训',1,1,0),
(14,18,'v1:u2TUGh2CRFYgr5Uqp8Q83RFzYm+ekEX85fma267cl2KLF4ntuyxFGKcWGFtmzg==','13900002014','C1','2026-10-05','在训',2,4,0),
(15,19,'v1:63e57ELEeEnTApsVbAp3KomEGw7vmm4soE1hNqBFhslwokoXmUTLMHqG0EO9wA==','13900002015','C2','2026-10-09','在训',3,5,0),
(16,20,'v1:7p5gWn+0D/A+WBaG2aB2pVz9g5RFm/NSkPSIXWL2+zpPASLhUcoOptgR7NnoZQ==','13900002016','C1','2026-10-15','待审核',1,1,0),
(17,21,'v1:xSC+YAaJsxulcR59p2mbz9n5Y9nRHaan/1WsfbLlqCA05Nq0ESVxnjG9R+Gqyg==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0),
(18,23,'v1:CcaAA4Ivn2A/En25wg/MRajtRw/l/CPdg/pBn6kALSLJWFU9sb8JdOH4cC11rA==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0),
(19,25,'v1:eBF09TPg0rMrp/Do/Ez/aPkyaFXwbfk3Onypq2h1qccTMkjPi6KgsPrDOK4yXA==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0),
(20,27,'v1:5az4xVv8P/KUJALfsp2yvrElOu5YXiHgfbucGyH+tDE8iPaD38KdnOYQOx7R8A==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0),
(21,29,'v1:v2HFqDZO7ocT2WCBvC5z0vVXGrniRDmcXmpy8Ec+6tDECMmTWacaENqyyHobKw==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0),
(22,31,'v1:Z/3WchGZR4ILWV0+eMij/IKD78cJlLobVg3KoeJpxl3VpSZqsOMsXc3XGnLOsQ==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0),
(23,33,'v1:6E6n7sSjlMps7mV4ycmKPBhVfAn29cXeDrhRdRJ9/etYxE97K89BLnsm1YtpLg==','13900009999','C1','2026-10-17','待审核',NULL,NULL,0);
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录名',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt 密文',
  `name` varchar(50) NOT NULL COMMENT '显示姓名',
  `role` varchar(20) NOT NULL DEFAULT 'student' COMMENT 'admin / coach / student',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 正常 / 1 删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='登录用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `name`, `role`, `status`, `create_time`, `del_flag`) VALUES (1,'admin','$2a$10$p5lTqM1.hglNHbU9Xdw6zu0KUzBMDzDOuxrZs7ZgMAl6GxIUsQBfq','系统管理员','admin',1,'2026-10-17 15:11:42',0),
(2,'coach','$2a$10$TuXhLDGCtHHl7mdwqVox5.2GVhBzJG2TJWNzi.dy9DKrP2cdzLG2K','王建国','coach',1,'2026-10-17 15:11:42',0),
(3,'student','$2a$10$iP/7DHXtMgJJid6VaKWcD.O5gOVljQuOYm3ypRLmVzKBfGzHZEy9S','李小明','student',1,'2026-10-17 15:11:42',0),
(4,'coach02','$2a$10$aNhFSLmRi8xqUouiJE2WMuVkXWpZEegcFK49BK8xT.bL2h6cAcJZ2','李秀兰','coach',1,'2026-10-17 15:11:42',0),
(5,'coach03','$2a$10$tj5y81sCJ0wjpGdlmrY/R./JPgL4r6ASqtwLCmWp5ZV1LKdtBH2Hi','张海涛','coach',1,'2026-10-17 15:11:42',0),
(6,'st_wangfang','$2a$10$.owTecOKfrK/Z5rVc0qucu0Pb/SyIr8Vo/uyk2Yj0U/LLrHvgxnX6','王芳','student',1,'2026-10-17 15:11:42',0),
(7,'st_zhangwei','$2a$10$BqGwesI9aM/8iCdUTgrZeOUFNd8v8XhD6Vz5xizTBAai3ELeGGqWe','张伟','student',1,'2026-10-17 15:11:42',0),
(8,'st_liuyang','$2a$10$6GKgSHbRsUu2iF8gzLGeXesGvrLn3bNrefACfGJdEAvVRijXMC.fW','刘洋','student',1,'2026-10-17 15:11:42',0),
(9,'st_chenjing','$2a$10$D4.QP5u/cz8s50BTN2UzZOWBDKTTcpd84cLAiboMYzS7BNr11L2JG','陈静','student',1,'2026-10-17 15:11:42',0),
(10,'st_sunhao','$2a$10$Uxwsm6AMKCowYJvo1cG0zOox4t1F4k6bHh/wxexLYMTENwLo37CxS','孙浩','student',1,'2026-10-17 15:11:43',0),
(11,'st_zhoumin','$2a$10$iEW4UBwdv3kVGHzG4qQQXe0FMYFa4CX2u5ZUXsFC3x4vwjRvCw7Qe','周敏','student',1,'2026-10-17 15:11:43',0),
(12,'st_wuqiang','$2a$10$5LSyt.W.ft/vPH6vpmVqJOz6yBtSpA/lvJjnfhuDPWAZc9wiU1OLm','吴强','student',1,'2026-10-17 15:11:43',0),
(13,'st_zhengli','$2a$10$xVU2M2Y.lplqi5nf2AxmPe0AWNjwSKUqs8akTrkR4gnlSBBhyUJnq','郑丽','student',1,'2026-10-17 15:11:43',0),
(14,'st_fengtao','$2a$10$2EdDPjNFgr.z35rSzNSXM.3WU5irK5cdTe8UnmPzNb76tEHWKcZzm','冯涛','student',1,'2026-10-17 15:11:43',0),
(15,'st_xuna','$2a$10$yzvaGfBk5lqJwCaQWYxk9.XAqD1ZwIZwQiY1YrFoit.q78noncpHi','许娜','student',1,'2026-10-17 15:11:43',0),
(16,'st_hejun','$2a$10$YvjjkfQbp75FdkgGqV69eezH3ETCkYfmBo6kwuHCXhT0WzZIOHSje','何军','student',1,'2026-10-17 15:11:43',0),
(17,'st_lisiyu','$2a$10$8roZRo6zeEoycrnOp2P5o.3V2tzWCtOzrEQlIaftd3hEJjHBLmu6S','李思雨','student',1,'2026-10-17 15:11:43',0),
(18,'st_zhaolei','$2a$10$AgVu1vpwUuXa.p/WlT1j5u0J2a6YT.XTHltE08noXCdqpofGsBv4O','赵磊','student',1,'2026-10-17 15:11:43',0),
(19,'st_qianyu','$2a$10$sJSg61X7s5lh5zbFzEHxvu47mYLNMmk6s9xJ7OnYKLX1h3uenFL1i','钱雨','student',1,'2026-10-17 15:11:43',0),
(20,'st_sunli','$2a$10$jCENRrmkmcfvApQrNdj4beXmNfXUSzfZDfxEJPiog28fEKMvyw5Om','孙丽','student',1,'2026-10-17 15:11:43',0),
(21,'st38739','$2a$10$pMtjLrpimj6B8A1XZQhwnu6D1nvbHJ5/Ig37gO5ytoYadm2MZjbQK','测试学员','student',1,'2026-10-17 15:18:58',0),
(22,'newcoach1792221538','$2a$10$vyt5hklXdoJ7ytHTolAcS.F/fg1TO7rcrrh5DHMjJm/NJvvEjX34S','新教练','coach',1,'2026-10-17 15:18:59',0),
(23,'st74458','$2a$10$kIKC.CHVusT5McBo8kFpXeuQL/nDc5v.co.0bucthQW6jChG29zRq','测试学员','student',1,'2026-10-17 15:19:34',0),
(24,'newcoach1792221574','$2a$10$L84N1HW8hq4TcjCiZnoo2.7jaQLYrAnjjxIJD07/itjvwalZTgfT6','新教练','coach',1,'2026-10-17 15:19:34',0),
(25,'st81395','$2a$10$S7GUcAaWDNbtpZNoMD1pyOe2nNKRxbeJei8imL6Qkc/TSu23PfYNi','测试学员','student',1,'2026-10-17 15:19:41',0),
(26,'newcoach1792221581','$2a$10$CDEwr2XlXwaGh.FzhWk6QOGsfuyeM/Ijf.kY3npQEJtVc6wvcsj5a','新教练','coach',1,'2026-10-17 15:19:41',0),
(27,'st82380','$2a$10$.gqJgaEVeb45rSYjvX2EU.g5lIcD.JGigs4MZlCc1fJavgSUQFdMu','测试学员','student',1,'2026-10-17 15:19:42',0),
(28,'newcoach1792221582','$2a$10$5xP/PNV7AIgjP0eR0JV4seCu4BthgYabTlvMRDjWOhCqzme69dDEe','新教练','coach',1,'2026-10-17 15:19:42',0),
(29,'st88648','$2a$10$n5QnRn8gPddceeJIzKtlIe5lwaN1/LcryXfr236bWGKGWTMvuonSa','测试学员','student',1,'2026-10-17 15:19:48',0),
(30,'newcoach1792221588','$2a$10$Fss89l8KjKUe7DlN9s9YHOXiR4ULoltBH/PH7rbxw3onEdcFpFMai','新教练','coach',1,'2026-10-17 15:19:48',0),
(31,'st92246','$2a$10$pdd1TzxN8ifKvme6DJwZ/.8dBf66pa15VZ0mbrxidmRZVAydMhOGG','测试学员','student',1,'2026-10-17 15:19:52',0),
(32,'newcoach1792221592','$2a$10$AaGcFvunOnlDcI2A7/fFau4S7K5LUQ1A3/0Iye4nTwcPGkA03TcxO','新教练','coach',1,'2026-10-17 15:19:52',0),
(33,'st2732','$2a$10$A4emtRipTtFuE0bDOGMSeOi5jKuSyQDwtWAL8/2.QRZ.6dCyyy1Y.','测试学员','student',1,'2026-10-17 15:28:22',0),
(34,'newcoach1792222102','$2a$10$GSZTUcrFfM44hTlwgxvvfO.GU3/WaIb8Ss8oy62QUZrnEZItpEZ.y','新教练','coach',1,'2026-10-17 15:28:22',0);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `train_course`
--

DROP TABLE IF EXISTS `train_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `train_course` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `license_type` varchar(10) NOT NULL COMMENT '准驾车型',
  `subject_code` varchar(10) NOT NULL COMMENT '科目代码 1/2/3/4',
  `subject_name` varchar(50) NOT NULL COMMENT '科目名称',
  `required_hours` decimal(5,1) NOT NULL COMMENT '大纲要求学时',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_subject` (`license_type`,`subject_code`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='培训课程 / 科目学时配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `train_course`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `train_course` DISABLE KEYS */;
INSERT INTO `train_course` (`id`, `license_type`, `subject_code`, `subject_name`, `required_hours`, `del_flag`) VALUES (1,'C1','1','科目一 · 道路交通安全法律法规',12.0,0),
(2,'C1','2','科目二 · 场地驾驶技能',26.0,0),
(3,'C1','3','科目三 · 道路驾驶技能',24.0,0),
(4,'C1','4','科目四 · 安全文明驾驶常识',10.0,0),
(5,'C2','1','科目一 · 道路交通安全法律法规',12.0,0),
(6,'C2','2','科目二 · 场地驾驶技能',14.0,0),
(7,'C2','3','科目三 · 道路驾驶技能',24.0,0),
(8,'C2','4','科目四 · 安全文明驾驶常识',10.0,0),
(9,'B2','1','科目一 · 道路交通安全法律法规',12.0,0),
(10,'B2','2','科目二 · 场地驾驶技能',32.0,0),
(11,'B2','3','科目三 · 道路驾驶技能',36.0,0),
(12,'B2','4','科目四 · 安全文明驾驶常识',16.0,0),
(13,'A2','1','科目一 · 道路交通安全法律法规',12.0,0),
(14,'A2','2','科目二 · 场地驾驶技能',30.0,0),
(15,'A2','3','科目三 · 道路驾驶技能',34.0,0),
(16,'A2','4','科目四 · 安全文明驾驶常识',14.0,0);
/*!40000 ALTER TABLE `train_course` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `vehicle`
--

DROP TABLE IF EXISTS `vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plate_no` varchar(20) NOT NULL COMMENT '车牌号',
  `model` varchar(50) DEFAULT NULL COMMENT '车型',
  `coach_id` bigint(20) DEFAULT NULL COMMENT '所属教练',
  `status` varchar(10) NOT NULL DEFAULT '可用' COMMENT '可用 / 维修 / 停用',
  `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate` (`plate_no`),
  KEY `fk_vehicle_coach` (`coach_id`),
  CONSTRAINT `fk_vehicle_coach` FOREIGN KEY (`coach_id`) REFERENCES `coach` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='教练车辆';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
/*!40000 ALTER TABLE `vehicle` DISABLE KEYS */;
INSERT INTO `vehicle` (`id`, `plate_no`, `model`, `coach_id`, `status`, `del_flag`) VALUES (1,'沪A·1234学','大众桑塔纳',1,'可用',0),
(2,'沪A·1235学','大众捷达',1,'可用',0),
(3,'沪A·1236学','日产轩逸',1,'维修',0),
(4,'沪A·2345学','日产轩逸',2,'可用',0),
(5,'沪A·3456学','东风货车',3,'可用',0);
/*!40000 ALTER TABLE `vehicle` ENABLE KEYS */;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Dumping routines for database 'driving_school'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-10-17 15:45:25
