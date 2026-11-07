package sch.whvcse.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sch.whvcse.entity.Booking;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.CoachSchedule;
import sch.whvcse.entity.ExamPlan;
import sch.whvcse.entity.Vehicle;
import sch.whvcse.mapper.BookingMapper;
import sch.whvcse.mapper.CoachMapper;
import sch.whvcse.mapper.CoachScheduleMapper;
import sch.whvcse.mapper.ExamPlanMapper;
import sch.whvcse.mapper.VehicleMapper;

/**
 * 演示数据「日期保鲜」。
 *
 * 背景：排课时段 / 考试计划是相对导入日期生成的（CURDATE()+INTERVAL n DAY）。
 * 如果数据库是几天前导入的，所有时段都会变成过去时间，学员端就会出现
 * 「没有可预约时间段」；考试页也会把过期的「报名中」场次当成可报名。
 *
 * 本组件在启动时做两件幂等的事：
 *   1. 若某教练在未来 14 天内没有任何排课，则为其补齐未来排课；
 *   2. 把已过期的「报名中」考试计划置为「已截止」，并为每个科目补一场未来考试。
 *
 * 只在「数据已经过期」时补数据，已有未来数据时不会重复插入。
 * 可通过 app.demo.auto-refresh=false 关闭。
 */
@Component
@Order(2)
public class DemoDataRefresher implements CommandLineRunner {

    /** 与 StudentPortalServiceImpl.BOOKING_WINDOW_DAYS 保持一致 */
    private static final int BOOKING_WINDOW_DAYS = 5;

    private static final LocalTime[][] TIME_SLOTS = {
            {LocalTime.of(8, 0), LocalTime.of(10, 0)},
            {LocalTime.of(10, 0), LocalTime.of(12, 0)},
            {LocalTime.of(13, 30), LocalTime.of(15, 30)},
            {LocalTime.of(15, 30), LocalTime.of(17, 30)},
    };

    private static final String[][] EXAM_SEED = {
            {"1", "09:00", "第一理论考场", "60"},
            {"2", "08:30", "场地驾驶考场 A", "30"},
            {"3", "08:00", "道路驾驶考场", "40"},
            {"4", "10:00", "第二理论考场", "50"},
    };

    private final CoachMapper coachMapper;
    private final VehicleMapper vehicleMapper;
    private final CoachScheduleMapper scheduleMapper;
    private final ExamPlanMapper examPlanMapper;
    private final BookingMapper bookingMapper;

    @Value("${app.demo.auto-refresh:true}")
    private boolean autoRefresh;

    public DemoDataRefresher(CoachMapper coachMapper,
                             VehicleMapper vehicleMapper,
                             CoachScheduleMapper scheduleMapper,
                             ExamPlanMapper examPlanMapper,
                             BookingMapper bookingMapper) {
        this.coachMapper = coachMapper;
        this.vehicleMapper = vehicleMapper;
        this.scheduleMapper = scheduleMapper;
        this.examPlanMapper = examPlanMapper;
        this.bookingMapper = bookingMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!autoRefresh) return;

        LocalDate today = LocalDate.now();
        int removedWeekend = cleanupWeekendSchedules(today);
        int createdSchedules = refreshSchedules(today);
        int createdPlans = refreshExamPlans(today);

        if (removedWeekend > 0 || createdSchedules > 0 || createdPlans > 0) {
            System.out.println(">>> 演示数据已刷新：清理周末排课 " + removedWeekend
                    + " 个，新增排课 " + createdSchedules
                    + " 个，新增考试计划 " + createdPlans + " 场");
        }
    }

    /**
     * 清掉今天及以后仍然存在的周六 / 周日排课。
     *
     * 旧版本演示数据（以及旧的手工排课）可能已经生成了周末时段。
     * 若这些时段上还有未完成预约，先按「周末不排课」自动取消，再软删除时段，
     * 避免学员端出现无法到场的预约。历史周末排课保留不动。
     */
    private int cleanupWeekendSchedules(LocalDate today) {
        List<Booking> allBookings = bookingMapper.findAll();
        int removed = 0;
        for (CoachSchedule s : scheduleMapper.findAll()) {
            if (s.getScheduleDate().isBefore(today)) continue;
            DayOfWeek dow = s.getScheduleDate().getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) continue;

            for (Booking b : allBookings) {
                if (!s.getId().equals(b.getScheduleId())) continue;
                if ("待确认".equals(b.getStatus()) || "已确认".equals(b.getStatus())) {
                    bookingMapper.cancel(b.getId(), "周末不排课，系统自动取消");
                }
            }
            scheduleMapper.softDelete(s.getId());
            removed++;
        }
        return removed;
    }

    /** 某教练在可预约窗口（今天起 5 天）内没有排课时，补齐未来 14 天排课 */
    private int refreshSchedules(LocalDate today) {
        int created = 0;
        List<Coach> coaches = coachMapper.findActive();
        for (Coach coach : coaches) {
            List<CoachSchedule> window = scheduleMapper.search(
                    coach.getId(), null, today, today.plusDays(BOOKING_WINDOW_DAYS - 1L));
            if (!window.isEmpty()) continue;

            List<Vehicle> vehicles = new ArrayList<>(vehicleMapper.findByCoachId(coach.getId()));
            vehicles.removeIf(v -> !"可用".equals(v.getStatus()));

            for (int day = 0; day <= 14; day++) {
                LocalDate date = today.plusDays(day);
                // 周六、周日不排课，和 DataInitializer 保持一致
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

                for (int ti = 0; ti < TIME_SLOTS.length; ti++) {
                    LocalTime start = TIME_SLOTS[ti][0];
                    LocalTime end = TIME_SLOTS[ti][1];
                    if (scheduleMapper.countClash(coach.getId(), date, start, null) > 0) continue;

                    CoachSchedule s = new CoachSchedule();
                    s.setCoachId(coach.getId());
                    s.setVehicleId(vehicles.isEmpty()
                            ? null : vehicles.get(ti % vehicles.size()).getId());
                    s.setScheduleDate(date);
                    s.setStartTime(start);
                    s.setEndTime(end);
                    s.setCapacity(1);
                    s.setBookedCount(0);
                    s.setStatus("可约");
                    scheduleMapper.insert(s);
                    created++;
                }
            }
        }
        return created;
    }

    /** 过期考试计划置为已截止；每个科目没有未来「报名中」场次时补一场 */
    private int refreshExamPlans(LocalDate today) {
        int created = 0;
        List<ExamPlan> plans = examPlanMapper.findAll();

        for (ExamPlan p : plans) {
            if ("报名中".equals(p.getStatus()) && p.getExamDate().isBefore(today)) {
                examPlanMapper.updateStatus(p.getId(), "已截止");
            }
        }

        for (int i = 0; i < EXAM_SEED.length; i++) {
            String subject = EXAM_SEED[i][0];
            boolean hasOpen = plans.stream().anyMatch(p ->
                    subject.equals(p.getSubjectCode())
                            && "报名中".equals(p.getStatus())
                            && !p.getExamDate().isBefore(today));
            if (hasOpen) continue;

            ExamPlan plan = new ExamPlan();
            plan.setSubjectCode(subject);
            plan.setExamDate(today.plusDays(2L + i * 2L));
            plan.setExamTime(LocalTime.parse(EXAM_SEED[i][1]));
            plan.setVenue(EXAM_SEED[i][2]);
            plan.setCapacity(Integer.parseInt(EXAM_SEED[i][3]));
            plan.setEnrolledCount(0);
            plan.setStatus("报名中");
            examPlanMapper.insert(plan);
            created++;
        }
        return created;
    }
}
