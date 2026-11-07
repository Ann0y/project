package sch.whvcse.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sch.whvcse.common.PageResult;
import sch.whvcse.common.Result;
import sch.whvcse.entity.Booking;
import sch.whvcse.interceptor.LoginInterceptor;
import sch.whvcse.service.StudentPortalService;
import sch.whvcse.util.SessionUser;
import sch.whvcse.vo.ProgressVO;
import sch.whvcse.vo.UserVO;

/**
 * 学员端接口（需求文档 F02–F04）。
 * 所有接口的数据范围都由「当前登录学员」决定，不接受前端传入 studentId，
 * 从根上避免越权查他人数据。
 */
@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentPortalService studentPortalService;
    private final SessionUser sessionUser;

    public StudentController(StudentPortalService studentPortalService, SessionUser sessionUser) {
        this.studentPortalService = studentPortalService;
        this.sessionUser = sessionUser;
    }

    /** F02 我的培训进度 */
    @GetMapping("/students/me/progress")
    public Result<ProgressVO> myProgress(HttpSession session) {
        return Result.ok(studentPortalService.myProgress(sessionUser.studentId(session)));
    }

    /** F03 本人带教教练的可预约时段 */
    @GetMapping("/schedules/mine")
    public Result<List<Map<String, Object>>> availableSchedules(
            @RequestParam(required = false) String date, HttpSession session) {
        return Result.ok(studentPortalService.myAvailableSchedules(sessionUser.studentId(session), date));
    }

    /** F03 我的预约 */
    @GetMapping("/bookings/my")
    public Result<PageResult<Map<String, Object>>> myBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        return Result.ok(studentPortalService.myBookings(
                sessionUser.studentId(session), status, page, size));
    }

    /** F03 提交预约（含 E1 冲突检测） */
    @PostMapping("/bookings")
    public Result<Booking> book(@RequestBody Map<String, Object> body, HttpSession session) {
        Object scheduleId = body.get("scheduleId");
        if (scheduleId == null) return Result.fail(400, "请选择预约时段");
        return Result.ok(studentPortalService.book(
                sessionUser.studentId(session), Long.valueOf(scheduleId.toString())));
    }

    /** F03 取消预约（可填写取消原因） */
    @PatchMapping("/bookings/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, String> body,
                               HttpSession session) {
        String reason = body == null ? null : body.get("reason");
        studentPortalService.cancelBooking(sessionUser.studentId(session), id, reason);
        return Result.ok();
    }

    /** F03 预约改期 */
    @PatchMapping("/bookings/{id}/reschedule")
    public Result<Void> reschedule(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body,
                                   HttpSession session) {
        Object scheduleId = body.get("scheduleId");
        if (scheduleId == null) return Result.fail(400, "请选择新的预约时段");
        studentPortalService.rescheduleBooking(
                sessionUser.studentId(session), id, Long.valueOf(scheduleId.toString()));
        return Result.ok();
    }

    /** 删除本人已结束的预约记录（已取消 / 已完成 / 爽约），未完成的须先取消 */
    @DeleteMapping("/bookings/{id}")
    public Result<Void> deleteBooking(@PathVariable Long id, HttpSession session) {
        studentPortalService.deleteBooking(sessionUser.studentId(session), id);
        return Result.ok();
    }

    /** F04 我的考试成绩 */
    @GetMapping("/exam-scores/my")
    public Result<PageResult<Map<String, Object>>> myScores(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        return Result.ok(studentPortalService.myExamRecords(
                sessionUser.studentId(session), subjectCode, status, page, size));
    }

    /** 我的缴费记录 */
    @GetMapping("/payments/my")
    public Result<List<Map<String, Object>>> myPayments(HttpSession session) {
        return Result.ok(studentPortalService.myPayments(sessionUser.studentId(session)));
    }

    /** 缴纳本人一笔待缴费用（演示环境直接标记为已缴） */
    @PostMapping("/payments/my/{id}/pay")
    public Result<Void> payMyPayment(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    HttpSession session) {
        String payMethod = body == null ? null : body.get("payMethod");
        studentPortalService.payMyPayment(sessionUser.studentId(session), id, payMethod);
        return Result.ok();
    }

    /** 我的学时明细（含审核状态与驳回原因） */
    @GetMapping("/hours/my")
    public Result<List<Map<String, Object>>> myHours(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String auditStatus,
            HttpSession session) {
        return Result.ok(studentPortalService.myHours(
                sessionUser.studentId(session), subjectCode, auditStatus));
    }

    /** 修改个人资料（姓名 + 联系电话） */
    @PutMapping("/students/me/profile")
    public Result<UserVO> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        Long studentId = sessionUser.studentId(session);
        String name = body.get("name");
        String phone = body.get("phone");
        studentPortalService.updateProfile(studentId, name, phone);

        // 同步 Session 中的显示名，顶部导航无需重新登录即可更新
        UserVO vo = sessionUser.current(session);
        vo.setName(name == null ? vo.getName() : name.trim());
        session.setAttribute(LoginInterceptor.SESSION_USER, vo);
        return Result.ok(vo);
    }
}
