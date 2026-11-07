-- 2026-11-07 学员端增强：取消周末排课 + 学费缴纳演示数据
-- 适用于已存在旧库的环境；全新导入可直接使用 schema.sql / driving_school_full.sql。
-- 说明：
--   1. 周六、周日不再排课，历史周末排课保留，仅清理今天及以后的周末时段；
--   2. 若周末时段上还有未完成预约，先自动取消，避免学员白跑；
--   3. 为演示学员 student（李小明）补一笔待缴学费，便于演示「学费缴纳」。

-- 1) 取消未来周末时段上的未完成预约
UPDATE `booking` b
JOIN `coach_schedule` s ON s.id = b.schedule_id
SET b.status = '已取消',
    b.cancel_reason = '周末不排课，系统自动取消',
    b.cancel_time = NOW()
WHERE b.status IN ('待确认', '已确认')
  AND s.del_flag = 0
  AND s.schedule_date >= CURDATE()
  AND DAYOFWEEK(s.schedule_date) IN (1, 7);

-- 2) 软删除今天及以后的周末排课
UPDATE `coach_schedule`
SET del_flag = 1
WHERE del_flag = 0
  AND schedule_date >= CURDATE()
  AND DAYOFWEEK(schedule_date) IN (1, 7);

-- 3) 演示学员补一笔待缴学费（幂等：已有同名项目则不重复插入）
INSERT INTO `payment` (`student_id`, `item`, `amount`, `pay_time`, `pay_method`, `status`, `operator_id`)
SELECT s.id, '培训费（第二阶段）', 2800.00, NULL, NULL, '未缴',
       (SELECT id FROM `sys_user` WHERE username = 'admin' LIMIT 1)
FROM `student` s
JOIN `sys_user` u ON u.id = s.user_id
WHERE u.username = 'student'
  AND NOT EXISTS (
      SELECT 1 FROM `payment` p
      WHERE p.student_id = s.id AND p.item = '培训费（第二阶段）'
  );
