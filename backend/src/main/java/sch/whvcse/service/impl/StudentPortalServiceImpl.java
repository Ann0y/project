package sch.whvcse.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import sch.whvcse.service.StudentPortalService;
import sch.whvcse.util.IdCardCrypto;
import sch.whvcse.vo.ExamRecordView;
import sch.whvcse.vo.ProgressVO;

@Service
public class StudentPortalServiceImpl implements StudentPortalService {

    /*
     * 学员端业务规则摘要
     *   约车：只能约本人带教教练的时段，且只能约今天起 BOOKING_WINDOW_DAYS 天内的；
     *         一名学员最多同时持有 MAX_ACTIVE_BOOKINGS 条未完成预约（待确认 / 已确认）。
     *   考试：本科学时达标 + 按科目顺序报名（上一科目需已有报名记录）。
     */

    /** 学员可预约的时间窗口（天）：今天起算 5 天 */
    private static final int BOOKING_WINDOW_DAYS = 5;

    /**
     * 一名学员最多同时持有的「未完成」预约数。
     * 未完成 = 待确认 / 已确认；已完成、已取消、爽约都不计入。
     */
    private static final int MAX_ACTIVE_BOOKINGS = 2;

    /** 科目报考顺序：后一科目需先有上一科目的报名记录 */
    private static final List<String> SUBJECT_ORDER = List.of("1", "2", "3", "4");

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

    public StudentPortalServiceImpl(StudentMapper studentMapper,
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
                                    IdCardCrypto idCardCrypto) {
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
    }

    /* ======================= F02 我的培训进度 ======================= */

    @Override
    public ProgressVO myProgress(Long studentId) {
        Student student = requireStudent(studentId);
        User user = userMapper.findById(student.getUserId());

        ProgressVO vo = new ProgressVO();
        vo.setStudentId(student.getId());
        vo.setName(user == null ? "" : user.getName());
        vo.setLicenseType(student.getLicenseType());
        vo.setStatus(student.getStatus());
        vo.setEnrollDate(student.getEnrollDate());
        vo.setPhone(student.getPhone());
        vo.setIdCardMasked(idCardCrypto.mask(student.getIdCard()));

        // 缴费状态：有未缴即「未缴」，全缴清为「已缴」
        List<Payment> payments = paymentMapper.findByStudentId(studentId);
        String payStatus = payments.isEmpty() ? "未缴"
                : payments.stream().anyMatch(p -> "未缴".equals(p.getStatus())) ? "未缴"
                : payments.stream().anyMatch(p -> "退费".equals(p.getStatus())) ? "退费" : "已缴";
        vo.setPaymentStatus(payStatus);

        // 固定带教关系
        if (student.getCoachId() != null) {
            Coach coach = coachMapper.findById(student.getCoachId());
            if (coach != null) {
                vo.setCoachId(coach.getId());
                vo.setCoachPhone(coach.getPhone());
                User cu = userMapper.findById(coach.getUserId());
                vo.setCoachName(cu == null ? "" : cu.getName());
            }
        }
        if (student.getVehicleId() != null) {
            Vehicle v = vehicleMapper.findById(student.getVehicleId());
            if (v != null) {
                vo.setVehicleId(v.getId());
                vo.setVehiclePlate(v.getPlateNo());
                vo.setVehicleModel(v.getModel());
            }
        }

        // 各科目已通过学时
        Map<String, BigDecimal> doneBySubject = new HashMap<>();
        for (Map<String, Object> row : hoursMapper.sumApprovedBySubject(studentId)) {
            Object code = row.get("subject_code") != null ? row.get("subject_code") : row.get("SUBJECT_CODE");
            Object h = row.get("hours") != null ? row.get("hours") : row.get("HOURS");
            if (code != null) {
                doneBySubject.put(String.valueOf(code), toDecimal(h));
            }
        }
        vo.setDoneHoursBySubject(doneBySubject);

        // 按准驾车型取大纲要求
        List<TrainCourse> courses = trainCourseMapper.findByLicenseType(student.getLicenseType());
        if (courses.isEmpty()) courses = trainCourseMapper.findByLicenseType("C1");

        BigDecimal doneTotal = BigDecimal.ZERO;
        BigDecimal requiredTotal = BigDecimal.ZERO;
        List<ProgressVO.SubjectProgress> subjects = new ArrayList<>();
        boolean hoursOk = true;

        // 各科目完成率之和，用于计算「整体完成率 = 4 科完成率的平均值」
        BigDecimal percentSum = BigDecimal.ZERO;
        int percentCount = 0;

        for (TrainCourse c : courses) {
            BigDecimal done = doneBySubject.getOrDefault(c.getSubjectCode(), BigDecimal.ZERO);
            BigDecimal required = c.getRequiredHours();
            doneTotal = doneTotal.add(done);
            requiredTotal = requiredTotal.add(required);
            if (done.compareTo(required) < 0) hoursOk = false;

            // 单科完成率，封顶 100%（超额学时不计入完成率，避免某项超过 100% 拉高平均）
            int subjectPercent = required.signum() == 0 ? 100
                    : done.multiply(BigDecimal.valueOf(100))
                        .divide(required, 0, RoundingMode.HALF_UP).intValue();
            subjectPercent = Math.min(100, subjectPercent);
            percentSum = percentSum.add(BigDecimal.valueOf(subjectPercent));
            percentCount++;

            ProgressVO.SubjectProgress sp = new ProgressVO.SubjectProgress();
            sp.setCode(c.getSubjectCode());
            sp.setName(c.getSubjectName());
            sp.setDone(done);
            sp.setRequired(required);
            sp.setPercent(subjectPercent);
            subjects.add(sp);
        }
        // 按科目代码排序（1/2/3/4）
        subjects.sort(Comparator.comparing(ProgressVO.SubjectProgress::getCode));

        vo.setSubjects(subjects);
        vo.setDoneHours(doneTotal);
        vo.setRequiredHours(requiredTotal);
        vo.setRemainHours(requiredTotal.subtract(doneTotal).max(BigDecimal.ZERO));
        // 需求：整体完成情况 = 各科目完成率的平均值（4 科等权）
        vo.setPercent(percentCount == 0 ? 0
                : percentSum.divide(BigDecimal.valueOf(percentCount), 0, RoundingMode.HALF_UP).intValue());

        // 结业条件：全部科目学时达标 + 全部科目考试合格（需求 3.3）
        // 科目数按大纲（4 科），考试合格科目数 >= 4 视为全部合格
        long passedSubjects = examRecordMapper.countPassedSubjects(studentId);
        boolean examsOk = passedSubjects >= 4;
        vo.setHoursOk(hoursOk);
        vo.setExamsOk(examsOk);
        vo.setGraduationReady(hoursOk && examsOk);

        vo.setTimeline(buildTimeline(studentId, student));

        return vo;
    }

