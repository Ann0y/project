package sch.whvcse.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sch.whvcse.common.Result;
import sch.whvcse.entity.CoachSchedule;
import sch.whvcse.entity.HoursRecord;
import sch.whvcse.service.CoachPortalService;
import sch.whvcse.util.SessionUser;

/**
 * 教练端接口（需求文档 F05–F09）。
 * 数据范围一律由「当前登录教练」决定，不接受前端传入 coachId。
 */
@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final CoachPortalService coachPortalService;
    private final SessionUser sessionUser;

    public CoachController(CoachPortalService coachPortalService, SessionUser sessionUser) {
        this.coachPortalService = coachPortalService;
        this.sessionUser = sessionUser;
    }

    /** F05 教练工作台 */
    @GetMapping("/workbench")
    public Result<Map<String, Object>> workbench(HttpSession session) {
        return Result.ok(coachPortalService.workbench(sessionUser.coachId(session)));
    }

    /* ------------------------------ F06 我的排课 ------------------------------ */

    @GetMapping("/schedules")
    public Result<List<CoachSchedule>> mySchedules(
            @RequestParam(required = false) String date, HttpSession session) {
        return Result.ok(coachPortalService.mySchedules(sessionUser.coachId(session), date));
    }

    @PostMapping("/schedules")
    public Result<CoachSchedule> createSchedule(@RequestBody CoachSchedule payload, HttpSession session) {
        return Result.ok(coachPortalService.saveSchedule(sessionUser.coachId(session), payload));
    }

    @PutMapping("/schedules/{id}")
    public Result<CoachSchedule> updateSchedule(@PathVariable Long id,
                                                @RequestBody CoachSchedule payload,
                                                HttpSession session) {
        payload.setId(id);
        return Result.ok(coachPortalService.saveSchedule(sessionUser.coachId(session), payload));
    }

    @DeleteMapping("/schedules/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id, HttpSession session) {
        coachPortalService.removeSchedule(sessionUser.coachId(session), id);
        return Result.ok();
    }

    /* ------------------------------ F07 预约处理 ------------------------------ */

    @GetMapping("/bookings")
    public Result<List<Map<String, Object>>> assignedBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentName,
            HttpSession session) {
        return Result.ok(coachPortalService.assignedBookings(
                sessionUser.coachId(session), status, studentName));
    }

    @PatchMapping("/bookings/{id}/status")
    public Result<Void> changeBookingStatus(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            HttpSession session) {
        String status = body.get("status");
        if (status == null || status.isBlank()) return Result.fail(400, "请指定目标状态");
        coachPortalService.changeBookingStatus(sessionUser.coachId(session), id, status);
        return Result.ok();
    }

    /* ------------------------------ F08 学时登记 ------------------------------ */

    @GetMapping("/hours")
    public Result<List<Map<String, Object>>> myHours(HttpSession session) {
        return Result.ok(coachPortalService.myHours(sessionUser.coachId(session)));
    }

    @PostMapping("/hours")
    public Result<HoursRecord> createHours(@RequestBody HoursRecord payload, HttpSession session) {
        return Result.ok(coachPortalService.createHours(sessionUser.coachId(session), payload));
    }

    /* ------------------------------ F09 我的学员 ------------------------------ */

    @GetMapping("/students")
    public Result<List<Map<String, Object>>> myStudents(HttpSession session) {
        return Result.ok(coachPortalService.myStudents(sessionUser.coachId(session)));
    }
}
