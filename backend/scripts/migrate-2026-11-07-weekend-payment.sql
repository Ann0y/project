-- 2026-11-07 学员端增强：取消周末排课 + 学费缴纳演示数据
-- 适用于已存在旧库的环境；全新导入可直接使用 schema.sql / driving_school_full.sql。
-- 说明：
--   1. 周六、周日不再排课，历史周末排课保留，仅清理今天及以后的周末时段；
--   2. 若周末时段上还有未完成预约，先自动取消，避免学员白跑；
--   3. 培训费每名学员只保留一笔，删除历史演示中多追加的“培训费（第二阶段）”；
--   4. 演示学员 student（李小明）的这笔培训费保持待缴，用于演示「学费缴纳」。

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

-- 3) 删除历史演示中多追加的“培训费（第二阶段）”
DELETE FROM `payment` WHERE `item` = '培训费（第二阶段）';

-- 4) 同一学员若存在多笔培训费，仅保留最早的一笔
DELETE p1
FROM `payment` p1
JOIN `payment` p2
  ON p1.student_id = p2.student_id
 AND p1.item = '培训费'
 AND p2.item = '培训费'
 AND p1.id > p2.id;

-- 5) 演示学员的唯一一笔培训费保持待缴，便于演示在线缴费
UPDATE `payment` p
JOIN `student` s ON s.id = p.student_id
JOIN `sys_user` u ON u.id = s.user_id
SET p.status = '未缴',
    p.pay_time = NULL,
    p.pay_method = NULL
WHERE u.username = 'student'
  AND p.item = '培训费';