    /** 最近培训 / 考试 / 报名记录时间轴 */
    private List<ProgressVO.TimelineItem> buildTimeline(Long studentId, Student student) {
        List<ProgressVO.TimelineItem> items = new ArrayList<>();

        if (student.getEnrollDate() != null) {
            items.add(timeline(student.getEnrollDate().toString(), "报名", "完成报名登记"));
        }

        for (HoursRecord r : hoursMapper.findByStudentId(studentId)) {
            if (!"已通过".equals(r.getAuditStatus())) continue;
            items.add(timeline(r.getTrainDate().toString(), "培训",
                    "完成" + Enums.SUBJECTS.getOrDefault(r.getSubjectCode(), "科目" + r.getSubjectCode())
                            + " " + r.getHours().stripTrailingZeros().toPlainString() + " 学时培训"));
        }

        for (ExamRecordView r : examRecordMapper.findViewByStudent(studentId)) {
            if (r.getExamDate() == null) continue;
            String sub = Enums.SUBJECTS.getOrDefault(r.getSubjectCode(), "科目" + r.getSubjectCode());
            String text = r.getScore() == null
                    ? sub + r.getStatus()
                    : sub + " " + r.getStatus() + " " + r.getScore().stripTrailingZeros().toPlainString() + " 分";
            items.add(timeline(r.getExamDate().toString(), "考试", text));
        }

        items.sort(Comparator.comparing(ProgressVO.TimelineItem::getDate).reversed());
        return items.size() > 8 ? items.subList(0, 8) : items;
    }

    private ProgressVO.TimelineItem timeline(String date, String type, String text) {
        ProgressVO.TimelineItem t = new ProgressVO.TimelineItem();
        t.setDate(date);
        t.setType(type);
        t.setText(text);
        return t;
    }

    /* ======================= F03 培训与约车 ======================= */

