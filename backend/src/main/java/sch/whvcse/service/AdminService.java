package sch.whvcse.service;

import java.util.List;
import java.util.Map;
import sch.whvcse.common.PageResult;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.ExamPlan;
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

    /* ------------------------------ F14 考试管理 ------------------------------ */
    List<ExamPlan> listExamPlans();

    ExamPlan saveExamPlan(ExamPlan plan);

    void changeExamPlanStatus(Long id, String status);

    void deleteExamPlan(Long id);

    PageResult<ExamRecordView> listExamRecords(Long planId, String status, int page, int size);

    /** 录入成绩：不合格自动生成补考记录 */
    void saveExamScore(Long recordId, java.math.BigDecimal score);

    /** 确认考试名单（把待考记录纳入本次考试） */
    void confirmRoster(Long planId);

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
