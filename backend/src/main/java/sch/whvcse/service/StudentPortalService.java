package sch.whvcse.service;

import java.util.List;
import java.util.Map;
import sch.whvcse.common.PageResult;
import sch.whvcse.entity.Booking;
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

    /** F03 取消预约（待确认 / 已确认可取消，可填写原因） */
    void cancelBooking(Long studentId, Long bookingId, String reason);

    /** F03 预约改期（换到本人带教教练的另一个可约时段） */
    void rescheduleBooking(Long studentId, Long bookingId, Long newScheduleId);

    /** 删除本人已结束的预约记录（已取消 / 已完成 / 爽约） */
    void deleteBooking(Long studentId, Long bookingId);

    /** F04 我的考试成绩 */
    PageResult<Map<String, Object>> myExamRecords(Long studentId, String subjectCode,
                                                  String status, int page, int size);

    /** 我的缴费记录 */
    List<Map<String, Object>> myPayments(Long studentId);

    /** 缴纳一笔本人待缴费用 */
    void payMyPayment(Long studentId, Long paymentId, String payMethod);

    /** 我的学时明细（含审核状态与驳回原因） */
    List<Map<String, Object>> myHours(Long studentId, String subjectCode, String auditStatus);

    /** 修改个人资料（姓名 + 联系电话） */
    void updateProfile(Long studentId, String name, String phone);
}