    @Override
    public List<Map<String, Object>> myAvailableSchedules(Long studentId, String date) {
        Student student = requireStudent(studentId);
        if (student.getCoachId() == null) return List.of(); // 未分配教练则无可约时段

        // 只开放「今天起 5 天内」的时段（今天 ~ 第 4 天），避免过期/过远的时段干扰选择
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(BOOKING_WINDOW_DAYS - 1L);

        LocalDate from = today;
        LocalDate to = windowEnd;
        if (date != null && !date.isBlank()) {
            LocalDate d = LocalDate.parse(date);
            from = d;
            to = d;
        }

        // 固定带教：只查本人带教教练的时段（需求 2）
        List<CoachSchedule> list = scheduleMapper.search(student.getCoachId(), null, from, to);

        List<Map<String, Object>> result = new ArrayList<>();
        for (CoachSchedule s : list) {
            // 过滤掉已过去的时段（当天更早的时间点）
            if (s.getScheduleDate().equals(today) && s.getStartTime().isBefore(LocalTime.now())) {
                continue;
            }
            Map<String, Object> m = scheduleToMap(s);
            // 标注"我已预约"，前端据此禁用该选项
            m.put("bookedByMe", bookingMapper.countActive(studentId, s.getId()) > 0);
            result.add(m);
        }
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> myBookings(Long studentId, String status, int page, int size) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Booking b : bookingMapper.findByStudentId(studentId)) {
            if (status != null && !status.isBlank() && !status.equals(b.getStatus())) continue;
            CoachSchedule s = scheduleMapper.findById(b.getScheduleId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("status", b.getStatus());
            m.put("createTime", b.getCreateTime());
            m.put("scheduleId", b.getScheduleId());
            if (s != null) {
                m.put("scheduleDate", s.getScheduleDate());
                m.put("startTime", s.getStartTime());
                m.put("endTime", s.getEndTime());
                Vehicle v = s.getVehicleId() == null ? null : vehicleMapper.findById(s.getVehicleId());
                m.put("vehiclePlate", v == null ? null : v.getPlateNo());
            }
            rows.add(m);
        }
        return PageResult.of(rows, page, size);
    }

    @Override
    @Transactional
    public Booking book(Long studentId, Long scheduleId) {
        Student student = requireStudent(studentId);
        if (!"在训".equals(student.getStatus())) {
            throw new BizException(400, "当前报名状态为「" + student.getStatus() + "」，仅「在训」学员可预约");
        }

        CoachSchedule s = scheduleMapper.findById(scheduleId);
        if (s == null) throw new BizException(404, "该时段不存在");

        // 固定带教：只能约本人带教教练的时段
        if (student.getCoachId() == null || !student.getCoachId().equals(s.getCoachId())) {
            throw new BizException(403, "只能预约本人带教教练发布的时段");
        }
        if ("停用".equals(s.getStatus())) throw new BizException(400, "该时段已停用");

        // 只允许预约「今天起 5 天内」的时段
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(BOOKING_WINDOW_DAYS - 1L);
        if (s.getScheduleDate().isBefore(today) || s.getScheduleDate().isAfter(windowEnd)) {
            throw new BizException(400, "只能预约今天起 " + BOOKING_WINDOW_DAYS + " 天内的时段");
        }

        // E1 冲突检测：学员本人是否已约同一时段（先查这条，报错更具体）
        if (bookingMapper.countActive(studentId, scheduleId) > 0) {
            throw new BizException(400, "你已预约该时段，不能重复预约");
        }

        // 同一学员最多同时持有 MAX_ACTIVE_BOOKINGS 条未完成预约（待确认 / 已确认）
        long myActive = bookingMapper.countActiveByStudent(studentId);
        if (myActive >= MAX_ACTIVE_BOOKINGS) {
            throw new BizException(400, "你已有 " + MAX_ACTIVE_BOOKINGS
                    + " 个未完成的预约，请先完成或取消后再预约");
        }

        // E1 冲突检测：容量是否已满
        long active = bookingMapper.countActiveBySchedule(scheduleId);
        if (active >= s.getCapacity()) {
            throw new BizException(400, "该时段已约满");
        }

        // 若该学员此前预约过这个时段（已取消 / 爽约），唯一约束会阻止再插入，
        // 这里直接复用原记录重新激活为「待确认」。
        Booking booking = existingBookingOf(studentId, scheduleId);
        if (booking != null) {
            bookingMapper.reactivate(booking.getId(), "待确认");
            booking.setStatus("待确认");
        } else {
            booking = new Booking();
            booking.setStudentId(studentId);
            booking.setScheduleId(scheduleId);
            booking.setStatus("待确认");
            bookingMapper.insert(booking);
        }

        // 同步冗余计数（需求 6.1：booked_count 由业务写入时同步更新）
        int booked = (int) active + 1;
        String status = booked >= s.getCapacity() ? "已满" : s.getStatus();
        scheduleMapper.updateBooked(scheduleId, booked, "停用".equals(s.getStatus()) ? "停用" : status);

        return booking;
    }

