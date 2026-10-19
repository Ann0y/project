package sch.whvcse.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sch.whvcse.common.PageResult;
import sch.whvcse.common.Result;
import sch.whvcse.entity.Booking;
import sch.whvcse.entity.ExamRecord;
import sch.whvcse.service.StudentPortalService;
import sch.whvcse.util.SessionUser;
import sch.whvcse.vo.ProgressVO;

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

    /** F03 取消预约 */
    @PatchMapping("/bookings/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpSession session) {
        studentPortalService.cancelBooking(sessionUser.studentId(session), id);
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

    /** F04 可报名的考试计划 */
    @GetMapping("/exam-plans/open")
    public Result<List<Map<String, Object>>> openPlans(HttpSession session) {
        return Result.ok(studentPortalService.openExamPlans(sessionUser.studentId(session)));
    }

    /** F04 考试报名 */
    @PostMapping("/exam-plans/{id}/register")
    public Result<ExamRecord> registerExam(@PathVariable Long id, HttpSession session) {
        return Result.ok(studentPortalService.registerExam(sessionUser.studentId(session), id));
    }

    /** 我的缴费记录 */
    @GetMapping("/payments/my")
    public Result<List<Map<String, Object>>> myPayments(HttpSession session) {
        return Result.ok(studentPortalService.myPayments(sessionUser.studentId(session)));
    }
}
