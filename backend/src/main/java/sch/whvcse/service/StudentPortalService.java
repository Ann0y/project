package sch.whvcse.service;

import java.util.List;
import java.util.Map;
import sch.whvcse.common.PageResult;
import sch.whvcse.entity.Booking;
import sch.whvcse.entity.ExamPlan;
import sch.whvcse.entity.ExamRecord;
import sch.whvcse.vo.ProgressVO;

/** 学员端业务（F01–F04） */
public interface StudentPortalService {

    /** F02 我的培训进度 */
    ProgressVO myProgress(Long studentId);

    /** F03 本人带教教练的可预约时段（可按日期筛选） */
    List<Map<String, Object>> myAvailableSchedules(Long studentId, String date);

    /** F03 我的预约（可按状态筛选） */
    PageResult<Map<String, Object>> myBookings(Long studentId, String status, int page, int size);

    /** F03 提交预约（含 E1 冲突检测） */
    Booking book(Long studentId, Long scheduleId);

    /** F03 取消预约（仅待确认可取消） */
    void cancelBooking(Long studentId, Long bookingId);

    /** F04 我的考试成绩 */
    PageResult<Map<String, Object>> myExamRecords(Long studentId, String subjectCode,
                                                  String status, int page, int size);

    /** F04 可报名的考试计划 */
    List<Map<String, Object>> openExamPlans(Long studentId);

    /** F04 考试报名（校验学时与容量） */
    ExamRecord registerExam(Long studentId, Long planId);

    /** 我的缴费记录 */
    List<Map<String, Object>> myPayments(Long studentId);
}