    @Override
    @Transactional
    public void cancelBooking(Long studentId, Long bookingId) {
        Booking b = bookingMapper.findById(bookingId);
        if (b == null || !b.getStudentId().equals(studentId)) {
            throw new BizException(404, "预约不存在");
        }
        if (!"待确认".equals(b.getStatus())) {
            throw new BizException(400, "仅「待确认」的预约可以取消");
        }
        bookingMapper.updateStatus(bookingId, "已取消");
        releaseSlot(b.getScheduleId());
    }

    /** 释放时段容量并把「已满」改回「可约」 */
    private void releaseSlot(Long scheduleId) {
        CoachSchedule s = scheduleMapper.findById(scheduleId);
        if (s == null) return;
        long active = bookingMapper.countActiveBySchedule(scheduleId);
        String status = "停用".equals(s.getStatus())
                ? "停用"
                : active >= s.getCapacity() ? "已满" : "可约";
        scheduleMapper.updateBooked(scheduleId, (int) active, status);
    }

    /* ======================= F04 我的考试与成绩 ======================= */

    @Override
    public PageResult<Map<String, Object>> myExamRecords(Long studentId, String subjectCode,
                                                         String status, int page, int size) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamRecordView r : examRecordMapper.findView(studentId, subjectCode, status)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("subjectCode", r.getSubjectCode());
            m.put("subjectName", Enums.SUBJECTS.getOrDefault(r.getSubjectCode(), "科目" + r.getSubjectCode()));
            m.put("attemptNo", r.getAttemptNo());
            m.put("score", r.getScore());
            m.put("status", r.getStatus());
            m.put("examDate", r.getExamDate());
            m.put("registerTime", r.getRegisterTime());
            m.put("venue", r.getVenue());
            m.put("planDate", r.getPlanDate());
            m.put("planTime", r.getExamTime());
            rows.add(m);
        }
        return PageResult.of(rows, page, size);
    }

    @Override
    public List<Map<String, Object>> openExamPlans(Long studentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> done = doneHoursMap(studentId);
        Student st = requireStudent(studentId);

        for (ExamPlan p : examPlanMapper.findOpen()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("subjectCode", p.getSubjectCode());
            m.put("subjectName", Enums.SUBJECTS.getOrDefault(p.getSubjectCode(), "科目" + p.getSubjectCode()));
            m.put("examDate", p.getExamDate());
            m.put("examTime", p.getExamTime());
            m.put("venue", p.getVenue());
            m.put("capacity", p.getCapacity());
            m.put("enrolledCount", p.getEnrolledCount());
            m.put("status", p.getStatus());

            // 报名条件 1：本科学时达标
            TrainCourse c = trainCourseMapper.findByTypeAndSubject(st.getLicenseType(), p.getSubjectCode());
            BigDecimal required = c == null ? BigDecimal.ZERO : c.getRequiredHours();
            BigDecimal mine = done.getOrDefault(p.getSubjectCode(), BigDecimal.ZERO);
            m.put("myHours", mine);
            m.put("requiredHours", required);
            m.put("qualified", mine.compareTo(required) >= 0);

            // 报名条件 2：按科目顺序报名（上一科目需已有报名记录）
            String prev = previousSubject(p.getSubjectCode());
            boolean prerequisiteMet = prev == null
                    || examRecordMapper.countRegisteredBySubject(studentId, prev) > 0;
            m.put("prerequisiteCode", prev);
            m.put("prerequisiteName", prev == null ? null : Enums.subjectShort(prev));
            m.put("prerequisiteMet", prerequisiteMet);

            // 报名条件 3：本科目尚未合格（已合格无需重复报考）
            m.put("passed", examRecordMapper.countPassedBySubject(studentId, p.getSubjectCode()) > 0);

            m.put("registered", examRecordMapper.findByPlanId(p.getId()).stream()
                    .anyMatch(r -> r.getStudentId().equals(studentId)));
            rows.add(m);
        }
        return rows;
    }

    @Override
    @Transactional
    public ExamRecord registerExam(Long studentId, Long planId) {
        Student student = requireStudent(studentId);
        ExamPlan plan = examPlanMapper.findById(planId);
        if (plan == null) throw new BizException(404, "考试计划不存在");
        if (!"报名中".equals(plan.getStatus())) throw new BizException(400, "该场次已截止报名");
        if (plan.getEnrolledCount() >= plan.getCapacity()) throw new BizException(400, "该场次名额已满");

        // 已报名校验（uk_plan_student 唯一约束）
        boolean exists = examRecordMapper.findByPlanId(planId).stream()
                .anyMatch(r -> r.getStudentId().equals(studentId));
        if (exists) throw new BizException(400, "你已报名该场次");

        // 报考顺序：上一科目还没报名时，不允许报名后一科目（科二未报名 → 科三不能报名）
        String prev = previousSubject(plan.getSubjectCode());
        if (prev != null && examRecordMapper.countRegisteredBySubject(studentId, prev) == 0) {
            throw new BizException(400, "请先报名" + Enums.subjectShort(prev)
                    + "，再进行" + Enums.subjectShort(plan.getSubjectCode()) + "报名");
        }

        // 需求 3.3：校验学时是否满足该科目报考条件
        TrainCourse c = trainCourseMapper.findByTypeAndSubject(
                student.getLicenseType(), plan.getSubjectCode());
        BigDecimal required = c == null ? BigDecimal.ZERO : c.getRequiredHours();
        BigDecimal mine = doneHoursMap(studentId).getOrDefault(plan.getSubjectCode(), BigDecimal.ZERO);
        if (mine.compareTo(required) < 0) {
            throw new BizException(400, "该科目学时未达标（" + mine.stripTrailingZeros().toPlainString()
                    + "/" + required.stripTrailingZeros().toPlainString() + "），暂不可报考");
        }

        // 该科目已合格则无需重复报考
        boolean alreadyPassed = examRecordMapper.countPassedBySubject(studentId, plan.getSubjectCode()) > 0;
        if (alreadyPassed) throw new BizException(400, "该科目已合格，无需重复报考");

        ExamRecord record = new ExamRecord();
        record.setPlanId(planId);
        record.setStudentId(studentId);
        record.setAttemptNo(1);
        record.setStatus("待考");
        examRecordMapper.insert(record);

        // 同步冗余计数
        examPlanMapper.addEnrolled(planId, 1);
        return record;
    }

    @Override
    public List<Map<String, Object>> myPayments(Long studentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Payment p : paymentMapper.findByStudentId(studentId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("item", p.getItem());
            m.put("amount", p.getAmount());
            m.put("payTime", p.getPayTime());
            m.put("payMethod", p.getPayMethod());
            m.put("status", p.getStatus());
            rows.add(m);
        }
        return rows;
    }

    /* ======================= 私有辅助 ======================= */

    /** 查该学员与某时段的历史预约记录（含已取消），用于复用而非重复插入 */
    private Booking existingBookingOf(Long studentId, Long scheduleId) {
        for (Booking b : bookingMapper.findByStudentId(studentId)) {
            if (b.getScheduleId().equals(scheduleId)) return b;
        }
        return null;
    }

    private Student requireStudent(Long studentId) {
        Student s = studentMapper.findById(studentId);
        if (s == null) throw new BizException(404, "学员档案不存在");
        return s;
    }

    /** 取报考顺序上的上一个科目代码；科目一没有前置科目，返回 null */
    private String previousSubject(String subjectCode) {
        int idx = SUBJECT_ORDER.indexOf(subjectCode);
        return idx <= 0 ? null : SUBJECT_ORDER.get(idx - 1);
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

    private Map<String, Object> scheduleToMap(CoachSchedule s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("coachId", s.getCoachId());
        m.put("vehicleId", s.getVehicleId());
        m.put("scheduleDate", s.getScheduleDate());
        m.put("startTime", s.getStartTime());
        m.put("endTime", s.getEndTime());
        m.put("capacity", s.getCapacity());
        m.put("bookedCount", s.getBookedCount());
        m.put("status", s.getStatus());
        return m;
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }
}
