package sch.whvcse.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sch.whvcse.common.BizException;
import sch.whvcse.common.Enums;
import sch.whvcse.entity.Booking;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.CoachSchedule;
import sch.whvcse.entity.HoursRecord;
import sch.whvcse.entity.Student;
import sch.whvcse.entity.TrainCourse;
import sch.whvcse.entity.User;
import sch.whvcse.entity.Vehicle;
import sch.whvcse.mapper.BookingMapper;
import sch.whvcse.mapper.CoachMapper;
import sch.whvcse.mapper.CoachScheduleMapper;
import sch.whvcse.mapper.HoursRecordMapper;
import sch.whvcse.mapper.StudentMapper;
import sch.whvcse.mapper.TrainCourseMapper;
import sch.whvcse.mapper.UserMapper;
import sch.whvcse.mapper.VehicleMapper;
import sch.whvcse.service.CoachPortalService;

@Service
public class CoachPortalServiceImpl implements CoachPortalService {

    private final CoachMapper coachMapper;
    private final CoachScheduleMapper scheduleMapper;
    private final BookingMapper bookingMapper;
    private final HoursRecordMapper hoursMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final TrainCourseMapper trainCourseMapper;

    public CoachPortalServiceImpl(CoachMapper coachMapper,
                                  CoachScheduleMapper scheduleMapper,
                                  BookingMapper bookingMapper,
                                  HoursRecordMapper hoursMapper,
                                  StudentMapper studentMapper,
                                  UserMapper userMapper,
                                  VehicleMapper vehicleMapper,
                                  TrainCourseMapper trainCourseMapper) {
        this.coachMapper = coachMapper;
        this.scheduleMapper = scheduleMapper;
        this.bookingMapper = bookingMapper;
        this.hoursMapper = hoursMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.vehicleMapper = vehicleMapper;
        this.trainCourseMapper = trainCourseMapper;
    }

    /* ======================= F05 工作台 ======================= */

    @Override
    public Map<String, Object> workbench(Long coachId) {
        LocalDate today = LocalDate.now();
        List<CoachSchedule> todaySchedules = scheduleMapper.search(coachId, today, null, null);
        List<Booking> bookings = bookingMapper.findByCoachId(coachId);
        List<Student> students = studentMapper.findByCoachId(coachId);

        long pendingBookings = bookings.stream().filter(b -> "待确认".equals(b.getStatus())).count();
        long pendingHours = hoursMapper.findByCoachId(coachId).stream()
                .filter(r -> "待审核".equals(r.getAuditStatus())).count();

        // 今日排课明细（带学员与车辆信息）
        List<Map<String, Object>> todayList = new ArrayList<>();
        for (CoachSchedule s : todaySchedules) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("startTime", s.getStartTime());
            m.put("endTime", s.getEndTime());
            m.put("capacity", s.getCapacity());
            m.put("bookedCount", s.getBookedCount());
            m.put("status", s.getStatus());
            todayList.add(m);
        }

        // 待确认预约明细
        List<Map<String, Object>> pendingList = new ArrayList<>();
        for (Booking b : bookings) {
            if (!"待确认".equals(b.getStatus())) continue;
            pendingList.add(bookingToMap(b));
        }

