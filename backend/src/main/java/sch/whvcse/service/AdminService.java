package sch.whvcse.service;

import java.util.List;
import java.util.Map;
import sch.whvcse.common.PageResult;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.ExamPlan;
import sch.whvcse.entity.ExamRecord;
import sch.whvcse.entity.HoursRecord;
import sch.whvcse.entity.Payment;
import sch.whvcse.entity.TrainCourse;
import sch.whvcse.entity.User;
import sch.whvcse.entity.Vehicle;
import sch.whvcse.vo.ExamRecordView;

/** 管理端业务（F10–F16） */
public interface AdminService {

    /* ------------------------------ F10 工作台 ------------------------------ */
    Map<String, Object> dashboard();

    /* ------------------------------ F11 学员管理 ------------------------------ */
    PageResult<Map<String, Object>> searchStudents(String name, String idCard, String status,
                                                   String licenseType, Long coachId,
                                                   int page, int size);

    Map<String, Object> studentDetail(Long id);

    /** 报名登记 / 档案维护；payload.id 为空即新增 */
    Map<String, Object> saveStudent(Map<String, Object> payload);

    /** 报名审核（通过 → 在训；驳回 → 退学） */
    void auditStudent(Long id, boolean pass, String opinion);

    /** 分配固定带教教练与车辆 */
    void assignCoachAndVehicle(Long id, Long coachId, Long vehicleId);

    /** 状态流转：在训 / 结业 / 退学 */
    void changeStudentStatus(Long id, String status);

    void deleteStudent(Long id);

    /* --------------------------- F12 教练与车辆管理 --------------------------- */
    List<Map<String, Object>> listCoaches(String keyword);

    Coach saveCoach(Map<String, Object> payload);

    void deleteCoach(Long id);

    List<Vehicle> listVehicles(String keyword);

    Vehicle saveVehicle(Vehicle vehicle);

    void changeVehicleStatus(Long id, String status);

    void deleteVehicle(Long id);

    /* --------------------------- F13 培训与约车管理 --------------------------- */
    PageResult<Map<String, Object>> listBookings(String status, int page, int size);

    void changeBookingStatus(Long bookingId, String status);

    PageResult<Map<String, Object>> listHours(Long studentId, String subjectCode,
                                              String auditStatus, int page, int size);

    /** 学时审核：通过则计入学员已学学时 */
    void auditHours(Long id, boolean pass, String opinion, Long auditorId);

    List<TrainCourse> listCourses();

    void updateCourseHours(Long id, java.math.BigDecimal requiredHours);

    /* ------------------------------ F14 成绩同步（只读） ------------------------------
     * 科目考试统一在交管系统（交管 12123 等）中报考，本系统不提供考试报名、
     * 考场编排、名单确认等考试业务，只负责同步并展示成绩。
     * ------------------------------------------------------------------------------ */
    List<ExamPlan> listExamPlans();

    PageResult<ExamRecordView> listExamRecords(Long planId, String status, int page, int size);

    /**
     * 同步交管系统成绩：按「学员 + 科目 + 考试日期」写入或更新一条成绩记录。
     *
     * @param studentId   本系统学员主键，可为空（为空时用 studentName 匹配）
     * @param studentName 学员姓名，studentId 为空时必填；重名时取第一条
     * @param subjectCode 科目代码 1~4
     * @param examDate    实际考试日期，可为空（空则取当天）
     * @param score       成绩，可为空（空表示该场次尚未出成绩，状态为待考）
     */
    ExamRecordView syncExamScore(Long studentId, String studentName, String subjectCode,
                                 java.time.LocalDate examDate, java.math.BigDecimal score);

    /**
     * 批量同步交管系统成绩（对接推送接口时使用）。
     * <p>单条失败只记录到错误列表，不影响其余条目；整个批次在一个事务里。</p>
     *
     * @param items 每项含 studentId 或 studentName、subjectCode、examDate、score
     * @return {total, created, updated, skipped, errors[]}
     */
    java.util.Map<String, Object> syncExamScoresBatch(java.util.List<java.util.Map<String, Object>> items);

    /* ------------------------------ F15 统计报表 ------------------------------ */
    Map<String, Object> statistics(String start, String end, String licenseType,
                                   String subject, Long coachId, String range);

    /* ------------------------------ F16 用户管理 ------------------------------ */
    PageResult<User> listUsers(String keyword, String role, int page, int size);

    User createUser(Map<String, Object> payload);

    void updateUser(Long id, Map<String, Object> payload);

    void changeUserStatus(Long id, Integer status);

    void resetPassword(Long id, String newPassword);

    void deleteUser(Long id);

    /* -------------------------------- 缴费 -------------------------------- */
    List<Payment> listPayments(Long studentId);

    Payment savePayment(Payment payment);

    void registerPayment(Long studentId, String item, java.math.BigDecimal amount, String payMethod,
                         Long operatorId);

    void deletePayment(Long id);
}
