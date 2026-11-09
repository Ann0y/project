package sch.whvcse.controller;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import sch.whvcse.common.PageResult;
import sch.whvcse.common.Result;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.ExamPlan;
import sch.whvcse.entity.Payment;
import sch.whvcse.entity.TrainCourse;
import sch.whvcse.entity.User;
import sch.whvcse.entity.Vehicle;
import sch.whvcse.service.AdminService;
import sch.whvcse.util.SessionUser;
import sch.whvcse.vo.ExamRecordView;

/**
 * 管理端接口（需求文档 F10–F16）。
 * 所有写操作均校验当前用户为管理员（需求 2：本期不做接口级角色校验的补充，
 * 这里在服务端补上，避免前端守卫被绕过）。
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    /** JSON 与 Map 互转（批量同步时把数组元素转成 Map） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AdminService adminService;
    private final SessionUser sessionUser;

    public AdminController(AdminService adminService, SessionUser sessionUser) {
        this.adminService = adminService;
        this.sessionUser = sessionUser;
    }

    /* =============================== F10 工作台 =============================== */

    @GetMapping("/statistics/overview")
    public Result<Map<String, Object>> dashboard(HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.dashboard());
    }

    /* ============================== F11 学员管理 ============================== */

    @GetMapping("/students")
    public Result<PageResult<Map<String, Object>>> listStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String licenseType,
            @RequestParam(required = false) Long coachId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.searchStudents(name, idCard, status, licenseType, coachId, page, size));
    }

    @GetMapping("/students/{id}")
    public Result<Map<String, Object>> studentDetail(@PathVariable Long id, HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.studentDetail(id));
    }

    @PostMapping("/students")
    public Result<Map<String, Object>> createStudent(@RequestBody Map<String, Object> payload,
                                                     HttpSession session) {
        sessionUser.requireAdmin(session);
        payload.remove("id");
        return Result.ok(adminService.saveStudent(payload));
    }

    @PutMapping("/students/{id}")
    public Result<Map<String, Object>> updateStudent(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> payload,
                                                     HttpSession session) {
        sessionUser.requireAdmin(session);
        payload.put("id", id);
        return Result.ok(adminService.saveStudent(payload));
    }

    @DeleteMapping("/students/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id, HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.deleteStudent(id);
        return Result.ok();
    }

    /** 报名审核 */
    @PostMapping("/students/{id}/audit")
    public Result<Void> auditStudent(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     HttpSession session) {
        sessionUser.requireAdmin(session);
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        Object opinion = body.get("opinion");
        adminService.auditStudent(id, pass, opinion == null ? "" : opinion.toString());
        return Result.ok();
    }

    /** 分配带教教练与车辆 */
    @PutMapping("/students/{id}/coach")
    public Result<Void> assignCoach(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.assignCoachAndVehicle(id, toLong(body.get("coachId")), toLong(body.get("vehicleId")));
        return Result.ok();
    }

    /** 状态流转：在训 / 结业 / 退学 */
    @PatchMapping("/students/{id}/status")
    public Result<Void> changeStudentStatus(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.changeStudentStatus(id, body.get("status"));
        return Result.ok();
    }

    /* =========================== F12 教练与车辆管理 =========================== */

    @GetMapping("/coaches")
    public Result<List<Map<String, Object>>> listCoaches(
            @RequestParam(required = false) String keyword, HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listCoaches(keyword));
    }

    @PostMapping("/coaches")
    public Result<Coach> createCoach(@RequestBody Map<String, Object> payload, HttpSession session) {
        sessionUser.requireAdmin(session);
        payload.remove("id");
        return Result.ok(adminService.saveCoach(payload));
    }

    @PutMapping("/coaches/{id}")
    public Result<Coach> updateCoach(@PathVariable Long id,
                                     @RequestBody Map<String, Object> payload,
                                     HttpSession session) {
        sessionUser.requireAdmin(session);
        payload.put("id", id);
        return Result.ok(adminService.saveCoach(payload));
    }

    @DeleteMapping("/coaches/{id}")
    public Result<Void> deleteCoach(@PathVariable Long id, HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.deleteCoach(id);
        return Result.ok();
    }

    @GetMapping("/vehicles")
    public Result<List<Vehicle>> listVehicles(
            @RequestParam(required = false) String keyword, HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listVehicles(keyword));
    }

    @PostMapping("/vehicles")
    public Result<Vehicle> createVehicle(@RequestBody Vehicle vehicle, HttpSession session) {
        sessionUser.requireAdmin(session);
        vehicle.setId(null);
        return Result.ok(adminService.saveVehicle(vehicle));
    }

    @PutMapping("/vehicles/{id}")
    public Result<Vehicle> updateVehicle(@PathVariable Long id,
                                         @RequestBody Vehicle vehicle,
                                         HttpSession session) {
        sessionUser.requireAdmin(session);
        vehicle.setId(id);
        return Result.ok(adminService.saveVehicle(vehicle));
    }

    @PatchMapping("/vehicles/{id}/status")
    public Result<Void> changeVehicleStatus(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.changeVehicleStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/vehicles/{id}")
    public Result<Void> deleteVehicle(@PathVariable Long id, HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.deleteVehicle(id);
        return Result.ok();
    }

    /* =========================== F13 培训与约车管理 =========================== */

    @GetMapping("/bookings")
    public Result<PageResult<Map<String, Object>>> listBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listBookings(status, page, size));
    }

    @PatchMapping("/bookings/{id}/status")
    public Result<Void> changeBookingStatus(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.changeBookingStatus(id, body.get("status"));
        return Result.ok();
    }

    @GetMapping("/hours")
    public Result<PageResult<Map<String, Object>>> listHours(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listHours(studentId, subjectCode, auditStatus, page, size));
    }

    /** 学时审核：通过则写入学员学时 */
    @PatchMapping("/hours/{id}/audit")
    public Result<Void> auditHours(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body,
                                   HttpSession session) {
        sessionUser.requireAdmin(session);
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        Object opinion = body.get("opinion");
        adminService.auditHours(id, pass, opinion == null ? "" : opinion.toString(),
                sessionUser.userId(session));
        return Result.ok();
    }

    @GetMapping("/courses")
    public Result<List<TrainCourse>> listCourses(HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listCourses());
    }

    @PutMapping("/courses/{id}")
    public Result<Void> updateCourse(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     HttpSession session) {
        sessionUser.requireAdmin(session);
        Object h = body.get("requiredHours");
        if (h == null) return Result.fail(400, "请填写学时");
        adminService.updateCourseHours(id, new BigDecimal(h.toString()));
        return Result.ok();
    }

    /* ==================== F14 考试与成绩（只在交管系统报考，本系统只同步成绩） ==================== */

    /** 只读：查看考试场次（用于成绩列表按场次筛选） */
    @GetMapping("/exam-plans")
    public Result<List<ExamPlan>> listExamPlans(HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listExamPlans());
    }

    @GetMapping("/exam-scores")
    public Result<PageResult<ExamRecordView>> listExamScores(
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listExamRecords(planId, status, page, size));
    }

    /**
     * 从交管系统同步一条成绩。
     * <p>管理端不提供考试报名与成绩手工录入，成绩由交管系统回传后写入本系统。
     * 可按学生主键 {@code studentId} 或姓名 {@code studentName} 指定学员。</p>
     */
    @PostMapping("/exam-scores/sync")
    public Result<ExamRecordView> syncExamScore(@RequestBody Map<String, Object> body,
                                                HttpSession session) {
        sessionUser.requireAdmin(session);
        Object studentId = body.get("studentId");
        Object studentName = body.get("studentName");
        Object subjectCode = body.get("subjectCode");
        if ((studentId == null && studentName == null) || subjectCode == null) {
            return Result.fail(400, "请提供学员与科目");
        }
        Object score = body.get("score");
        Object examDate = body.get("examDate");
        LocalDate date;
        try {
            date = examDate == null || examDate.toString().isBlank()
                    ? null : LocalDate.parse(examDate.toString().substring(0, 10));
        } catch (Exception e) {
            return Result.fail(400, "考试日期格式应为 yyyy-MM-dd");
        }
        return Result.ok(adminService.syncExamScore(
                studentId == null ? null : toLong(studentId),
                studentName == null ? null : studentName.toString().trim(),
                subjectCode.toString(),
                date,
                score == null || score.toString().isBlank() ? null : new BigDecimal(score.toString())));
    }

    /**
     * 批量同步交管系统成绩（对接推送接口）。
     * <p>请求体可以是数组，也可以是 {@code {"items": [...]}}；
     * 单条失败不会中断整批，结果里返回 created / updated / skipped 与逐条错误。</p>
     */
    @PostMapping("/exam-scores/sync/batch")
    public Result<Map<String, Object>> syncExamScoresBatch(
            @RequestBody JsonNode body,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        JsonNode arr =
                body.isArray() ? body : body.path("items");
        if (!arr.isArray()) {
            return Result.fail(400, "请提交成绩数组，或用 {\"items\": [...]} 包裹");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode node : arr) {
            if (node == null || node.isNull()) continue;
            items.add(OBJECT_MAPPER.convertValue(node,
                    new TypeReference<Map<String, Object>>() {}));
        }
        return Result.ok(adminService.syncExamScoresBatch(items));
    }

    /** 增量同步模板：告诉对接方每条记录该给哪些字段 */
    @GetMapping("/exam-scores/sync/template")
    public Result<List<Map<String, Object>>> syncTemplate(HttpSession session) {
        sessionUser.requireAdmin(session);
        List<Map<String, Object>> sample = new ArrayList<>();
        for (String subject : sch.whvcse.common.Enums.SUBJECTS.keySet().stream().sorted().toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", 1);
            row.put("studentName", "李小明");
            row.put("subjectCode", subject);
            row.put("examDate", LocalDate.now().toString());
            row.put("score", 90);
            sample.add(row);
        }
        return Result.ok(sample);
    }

    /* ============================== F15 统计报表 ============================== */

    @GetMapping("/statistics/report")
    public Result<Map<String, Object>> report(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String licenseType,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Long coachId,
            @RequestParam(required = false) String range,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.statistics(start, end, licenseType, subject, coachId, range));
    }

    /* ============================== F16 用户管理 ============================== */

    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listUsers(keyword, role, page, size));
    }

    @PostMapping("/users")
    public Result<User> createUser(@RequestBody Map<String, Object> payload, HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.createUser(payload));
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id,
                                   @RequestBody Map<String, Object> payload,
                                   HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.updateUser(id, payload);
        return Result.ok();
    }

    @PatchMapping("/users/{id}/status")
    public Result<Void> changeUserStatus(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         HttpSession session) {
        sessionUser.requireAdmin(session);
        Object s = body.get("status");
        adminService.changeUserStatus(id, s == null ? 1 : Integer.valueOf(s.toString()));
        return Result.ok();
    }

    @PatchMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody Map<String, String> body,
                                      HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.resetPassword(id, body.get("password"));
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.deleteUser(id);
        return Result.ok();
    }

    /* ================================ 缴费 ================================ */

    @GetMapping("/payments")
    public Result<List<Payment>> listPayments(@RequestParam(required = false) Long studentId,
                                              HttpSession session) {
        sessionUser.requireAdmin(session);
        return Result.ok(adminService.listPayments(studentId));
    }

    @PostMapping("/payments")
    public Result<Payment> savePayment(@RequestBody Payment payment, HttpSession session) {
        sessionUser.requireAdmin(session);
        payment.setOperatorId(sessionUser.userId(session));
        return Result.ok(adminService.savePayment(payment));
    }

    /** 缴费登记（快捷：直接置为已缴） */
    @PostMapping("/payments/register")
    public Result<Void> registerPayment(@RequestBody Map<String, Object> body, HttpSession session) {
        sessionUser.requireAdmin(session);
        Object sid = body.get("studentId");
        if (sid == null) return Result.fail(400, "请选择学员");
        Object amount = body.get("amount");
        Object method = body.get("payMethod");
        Object item = body.get("item");
        adminService.registerPayment(
                toLong(sid),
                item == null ? "培训费" : item.toString(),
                amount == null ? BigDecimal.ZERO : new BigDecimal(amount.toString()),
                method == null ? null : method.toString(),
                sessionUser.userId(session));
        return Result.ok();
    }

    @DeleteMapping("/payments/{id}")
    public Result<Void> deletePayment(@PathVariable Long id, HttpSession session) {
        sessionUser.requireAdmin(session);
        adminService.deletePayment(id);
        return Result.ok();
    }

    private static Long toLong(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }
}
