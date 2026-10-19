package sch.whvcse.service;

import java.util.List;
import java.util.Map;
import sch.whvcse.entity.CoachSchedule;
import sch.whvcse.entity.HoursRecord;

/** 教练端业务（F05–F09） */
public interface CoachPortalService {

    /** F05 教练工作台概览 */
    Map<String, Object> workbench(Long coachId);

    /** F06 我的排课（按日期筛选） */
    List<CoachSchedule> mySchedules(Long coachId, String date);

    /** F06 新增 / 编辑排课时段 */
    CoachSchedule saveSchedule(Long coachId, CoachSchedule payload);

    /** F06 删除排课时段（已有预约不允许删） */
    void removeSchedule(Long coachId, Long scheduleId);

    /** F07 我派单的预约（按学员 / 状态筛选） */
    List<Map<String, Object>> assignedBookings(Long coachId, String status, String studentName);

    /** F07 预约状态流转（确认 / 取消 / 完成 / 爽约） */
    void changeBookingStatus(Long coachId, Long bookingId, String status);

    /** F08 我的学时登记记录 */
    List<Map<String, Object>> myHours(Long coachId);

    /** F08 登记学时（提交后待审核） */
    HoursRecord createHours(Long coachId, HoursRecord payload);

    /** F09 我的学员（档案 + 进度） */
    List<Map<String, Object>> myStudents(Long coachId);
}
