package sch.whvcse.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sch.whvcse.common.BizException;
import sch.whvcse.common.Enums;
import sch.whvcse.common.PageResult;
import sch.whvcse.entity.Booking;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.CoachSchedule;
import sch.whvcse.entity.ExamPlan;
import sch.whvcse.entity.ExamRecord;
import sch.whvcse.entity.HoursRecord;
import sch.whvcse.entity.Payment;
import sch.whvcse.entity.Student;
import sch.whvcse.entity.TrainCourse;
import sch.whvcse.entity.User;
import sch.whvcse.entity.Vehicle;
import sch.whvcse.mapper.BookingMapper;
import sch.whvcse.mapper.CoachMapper;
import sch.whvcse.mapper.CoachScheduleMapper;
import sch.whvcse.mapper.ExamPlanMapper;
import sch.whvcse.mapper.ExamRecordMapper;
import sch.whvcse.mapper.HoursRecordMapper;
import sch.whvcse.mapper.PaymentMapper;
import sch.whvcse.mapper.StudentMapper;
import sch.whvcse.mapper.TrainCourseMapper;
import sch.whvcse.mapper.UserMapper;
import sch.whvcse.mapper.VehicleMapper;
import sch.whvcse.service.AdminService;
import sch.whvcse.util.IdCardCrypto;
import sch.whvcse.vo.ExamRecordView;