        // 近 7 天排课量（供前端折线图）
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long count = scheduleMapper.search(coachId, d, null, null).size();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("count", count);
            trend.add(m);
        }

        // 教练档案 + 名下车辆（供排课选择关联车辆）
        Coach coach = coachMapper.findById(coachId);
        Map<String, Object> coachInfo = new LinkedHashMap<>();
        if (coach != null) {
            coachInfo.put("id", coach.getId());
            coachInfo.put("code", coach.getCode());
            coachInfo.put("phone", coach.getPhone());
            coachInfo.put("licenseTypes", coach.getLicenseTypes());
            coachInfo.put("licenseTypeList", Enums.splitTypes(coach.getLicenseTypes()));
            coachInfo.put("status", coach.getStatus());
        }
        List<Map<String, Object>> myVehicles = new ArrayList<>();
        for (Vehicle v : vehicleMapper.findByCoachId(coachId)) {
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("id", v.getId());
            vm.put("plateNo", v.getPlateNo());
            vm.put("model", v.getModel());
            vm.put("status", v.getStatus());
            myVehicles.add(vm);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("coach", coachInfo);
        result.put("vehicles", myVehicles);
        result.put("todayDate", today.toString());
        result.put("todayCount", todaySchedules.size());
        result.put("pendingBookings", pendingBookings);
        result.put("studentCount", students.size());
        result.put("pendingHours", pendingHours);
        result.put("todaySchedules", todayList);
        result.put("pendingList", pendingList);
        result.put("scheduleTrend", trend);
        return result;
    }

    /* ======================= F06 我的排课 ======================= */

    @Override
    public List<CoachSchedule> mySchedules(Long coachId, String date) {
        LocalDate d = (date == null || date.isBlank()) ? null : LocalDate.parse(date);
        return scheduleMapper.search(coachId, d, null, null);
    }

    @Override
    @Transactional
    public CoachSchedule saveSchedule(Long coachId, CoachSchedule payload) {
        if (payload.getScheduleDate() == null
                || payload.getStartTime() == null || payload.getEndTime() == null) {
            throw new BizException(400, "请填写日期、开始时间与结束时间");
        }
        if (!payload.getStartTime().isBefore(payload.getEndTime())) {
            throw new BizException(400, "结束时间必须晚于开始时间");
        }

        // 同一教练同一日期同一起始时间不可重复发布
        long clash = scheduleMapper.countClash(coachId, payload.getScheduleDate(),
                payload.getStartTime(), payload.getId());
        if (clash > 0) throw new BizException(400, "该日期同一时段已存在排课，请勿重复发布");

        payload.setCoachId(coachId);
        if (payload.getCapacity() == null || payload.getCapacity() < 1) payload.setCapacity(1);

        if (payload.getId() != null) {
            CoachSchedule exist = scheduleMapper.findById(payload.getId());
            if (exist == null) throw new BizException(404, "时段不存在");
            if (!exist.getCoachId().equals(coachId)) throw new BizException(403, "只能维护本人的排课");
            if (payload.getStatus() == null) payload.setStatus(exist.getStatus());
            scheduleMapper.update(payload);
            return scheduleMapper.findById(payload.getId());
        }

        payload.setBookedCount(0);
        if (payload.getStatus() == null) payload.setStatus("可约");
        scheduleMapper.insert(payload);
        return scheduleMapper.findById(payload.getId());
    }

    @Override
    public void removeSchedule(Long coachId, Long scheduleId) {
        CoachSchedule s = scheduleMapper.findById(scheduleId);
        if (s == null) throw new BizException(404, "时段不存在");
        if (!s.getCoachId().equals(coachId)) throw new BizException(403, "只能删除本人的排课");
        if (s.getBookedCount() != null && s.getBookedCount() > 0) {
            throw new BizException(400, "该时段已有学员预约，不能删除，可改为「停用」");
        }
        scheduleMapper.softDelete(scheduleId);
    }

    /* ======================= F07 预约处理 ======================= */

    @Override
    public List<Map<String, Object>> assignedBookings(Long coachId, String status, String studentName) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Booking b : bookingMapper.findByCoachId(coachId)) {
            if (status != null && !status.isBlank() && !status.equals(b.getStatus())) continue;

            Map<String, Object> m = bookingToMap(b);
            if (studentName != null && !studentName.isBlank()) {
                String name = String.valueOf(m.get("studentName"));
                if (!name.contains(studentName)) continue;
            }
            rows.add(m);
        }
        return rows;
    }

    @Override
    @Transactional
    public void changeBookingStatus(Long coachId, Long bookingId, String status) {
        Booking b = bookingMapper.findById(bookingId);
        if (b == null) throw new BizException(404, "预约不存在");
        CoachSchedule s = scheduleMapper.findById(b.getScheduleId());
        if (s == null || !s.getCoachId().equals(coachId)) {
            throw new BizException(403, "只能处理自己排课产生的预约");
        }

        List<String> allowed = List.of("已确认", "已取消", "已完成", "爽约");
        if (!allowed.contains(status)) throw new BizException(400, "不支持的预约状态：" + status);

        // 状态流转校验：待确认 → 已确认/已取消；已确认 → 已完成/爽约
        String current = b.getStatus();
        boolean legal = ("待确认".equals(current) && List.of("已确认", "已取消", "爽约").contains(status))
                || ("已确认".equals(current) && List.of("已完成", "已取消", "爽约").contains(status));
        if (!legal) {
            throw new BizException(400, "预约当前状态为「" + current + "」，不能变更为「" + status + "」");
        }

        bookingMapper.updateStatus(bookingId, status);

        // 预约结束（已完成 / 已取消 / 爽约）后释放时段容量，让时段回到「可约」
        if (!List.of("待确认", "已确认").contains(status)) {
            long active = bookingMapper.countActiveBySchedule(s.getId());
            String newStatus = "停用".equals(s.getStatus()) ? "停用"
                    : active >= s.getCapacity() ? "已满" : "可约";
            scheduleMapper.updateBooked(s.getId(), (int) active, newStatus);
        }
    }

    /* ======================= F08 学时登记 ======================= */

    @Override
    public List<Map<String, Object>> myHours(Long coachId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HoursRecord r : hoursMapper.findByCoachId(coachId)) {
            rows.add(hoursToMap(r));
        }
        return rows;
    }

    @Override
    @Transactional
    public HoursRecord createHours(Long coachId, HoursRecord payload) {
        if (payload.getStudentId() == null) throw new BizException(400, "请选择学员");
        if (payload.getSubjectCode() == null || payload.getSubjectCode().isBlank()) {
            throw new BizException(400, "请选择科目");
        }
        if (payload.getHours() == null || payload.getHours().signum() <= 0) {
            throw new BizException(400, "请输入正确的学时");
        }
        if (payload.getTrainDate() == null) payload.setTrainDate(LocalDate.now());

        // 只能给本人带教的学员登记（需求 2 固定带教）
        Student student = studentMapper.findById(payload.getStudentId());
        if (student == null) throw new BizException(404, "学员不存在");
        if (!Objects.equals(student.getCoachId(), coachId)) {
            throw new BizException(403, "只能为本人带教的学员登记学时");
        }

        payload.setId(null);
        payload.setCoachId(coachId);
        payload.setAuditStatus("待审核");
        hoursMapper.insert(payload);
        return hoursMapper.findById(payload.getId());
    }

    /* ======================= F09 我的学员 ======================= */

    @Override
    public List<Map<String, Object>> myStudents(Long coachId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : studentMapper.findByCoachId(coachId)) {
            User u = userMapper.findById(s.getUserId());
            Vehicle v = s.getVehicleId() == null ? null : vehicleMapper.findById(s.getVehicleId());

            // 各科目已通过学时
            Map<String, BigDecimal> doneMap = new HashMap<>();
            for (Map<String, Object> row : hoursMapper.sumApprovedBySubject(s.getId())) {
                Object code = row.get("subject_code") != null ? row.get("subject_code") : row.get("SUBJECT_CODE");
                Object h = row.get("hours") != null ? row.get("hours") : row.get("HOURS");
                if (code != null) doneMap.put(String.valueOf(code), toDecimal(h));
            }
            double total = doneMap.values().stream().mapToDouble(BigDecimal::doubleValue).sum();

            List<TrainCourse> courses = trainCourseMapper.findByLicenseType(s.getLicenseType());
            double required = courses.stream().mapToDouble(c -> c.getRequiredHours().doubleValue()).sum();

            // 整体完成率 = 各科目完成率的平均值（与学员端口径一致）
            double percentSum = 0;
            for (TrainCourse c : courses) {
                double req = c.getRequiredHours().doubleValue();
                double done = doneMap.getOrDefault(c.getSubjectCode(), BigDecimal.ZERO).doubleValue();
                percentSum += req == 0 ? 100 : Math.min(100, done * 100 / req);
            }
            int avgPercent = courses.isEmpty() ? 0 : (int) Math.round(percentSum / courses.size());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", u == null ? "" : u.getName());
            m.put("phone", s.getPhone());
            m.put("licenseType", s.getLicenseType());
            m.put("enrollDate", s.getEnrollDate());
            m.put("status", s.getStatus());
            m.put("vehiclePlate", v == null ? null : v.getPlateNo());
            m.put("doneHours", BigDecimal.valueOf(total));
            m.put("requiredHours", BigDecimal.valueOf(required));
            m.put("hoursBySubject", doneMap);
            m.put("percent", avgPercent);
            rows.add(m);
        }
        return rows;
    }

    /* ======================= 私有辅助 ======================= */

    private Map<String, Object> bookingToMap(Booking b) {
        Student s = studentMapper.findById(b.getStudentId());
        User u = s == null ? null : userMapper.findById(s.getUserId());
        CoachSchedule sch = scheduleMapper.findById(b.getScheduleId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("studentId", b.getStudentId());
        m.put("studentName", u == null ? "" : u.getName());
        m.put("studentPhone", s == null ? null : s.getPhone());
        m.put("licenseType", s == null ? null : s.getLicenseType());
        m.put("scheduleId", b.getScheduleId());
        if (sch != null) {
            m.put("scheduleDate", sch.getScheduleDate());
            m.put("startTime", sch.getStartTime());
            m.put("endTime", sch.getEndTime());
            Vehicle v = sch.getVehicleId() == null ? null : vehicleMapper.findById(sch.getVehicleId());
            m.put("vehiclePlate", v == null ? null : v.getPlateNo());
        }
        m.put("status", b.getStatus());
        m.put("createTime", b.getCreateTime());
        return m;
    }

    private Map<String, Object> hoursToMap(HoursRecord r) {
        Student s = studentMapper.findById(r.getStudentId());
        User u = s == null ? null : userMapper.findById(s.getUserId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("studentId", r.getStudentId());
        m.put("studentName", u == null ? "" : u.getName());
        m.put("subjectCode", r.getSubjectCode());
        m.put("subjectName", Enums.SUBJECTS.getOrDefault(r.getSubjectCode(), "科目" + r.getSubjectCode()));
        m.put("hours", r.getHours());
        m.put("trainDate", r.getTrainDate());
        m.put("auditStatus", r.getAuditStatus());
        m.put("auditOpinion", r.getAuditOpinion());
        m.put("auditTime", r.getAuditTime());
        return m;
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }
}