@Service
public class AdminServiceImpl implements AdminService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final CoachMapper coachMapper;
    private final VehicleMapper vehicleMapper;
    private final TrainCourseMapper trainCourseMapper;
    private final CoachScheduleMapper scheduleMapper;
    private final BookingMapper bookingMapper;
    private final HoursRecordMapper hoursMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamRecordMapper examRecordMapper;
    private final PaymentMapper paymentMapper;
    private final IdCardCrypto idCardCrypto;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(StudentMapper studentMapper,
                            UserMapper userMapper,
                            CoachMapper coachMapper,
                            VehicleMapper vehicleMapper,
                            TrainCourseMapper trainCourseMapper,
                            CoachScheduleMapper scheduleMapper,
                            BookingMapper bookingMapper,
                            HoursRecordMapper hoursMapper,
                            ExamPlanMapper examPlanMapper,
                            ExamRecordMapper examRecordMapper,
                            PaymentMapper paymentMapper,
                            IdCardCrypto idCardCrypto,
                            PasswordEncoder passwordEncoder) {
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.coachMapper = coachMapper;
        this.vehicleMapper = vehicleMapper;
        this.trainCourseMapper = trainCourseMapper;
        this.scheduleMapper = scheduleMapper;
        this.bookingMapper = bookingMapper;
        this.hoursMapper = hoursMapper;
        this.examPlanMapper = examPlanMapper;
        this.examRecordMapper = examRecordMapper;
        this.paymentMapper = paymentMapper;
        this.idCardCrypto = idCardCrypto;
        this.passwordEncoder = passwordEncoder;
    }

    /* =============================== F10 工作台 =============================== */

    @Override
    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        long training = studentMapper.countByStatus("在训");
        long monthEnroll = studentMapper.countByEnrollRange(monthStart, monthEnd);
        Double monthHours = hoursMapper.sumApprovedInRange(monthStart, monthEnd.minusDays(1));

        // 本月考试通过率（按已出分记录）
        long scored = examRecordMapper.countScored();
        long passed = examRecordMapper.countScoredPassed();
        double passRate = scored == 0 ? 0 : Math.round(passed * 1000.0 / scored) / 10.0;

        // 待办
        Map<String, Object> todo = new LinkedHashMap<>();
        todo.put("pendingAudit", studentMapper.countByStatus("待审核"));
        todo.put("pendingHours", hoursMapper.countByAuditStatus("待审核"));
        todo.put("pendingBooking", bookingMapper.countByStatus("待确认"));
        todo.put("pendingScore", examRecordMapper.countPending());

        // 近 12 个月报名趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(today.minusMonths(i));
            long c = studentMapper.countByEnrollRange(ym.atDay(1), ym.plusMonths(1).atDay(1));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", ym.toString());
            m.put("count", c);
            trend.add(m);
        }

        // 准驾车型分布
        List<Map<String, Object>> licenseDist = new ArrayList<>();
        for (String type : Enums.LICENSE_TYPES) {
            long c = studentMapper.findAll().stream()
                    .filter(s -> type.equals(s.getLicenseType())).count();
            if (c > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", type);
                m.put("value", c);
                licenseDist.add(m);
            }
        }

        // 教练带教工作量
        List<Map<String, Object>> coaches = new ArrayList<>();
        for (Coach c : coachMapper.findActive()) {
            long n = studentMapper.findByCoachId(c.getId()).size();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("coachId", c.getId());
            m.put("coachName", userName(c.getUserId()));
            m.put("students", n);
            coaches.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trainingCount", training);
        result.put("monthEnroll", monthEnroll);
        result.put("monthHours", monthHours == null ? 0 : monthHours);
        result.put("passRate", passRate);
        result.put("todo", todo);
        result.put("enrollTrend", trend);
        result.put("licenseDistribution", licenseDist);
        result.put("coachWorkload", coaches);
        return result;
    }

    /* ============================== F11 学员管理 ============================== */

    @Override
    public PageResult<Map<String, Object>> searchStudents(String name, String idCard, String status,
                                                          String licenseType, Long coachId,
                                                          int page, int size) {
        // 身份证是加密存储的，无法直接 LIKE 检索密文；
        // 因此先按其它条件从库里查，再在内存里对解密后的明文做包含匹配。
        List<Student> base = studentMapper.search(name, null, status, licenseType, coachId);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : base) {
            String plain = idCardCrypto.decrypt(s.getIdCard());
            if (idCard != null && !idCard.isBlank()) {
                boolean hit = (plain != null && plain.contains(idCard))
                        || (plain == null && s.getIdCard().contains(idCard));
                if (!hit) continue;
            }
            rows.add(studentToMap(s));
        }
        return PageResult.of(rows, page, size);
    }

    @Override
    public Map<String, Object> studentDetail(Long id) {
        Student s = studentMapper.findById(id);
        if (s == null) throw new BizException(404, "学员不存在");
        Map<String, Object> m = studentToMap(s);

        // 附各科目已通过学时与结业判定
        Map<String, BigDecimal> doneMap = doneHoursMap(id);
        m.put("hoursBySubject", doneMap);
        m.put("doneHours", doneMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        List<TrainCourse> courses = trainCourseMapper.findByLicenseType(s.getLicenseType());
        BigDecimal required = courses.stream().map(TrainCourse::getRequiredHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("requiredHours", required);
        boolean hoursOk = courses.stream()
                .allMatch(c -> doneMap.getOrDefault(c.getSubjectCode(), BigDecimal.ZERO)
                        .compareTo(c.getRequiredHours()) >= 0);
        boolean examsOk = examRecordMapper.countPassedSubjects(id) >= 4;
        m.put("hoursOk", hoursOk);
        m.put("examsOk", examsOk);
        m.put("graduationReady", hoursOk && examsOk);

        m.put("payments", paymentMapper.findByStudentId(id));
        m.put("bookings", bookingMapper.findByStudentId(id));

        List<Map<String, Object>> hoursList = new ArrayList<>();
        for (HoursRecord r : hoursMapper.findByStudentId(id)) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("id", r.getId());
            h.put("subjectCode", r.getSubjectCode());
            h.put("subjectName", Enums.SUBJECTS.getOrDefault(r.getSubjectCode(), "科目" + r.getSubjectCode()));
            h.put("hours", r.getHours());
            h.put("trainDate", r.getTrainDate());
            h.put("auditStatus", r.getAuditStatus());
            h.put("auditOpinion", r.getAuditOpinion());
            hoursList.add(h);
        }
        m.put("hoursRecords", hoursList);
        return m;
    }

    @Override
    @Transactional
    public Map<String, Object> saveStudent(Map<String, Object> payload) {
        Long id = toLong(payload.get("id"));
        String name = str(payload.get("name"));
        String phone = str(payload.get("phone"));
        String licenseType = strOrDefault(payload.get("licenseType"), "C1");
        String status = strOrDefault(payload.get("status"), "待审核");
        String idCard = str(payload.get("idCard"));

        Student student;
        User user;

        if (id != null) {
            student = studentMapper.findById(id);
            if (student == null) throw new BizException(404, "学员不存在");
            user = userMapper.findById(student.getUserId());
        } else {
            // 新增学员：先建登录账号，再建档案
            if (name == null || name.isBlank()) throw new BizException(400, "请输入姓名");
            String username = strOrDefault(payload.get("username"),
                    "st" + System.currentTimeMillis() % 100000);
            if (userMapper.findByUsername(username) != null) {
                throw new BizException(400, "用户名 " + username + " 已存在");
            }
            user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(strOrDefault(payload.get("password"), "123456")));
            user.setName(name);
            user.setRole("student");
            user.setStatus(1);
            userMapper.insert(user);

            student = new Student();
            student.setUserId(user.getId());
        }

        if (name != null && !name.isBlank()) {
            user.setName(name);
            user.setRole(user.getRole() == null ? "student" : user.getRole());
            userMapper.update(user);
        }

        if (idCard != null && !idCard.isBlank() && !idCard.contains("*")) {
            student.setIdCard(idCardCrypto.encrypt(idCard));
        } else if (student.getIdCard() == null) {
            student.setIdCard(idCardCrypto.encrypt("PENDING-" + System.currentTimeMillis()));
        }
        if (phone != null) student.setPhone(phone);
        student.setLicenseType(licenseType);
        student.setStatus(status);
        student.setEnrollDate(toDate(payload.get("enrollDate"),
                student.getEnrollDate() == null ? LocalDate.now() : student.getEnrollDate()));
        student.setCoachId(toLong(payload.get("coachId")));
        student.setVehicleId(toLong(payload.get("vehicleId")));

        if (id == null) studentMapper.insert(student);
        else studentMapper.update(student);

        return studentToMap(studentMapper.findById(student.getId()));
    }

    @Override
    @Transactional
    public void auditStudent(Long id, boolean pass, String opinion) {
        Student s = studentMapper.findById(id);
        if (s == null) throw new BizException(404, "学员不存在");
        if (pass) {
            studentMapper.updateStatus(id, "在训");
        } else {
            // 需求 3.1：审核不通过 → 退学（并记录意见到缴费备注不适用，这里仅改状态）
            studentMapper.updateStatus(id, "退学");
        }
    }

    @Override
    public void assignCoachAndVehicle(Long id, Long coachId, Long vehicleId) {
        Student s = studentMapper.findById(id);
        if (s == null) throw new BizException(404, "学员不存在");
        if (coachId != null) {
            Coach c = coachMapper.findById(coachId);
            if (c == null) throw new BizException(404, "教练不存在");
            if (!"在职".equals(c.getStatus())) throw new BizException(400, "该教练已停用，无法分配");
        }
        if (vehicleId != null) {
            Vehicle v = vehicleMapper.findById(vehicleId);
            if (v == null) throw new BizException(404, "车辆不存在");
            if (coachId != null && v.getCoachId() != null && !v.getCoachId().equals(coachId)) {
                throw new BizException(400, "该车辆不属于所选教练");
            }
        }
        studentMapper.assignCoach(id, coachId, vehicleId);
    }

    @Override
    public void changeStudentStatus(Long id, String status) {
        if (!List.of("在训", "结业", "退学", "待审核").contains(status)) {
            throw new BizException(400, "不支持的学员状态：" + status);
        }
        Student s = studentMapper.findById(id);
        if (s == null) throw new BizException(404, "学员不存在");
        studentMapper.updateStatus(id, status);
    }

    @Override
    public void deleteStudent(Long id) {
        Student s = studentMapper.findById(id);
        if (s == null) throw new BizException(404, "学员不存在");
        // 需求 6.1：主数据表逻辑删除
        studentMapper.softDelete(id);
    }

    /* =========================== F12 教练与车辆管理 =========================== */

    @Override
    public List<Map<String, Object>> listCoaches(String keyword) {
        List<Coach> list = (keyword == null || keyword.isBlank())
                ? coachMapper.findAll() : coachMapper.searchByKeyword(keyword);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Coach c : list) {
            User u = userMapper.findById(c.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("userId", c.getUserId());
            m.put("code", c.getCode());
            m.put("name", u == null ? "" : u.getName());
            m.put("username", u == null ? "" : u.getUsername());
            m.put("phone", c.getPhone());
            m.put("licenseTypes", c.getLicenseTypes());
            m.put("licenseTypeList", Enums.splitTypes(c.getLicenseTypes()));
            m.put("status", c.getStatus());
            m.put("studentCount", studentMapper.findByCoachId(c.getId()).size());
            m.put("accountStatus", u == null ? 1 : u.getStatus());
            rows.add(m);
        }
        return rows;
    }

    @Override
    @Transactional
    public Coach saveCoach(Map<String, Object> payload) {
        Long id = toLong(payload.get("id"));
        String code = str(payload.get("code"));
        String name = str(payload.get("name"));
        String phone = str(payload.get("phone"));
        String status = strOrDefault(payload.get("status"), "在职");

        Object types = payload.get("licenseTypes");
        String licenseTypes;
        if (types instanceof List<?> l) {
            licenseTypes = String.join(",", l.stream().map(String::valueOf).toList());
        } else {
            licenseTypes = strOrDefault(types, "C1");
        }

        Coach coach;
        if (id != null) {
            coach = coachMapper.findById(id);
            if (coach == null) throw new BizException(404, "教练不存在");
            if (name != null && !name.isBlank()) {
                User u = userMapper.findById(coach.getUserId());
                if (u != null) {
                    u.setName(name);
                    u.setRole("coach");
                    userMapper.update(u);
                }
            }
        } else {
            if (code == null || code.isBlank()) throw new BizException(400, "请输入教练工号");
            if (name == null || name.isBlank()) throw new BizException(400, "请输入教练姓名");

            // 教练档案必须绑定登录账号（需求 2：教练由管理员后台建号）
            String username = strOrDefault(payload.get("username"), code.toLowerCase());
            if (userMapper.findByUsername(username) != null) {
                throw new BizException(400, "用户名 " + username + " 已存在");
            }
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(strOrDefault(payload.get("password"), "123456")));
            u.setName(name);
            u.setRole("coach");
            u.setStatus(1);
            userMapper.insert(u);

            coach = new Coach();
            coach.setUserId(u.getId());
            coach.setCode(code);
        }

        coach.setPhone(phone);
        coach.setLicenseTypes(licenseTypes);
        coach.setStatus(status);

        if (id != null) coachMapper.update(coach);
        else coachMapper.insert(coach);

        return coachMapper.findById(coach.getId());
    }

    @Override
    public void deleteCoach(Long id) {
        Coach c = coachMapper.findById(id);
        if (c == null) throw new BizException(404, "教练不存在");
        long teaching = studentMapper.findByCoachId(id).stream()
                .filter(s -> "在训".equals(s.getStatus())).count();
        if (teaching > 0) {
            throw new BizException(400, "该教练仍带教 " + teaching + " 名在训学员，请先转移后再停用");
        }
        // 停用 + 逻辑删除
        c.setStatus("停用");
        coachMapper.update(c);
        coachMapper.softDelete(id);
    }

    @Override
    public List<Vehicle> listVehicles(String keyword) {
        return (keyword == null || keyword.isBlank())
                ? vehicleMapper.findAll() : vehicleMapper.searchByKeyword(keyword);
    }

    @Override
    public Vehicle saveVehicle(Vehicle vehicle) {
        if (vehicle.getPlateNo() == null || vehicle.getPlateNo().isBlank()) {
            throw new BizException(400, "请输入车牌号");
        }
        if (vehicle.getId() != null) {
            if (vehicle.getStatus() == null) {
                Vehicle old = vehicleMapper.findById(vehicle.getId());
                vehicle.setStatus(old == null ? "可用" : old.getStatus());
            }
            vehicleMapper.update(vehicle);
        } else {
            if (vehicle.getStatus() == null) vehicle.setStatus("可用");
            vehicleMapper.insert(vehicle);
        }
        return vehicleMapper.findById(vehicle.getId());
    }

    @Override
    public void changeVehicleStatus(Long id, String status) {
        if (!List.of("可用", "维修", "停用").contains(status)) {
            throw new BizException(400, "不支持的车辆状态：" + status);
        }
        if (vehicleMapper.findById(id) == null) throw new BizException(404, "车辆不存在");
        vehicleMapper.updateStatus(id, status);
    }

    @Override
    public void deleteVehicle(Long id) {
        if (vehicleMapper.findById(id) == null) throw new BizException(404, "车辆不存在");
        vehicleMapper.softDelete(id);
    }

    /* =========================== 私有辅助 =========================== */

    private Map<String, Object> studentToMap(Student s) {
        User u = userMapper.findById(s.getUserId());
        Coach coach = s.getCoachId() == null ? null : coachMapper.findById(s.getCoachId());
        Vehicle vehicle = s.getVehicleId() == null ? null : vehicleMapper.findById(s.getVehicleId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("userId", s.getUserId());
        m.put("name", u == null ? "" : u.getName());
        m.put("username", u == null ? "" : u.getUsername());
        // 需求 6.1：列表展示脱敏（保留前 6 后 4）
        m.put("idCard", idCardCrypto.mask(s.getIdCard()));
        m.put("phone", s.getPhone());
        m.put("licenseType", s.getLicenseType());
        m.put("enrollDate", s.getEnrollDate());
        m.put("status", s.getStatus());
        m.put("coachId", s.getCoachId());
        m.put("coachName", coach == null ? null : userName(coach.getUserId()));
        m.put("vehicleId", s.getVehicleId());
        m.put("vehiclePlate", vehicle == null ? null : vehicle.getPlateNo());

        List<Payment> pays = paymentMapper.findByStudentId(s.getId());
        String payStatus = pays.isEmpty() ? "未缴"
                : pays.stream().anyMatch(p -> "未缴".equals(p.getStatus())) ? "未缴"
                : pays.stream().anyMatch(p -> "退费".equals(p.getStatus())) ? "退费" : "已缴";
        m.put("payment", payStatus);
        return m;
    }

    private String userName(Long userId) {
        if (userId == null) return "";
        User u = userMapper.findById(userId);
        return u == null ? "" : u.getName();
    }

    private Map<String, BigDecimal> doneHoursMap(Long studentId) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : hoursMapper.sumApprovedBySubject(studentId)) {
            Object code = row.get("subject_code") != null ? row.get("subject_code") : row.get("SUBJECT_CODE");
            Object h = row.get("hours") != null ? row.get("hours") : row.get("HOURS");
            if (code != null) map.put(String.valueOf(code), toDecimal(h));
        }
        return map;
    }

    private static BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    private static Long toLong(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String strOrDefault(Object v, String def) {
        String s = str(v);
        return (s == null || s.isBlank()) ? def : s;
    }

    private static LocalDate toDate(Object v, LocalDate def) {
        if (v == null || v.toString().isBlank()) return def;
        try {
            return LocalDate.parse(v.toString().substring(0, 10));
        } catch (Exception e) {
            return def;
        }
    }

    /* 以下方法在 AdminServiceImplPart2 中实现（拆分文件不利于阅读，故集中于此） */

    @Override
    public PageResult<Map<String, Object>> listBookings(String status, int page, int size) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Booking b : bookingMapper.searchByStatus(status)) {
            Long sid = b.getScheduleId();
            Long stid = b.getStudentId();
            Student s = studentMapper.findById(stid);
            User u = s == null ? null : userMapper.findById(s.getUserId());
            CoachSchedule sch = scheduleMapper.findById(sid);
            Coach coach = sch == null ? null : coachMapper.findById(sch.getCoachId());
            Vehicle v = sch == null || sch.getVehicleId() == null
                    ? null : vehicleMapper.findById(sch.getVehicleId());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("studentId", b.getStudentId());
            m.put("studentName", u == null ? "" : u.getName());
            m.put("coachId", coach == null ? null : coach.getId());
            m.put("coachName", coach == null ? null : userName(coach.getUserId()));
            m.put("vehiclePlate", v == null ? null : v.getPlateNo());
            m.put("scheduleId", b.getScheduleId());
            if (sch != null) {
                m.put("scheduleDate", sch.getScheduleDate());
                m.put("startTime", sch.getStartTime());
                m.put("endTime", sch.getEndTime());
            }
            m.put("status", b.getStatus());
            m.put("createTime", b.getCreateTime());
            rows.add(m);
        }
        return PageResult.of(rows, page, size);
    }

    @Override
    @Transactional
    public void changeBookingStatus(Long bookingId, String status) {
        Booking b = bookingMapper.findById(bookingId);
        if (b == null) throw new BizException(404, "预约不存在");
        if (!List.of("已确认", "已取消", "已完成", "爽约").contains(status)) {
            throw new BizException(400, "不支持的预约状态：" + status);
        }
        bookingMapper.updateStatus(bookingId, status);

        CoachSchedule s = scheduleMapper.findById(b.getScheduleId());
        // 预约结束（已完成 / 已取消 / 爽约）后释放时段容量，让时段回到「可约」
        if (s != null && !List.of("待确认", "已确认").contains(status)) {
            long active = bookingMapper.countActiveBySchedule(s.getId());
            String newStatus = "停用".equals(s.getStatus()) ? "停用"
                    : active >= s.getCapacity() ? "已满" : "可约";
            scheduleMapper.updateBooked(s.getId(), (int) active, newStatus);
        }
    }

    @Override
    public PageResult<Map<String, Object>> listHours(Long studentId, String subjectCode,
                                                     String auditStatus, int page, int size) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HoursRecord r : hoursMapper.search(studentId, subjectCode, auditStatus, null)) {
            Student s = studentMapper.findById(r.getStudentId());
            User su = s == null ? null : userMapper.findById(s.getUserId());
            Coach c = coachMapper.findById(r.getCoachId());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("studentId", r.getStudentId());
            m.put("studentName", su == null ? "" : su.getName());
            m.put("coachId", r.getCoachId());
            m.put("coachName", c == null ? null : userName(c.getUserId()));
            m.put("subjectCode", r.getSubjectCode());
            m.put("subjectName", Enums.SUBJECTS.getOrDefault(r.getSubjectCode(), "科目" + r.getSubjectCode()));
            m.put("hours", r.getHours());
            m.put("trainDate", r.getTrainDate());
            m.put("auditStatus", r.getAuditStatus());
            m.put("auditOpinion", r.getAuditOpinion());
            m.put("auditTime", r.getAuditTime());
            rows.add(m);
        }
        return PageResult.of(rows, page, size);
    }

    @Override
    @Transactional
    public void auditHours(Long id, boolean pass, String opinion, Long auditorId) {
        HoursRecord r = hoursMapper.findById(id);
        if (r == null) throw new BizException(404, "学时记录不存在");
        if (!"待审核".equals(r.getAuditStatus())) {
            throw new BizException(400, "该记录已审核，不能重复处理");
        }
        // 驳回必须说明原因（需求 3.2 / F13）
        if (!pass && (opinion == null || opinion.isBlank())) {
            throw new BizException(400, "驳回学时需填写原因");
        }
        hoursMapper.audit(id, pass ? "已通过" : "已驳回", auditorId, opinion);
    }

    @Override
    public List<TrainCourse> listCourses() {
        return trainCourseMapper.findAll();
    }

    @Override
    public void updateCourseHours(Long id, BigDecimal requiredHours) {
        if (requiredHours == null || requiredHours.signum() < 0) {
            throw new BizException(400, "学时必须为非负数");
        }
        trainCourseMapper.updateHours(id, requiredHours);
    }

    /* ============================== F14 考试管理 ============================== */

    @Override
    public List<ExamPlan> listExamPlans() {
        return examPlanMapper.findAll();
    }

    @Override
    public ExamPlan saveExamPlan(ExamPlan plan) {
        if (plan.getSubjectCode() == null || plan.getSubjectCode().isBlank()) {
            throw new BizException(400, "请选择科目");
        }
        if (plan.getExamDate() == null) throw new BizException(400, "请选择考试日期");
        if (plan.getVenue() == null || plan.getVenue().isBlank()) {
            throw new BizException(400, "请输入考场");
        }
        if (plan.getCapacity() == null || plan.getCapacity() < 1) {
            throw new BizException(400, "容量必须大于 0");
        }
        if (plan.getId() != null) {
            ExamPlan old = examPlanMapper.findById(plan.getId());
            if (old == null) throw new BizException(404, "考试计划不存在");
            if (plan.getStatus() == null) plan.setStatus(old.getStatus());
            examPlanMapper.update(plan);
        } else {
            if (plan.getStatus() == null) plan.setStatus("报名中");
            plan.setEnrolledCount(0);
            examPlanMapper.insert(plan);
        }
        return examPlanMapper.findById(plan.getId());
    }

    @Override
    public void changeExamPlanStatus(Long id, String status) {
        if (!List.of("报名中", "已截止", "已完成").contains(status)) {
            throw new BizException(400, "不支持的考试计划状态：" + status);
        }
        if (examPlanMapper.findById(id) == null) throw new BizException(404, "考试计划不存在");
        examPlanMapper.updateStatus(id, status);
    }

    @Override
    public void deleteExamPlan(Long id) {
        if (examPlanMapper.findById(id) == null) throw new BizException(404, "考试计划不存在");
        examPlanMapper.softDelete(id);
    }

    @Override
    public PageResult<ExamRecordView> listExamRecords(Long planId, String status, int page, int size) {
        List<ExamRecordView> views = examRecordMapper.findView(null, null, status);
        if (planId != null) {
            views = views.stream().filter(v -> planId.equals(v.getPlanId())).toList();
        }
        return PageResult.of(views, page, size);
    }

    @Override
    @Transactional
    public void saveExamScore(Long recordId, BigDecimal score) {
        ExamRecord r = examRecordMapper.findById(recordId);
        if (r == null) throw new BizException(404, "考试记录不存在");
        if (score == null || score.signum() < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException(400, "请输入 0-100 之间的成绩");
        }

        // 需求 3.3：合格 → 科目通过；不合格 → 生成补考记录
        boolean passed = score.compareTo(BigDecimal.valueOf(90)) >= 0;
        r.setScore(score);
        r.setExamDate(LocalDate.now());
        r.setStatus(passed ? "合格" : "补考中");
        examRecordMapper.updateScore(r);

        if (!passed) {
            ExamRecord retake = new ExamRecord();
            retake.setPlanId(r.getPlanId());
            retake.setStudentId(r.getStudentId());
            retake.setAttemptNo(r.getAttemptNo() == null ? 2 : r.getAttemptNo() + 1);
            retake.setStatus("待考");
            examRecordMapper.insert(retake);
            examPlanMapper.addEnrolled(r.getPlanId(), 1);
        }
    }

    @Override
    public void confirmRoster(Long planId) {
        ExamPlan plan = examPlanMapper.findById(planId);
        if (plan == null) throw new BizException(404, "考试计划不存在");
        if ("报名中".equals(plan.getStatus())) {
            examPlanMapper.updateStatus(planId, "已截止");
        }
    }

    /* ============================== F16 用户管理 ============================== */

    @Override
    public PageResult<User> listUsers(String keyword, String role, int page, int size) {
        List<User> users = userMapper.search(keyword, role);
        // 不把密码带出接口
        users.forEach(u -> u.setPassword(null));
        return PageResult.of(users, page, size);
    }

    @Override
    @Transactional
    public User createUser(Map<String, Object> payload) {
        String username = str(payload.get("username"));
        String name = str(payload.get("name"));
        String role = strOrDefault(payload.get("role"), "student");
        if (username == null || username.isBlank()) throw new BizException(400, "请输入用户名");
        if (name == null || name.isBlank()) throw new BizException(400, "请输入姓名");
        if (!Enums.ROLES.contains(role)) throw new BizException(400, "角色只能是 admin / coach / student");
        if (userMapper.findByUsername(username) != null) {
            throw new BizException(400, "该用户名已存在");
        }

        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(strOrDefault(payload.get("password"), "123456")));
        u.setName(name);
        u.setRole(role);
        u.setStatus(toLong(payload.get("status")) == null ? 1 : toLong(payload.get("status")).intValue());
        userMapper.insert(u);

        // 建对应业务档案，保证登录后能定位到自己的数据
        if ("student".equals(role)) {
            Student s = new Student();
            s.setUserId(u.getId());
            s.setIdCard(idCardCrypto.encrypt("PENDING-" + u.getId()));
            s.setPhone(strOrDefault(payload.get("phone"), ""));
            s.setLicenseType(strOrDefault(payload.get("licenseType"), "C1"));
            s.setEnrollDate(LocalDate.now());
            s.setStatus("待审核");
            studentMapper.insert(s);
        }

        u.setPassword(null);
        return u;
    }

    @Override
    public void updateUser(Long id, Map<String, Object> payload) {
        User u = userMapper.findById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        String name = str(payload.get("name"));
        String role = str(payload.get("role"));
        if (name != null && !name.isBlank()) u.setName(name);
        if (role != null && Enums.ROLES.contains(role)) u.setRole(role);
        userMapper.update(u);
    }

    @Override
    public void changeUserStatus(Long id, Integer status) {
        User u = userMapper.findById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if ("admin".equals(u.getUsername()) && status != null && status == 0) {
            throw new BizException(400, "内置管理员账号不可停用");
        }
        userMapper.updateStatus(id, status == null ? 1 : status);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        if (userMapper.findById(id) == null) throw new BizException(404, "用户不存在");
        userMapper.updatePassword(id, passwordEncoder.encode(
                (newPassword == null || newPassword.isBlank()) ? "123456" : newPassword));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User u = userMapper.findById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if ("admin".equals(u.getUsername())) throw new BizException(400, "内置管理员账号不可删除");

        // 有业务档案时只做逻辑删除，避免外键约束失败
        boolean hasProfile = ("student".equals(u.getRole()) && studentMapper.findByUserId(id) != null)
                || ("coach".equals(u.getRole()) && coachMapper.findByUserId(id) != null);
        if (hasProfile) userMapper.softDelete(id);
        else userMapper.hardDelete(id);
    }

    /* ================================ 缴费 ================================ */

    @Override
    public List<Payment> listPayments(Long studentId) {
        return studentId == null ? paymentMapper.findAll() : paymentMapper.findByStudentId(studentId);
    }

    @Override
    public Payment savePayment(Payment payment) {
        if (payment.getStudentId() == null) throw new BizException(400, "请选择学员");
        if (payment.getItem() == null || payment.getItem().isBlank()) {
            throw new BizException(400, "请选择缴费项目");
        }
        if (payment.getAmount() == null) throw new BizException(400, "请输入金额");
        if (payment.getStatus() == null) payment.setStatus("未缴");

        if (payment.getId() != null) paymentMapper.update(payment);
        else paymentMapper.insert(payment);
        return paymentMapper.findById(payment.getId());
    }

    @Override
    public void registerPayment(Long studentId, String item, BigDecimal amount, String payMethod,
                                Long operatorId) {
        if (studentMapper.findById(studentId) == null) throw new BizException(404, "学员不存在");
        Payment p = new Payment();
        p.setStudentId(studentId);
        p.setItem(item == null || item.isBlank() ? "培训费" : item);
        p.setAmount(amount == null ? BigDecimal.ZERO : amount);
        p.setPayTime(LocalDateTime.now());
        p.setPayMethod(payMethod);
        p.setStatus("已缴");
        p.setOperatorId(operatorId);
        paymentMapper.insert(p);
    }

    @Override
    public void deletePayment(Long id) {
        if (paymentMapper.findById(id) == null) throw new BizException(404, "缴费记录不存在");
        paymentMapper.delete(id);
    }

    /* ============================ F15 统计报表 ============================ */

    @Override
    public Map<String, Object> statistics(String start, String end, String licenseType,
                                          String subject, Long coachId, String range) {
        LocalDate today = LocalDate.now();
        LocalDate from = (start == null || start.isBlank())
                ? today.minusMonths(11).withDayOfMonth(1) : LocalDate.parse(start);
        LocalDate to = (end == null || end.isBlank()) ? today : LocalDate.parse(end);

        Map<String, Object> result = new LinkedHashMap<>();

        // 1) 报名量趋势
        List<Map<String, Object>> enrollTrend = new ArrayList<>();
        List<Student> allStudents = studentMapper.findAll();
        for (LocalDate d = from.withDayOfMonth(1); !d.isAfter(to); d = d.plusMonths(1)) {
            LocalDate monthStart = d;
            LocalDate monthEnd = d.plusMonths(1);
            long c = allStudents.stream()
                    .filter(s -> s.getEnrollDate() != null
                            && !s.getEnrollDate().isBefore(monthStart)
                            && s.getEnrollDate().isBefore(monthEnd))
                    .filter(s -> licenseType == null || licenseType.isBlank()
                            || licenseType.equals(s.getLicenseType()))
                    .count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", YearMonth.from(d).toString());
            m.put("count", c);
            enrollTrend.add(m);
        }
        result.put("enrollTrend", enrollTrend);

        // 2) 准驾车型分布
        List<Map<String, Object>> licenseDist = new ArrayList<>();
        for (String type : Enums.LICENSE_TYPES) {
            long c = allStudents.stream().filter(s -> type.equals(s.getLicenseType())).count();
            if (c > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", type);
                m.put("value", c);
                licenseDist.add(m);
            }
        }
        result.put("licenseDistribution", licenseDist);

        // 3) 各科目通过率与补考率
        List<Map<String, Object>> passRate = new ArrayList<>();
        for (Map<String, Object> row : examRecordMapper.statsBySubject()) {
            String code = String.valueOf(row.get("subjectCode"));
            if (subject != null && !subject.isBlank() && !subject.equals(code)) continue;
            long total = ((Number) row.get("total")).longValue();
            long passed = ((Number) row.get("passed")).longValue();
            long retake = ((Number) row.get("retake")).longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subject", "科目" + code);
            m.put("subjectCode", code);
            m.put("passRate", total == 0 ? 0 : Math.round(passed * 100.0 / total));
            m.put("retakeRate", total == 0 ? 0 : Math.round(retake * 100.0 / total));
            passRate.add(m);
        }
        result.put("subjectPassRate", passRate);

        // 4) 教练工作量（月学时 + 带教学员数）
        List<Map<String, Object>> workload = new ArrayList<>();
        for (Coach c : coachMapper.findActive()) {
            if (coachId != null && !coachId.equals(c.getId())) continue;
            List<HoursRecord> coachHours = hoursMapper.findByCoachId(c.getId()).stream()
                    .filter(r -> "已通过".equals(r.getAuditStatus()))
                    .filter(r -> !r.getTrainDate().isBefore(from) && !r.getTrainDate().isAfter(to))
                    .filter(r -> subject == null || subject.isBlank() || subject.equals(r.getSubjectCode()))
                    .toList();
            double hours = coachHours.stream().mapToDouble(r -> r.getHours().doubleValue()).sum();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("coachId", c.getId());
            m.put("coach", userName(c.getUserId()));
            m.put("students", studentMapper.findByCoachId(c.getId()).size());
            m.put("hours", BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP));
            workload.add(m);
        }
        result.put("coachWorkload", workload);

        // 5) 汇总指标
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("studentCount", allStudents.size());
        summary.put("trainingCount", studentMapper.countByStatus("在训"));
        Double hoursSum = hoursMapper.sumApprovedInRange(from, to);
        summary.put("approvedHours", hoursSum == null ? 0 : hoursSum);
        long scored = examRecordMapper.countScored();
        long passed = examRecordMapper.countScoredPassed();
        summary.put("passRate", scored == 0 ? 0 : Math.round(passed * 1000.0 / scored) / 10.0);
        result.put("summary", summary);

        return result;
    }
}
