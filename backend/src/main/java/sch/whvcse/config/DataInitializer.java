package sch.whvcse.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sch.whvcse.entity.Booking;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.CoachSchedule;
import sch.whvcse.entity.ExamPlan;
import sch.whvcse.entity.ExamRecord;
import sch.whvcse.entity.HoursRecord;
import sch.whvcse.entity.Payment;
import sch.whvcse.entity.Student;
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
import sch.whvcse.mapper.UserMapper;
import sch.whvcse.mapper.VehicleMapper;
import sch.whvcse.util.IdCardCrypto;

/**
 * 启动时初始化演示数据（三张角色账户 + 最小业务数据）。
 *
 * 三个角色账户（密码统一 123456，需求 2）：
 *   admin   / 123456  管理员
 *   coach   / 123456  教练（王建国，工号 C001）
 *   student / 123456  学员（李小明）
 *
 * 只有在 sys_user 表为空时才写入，重复启动不会重置数据。
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CoachMapper coachMapper;
    private final StudentMapper studentMapper;
    private final VehicleMapper vehicleMapper;
    private final CoachScheduleMapper scheduleMapper;
    private final BookingMapper bookingMapper;
    private final HoursRecordMapper hoursMapper;
    private final ExamPlanMapper examPlanMapper;
    private final ExamRecordMapper examRecordMapper;
    private final PaymentMapper paymentMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdCardCrypto idCardCrypto;

    public DataInitializer(UserMapper userMapper,
                           CoachMapper coachMapper,
                           StudentMapper studentMapper,
                           VehicleMapper vehicleMapper,
                           CoachScheduleMapper scheduleMapper,
                           BookingMapper bookingMapper,
                           HoursRecordMapper hoursMapper,
                           ExamPlanMapper examPlanMapper,
                           ExamRecordMapper examRecordMapper,
                           PaymentMapper paymentMapper,
                           PasswordEncoder passwordEncoder,
                           IdCardCrypto idCardCrypto) {
        this.userMapper = userMapper;
        this.coachMapper = coachMapper;
        this.studentMapper = studentMapper;
        this.vehicleMapper = vehicleMapper;
        this.scheduleMapper = scheduleMapper;
        this.bookingMapper = bookingMapper;
        this.hoursMapper = hoursMapper;
        this.examPlanMapper = examPlanMapper;
        this.examRecordMapper = examRecordMapper;
        this.paymentMapper = paymentMapper;
        this.passwordEncoder = passwordEncoder;
        this.idCardCrypto = idCardCrypto;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.count() > 0) {
            System.out.println(">>> sys_user 已有数据，跳过演示数据初始化");
            return;
        }

        LocalDate today = LocalDate.now();

        /* ==================== 1. 三个角色账户 ==================== */
        User admin = user("admin", "系统管理员", "admin");
        User coachUser = user("coach", "王建国", "coach");
        User studentUser = user("student", "李小明", "student");
        userMapper.insert(admin);
        userMapper.insert(coachUser);
        userMapper.insert(studentUser);

        /* ==================== 2. 教练档案 + 名下车辆 ==================== */
        // 主教练（coach 账号绑定）名下 3 辆车，用于排课关联车辆
        Coach coach = coach(coachUser.getId(), "C001", "13800001001", "C1,C2", "在职");
        coachMapper.insert(coach);
        List<Vehicle> coachVehicles = new ArrayList<>();
        coachVehicles.add(vehicle("沪A·1234学", "大众桑塔纳", coach.getId(), "可用"));
        coachVehicles.add(vehicle("沪A·1235学", "大众捷达", coach.getId(), "可用"));
        coachVehicles.add(vehicle("沪A·1236学", "日产轩逸", coach.getId(), "维修"));

        // 其他教练：同样需要登录账号（coach.user_id 外键非空）
        User coachUser2 = user("coach02", "李秀兰", "coach");
        userMapper.insert(coachUser2);
        Coach coach2 = coach(coachUser2.getId(), "C002", "13800001002", "C1", "在职");
        coachMapper.insert(coach2);
        coachVehicles.add(vehicle("沪A·2345学", "日产轩逸", coach2.getId(), "可用"));

        User coachUser3 = user("coach03", "张海涛", "coach");
        userMapper.insert(coachUser3);
        Coach coach3 = coach(coachUser3.getId(), "C003", "13800001003", "C1,B2", "在职");
        coachMapper.insert(coach3);
        coachVehicles.add(vehicle("沪A·3456学", "东风货车", coach3.getId(), "可用"));

        /* ==================== 3. 学员档案（含注册账号） ==================== */
        // 主学员：李小明（student 账号），已训练较深入
        Student mainStudent = student(studentUser.getId(), "310101199001011234",
                "13900002001", "C1", today.minusDays(60), "在训", coach.getId(), coachVehicles.get(0).getId());
        studentMapper.insert(mainStudent);

        // 其余学员，覆盖 在训 / 待审核 / 结业 / 退学 各种状态
        Object[][] studentSeed = {
            // 用户名, 姓名, 身份证, 手机, 车型, 报名距今天数, 状态, 教练索引
            {"st_wangfang", "王芳",   "310101199202022345", "13900002002", "C1", 58, "在训", 0},
            {"st_zhangwei", "张伟",   "310101199303033456", "13900002003", "C2", 52, "在训", 0},
            {"st_liuyang",  "刘洋",   "310101199404044567", "13900002004", "C1", 45, "在训", 0},
            {"st_chenjing", "陈静",   "310101199505055678", "13900002005", "C1", 120, "结业", 0},
            {"st_sunhao",   "孙浩",   "310101199606066789", "13900002006", "B2", 3,  "待审核", 0},
            {"st_zhoumin",  "周敏",   "310101199707077890", "13900002007", "C1", 40, "在训", 1},
            {"st_wuqiang",  "吴强",   "310101199808088901", "13900002008", "C2", 35, "在训", 1},
            {"st_zhengli",  "郑丽",   "310101199909099012", "13900002009", "C1", 30, "在训", 2},
            {"st_fengtao",  "冯涛",   "310101199010101123", "13900002010", "B2", 90, "退学", 2},
            {"st_xuna",     "许娜",   "310101199111111234", "13900002011", "C1", 25, "在训", 0},
            {"st_hejun",    "何军",   "310101199212122345", "13900002012", "A2", 70, "在训", 2},
            {"st_lisiyu",   "李思雨", "310101199601013456", "13900002013", "C1", 18, "在训", 0},
            {"st_zhaolei",  "赵磊",   "310101199702024567", "13900002014", "C1", 12, "在训", 1},
            {"st_qianyu",   "钱雨",   "310101199803035678", "13900002015", "C2", 8,  "在训", 2},
            {"st_sunli",    "孙丽",   "310101199904046789", "13900002016", "C1", 2,  "待审核", 0},
        };

        Coach[] coachList = {coach, coach2, coach3};
        List<Student> allStudents = new ArrayList<>();
        allStudents.add(mainStudent);

        for (Object[] row : studentSeed) {
            User u = user((String) row[0], (String) row[1], "student");
            userMapper.insert(u);
            Coach assigned = coachList[(Integer) row[7]];
            Vehicle assignedVehicle = coachVehicles.stream()
                    .filter(v -> v.getCoachId().equals(assigned.getId()))
                    .findFirst().orElse(null);
            Student st = student(u.getId(), (String) row[2], (String) row[3], (String) row[4],
                    today.minusDays((Integer) row[5]), (String) row[6],
                    assigned.getId(), assignedVehicle == null ? null : assignedVehicle.getId());
            studentMapper.insert(st);
            allStudents.add(st);
        }

        /* ==================== 4. 缴费记录 ==================== */
        for (Student st : allStudents) {
            if ("待审核".equals(st.getStatus())) {
                // 待审核学员：缴费未缴，演示缴费登记
                paymentMapper.insert(payment(st.getId(), "培训费", "5800.00", null, null, "未缴", admin.getId()));
            } else if ("退学".equals(st.getStatus())) {
                Payment p = payment(st.getId(), "培训费", "8800.00",
                        LocalDateTime.now().minusDays(90), "银行", "退费", admin.getId());
                paymentMapper.insert(p);
            } else {
                String[] methods = {"微信", "支付宝", "银行", "现金"};
                Payment p = payment(st.getId(), "培训费", "5800.00",
                        LocalDateTime.now().minusDays(30 + st.getId() % 20),
                        methods[(int) (st.getId() % methods.length)], "已缴", admin.getId());
                paymentMapper.insert(p);
            }
        }

        /* ==================== 5. 排课时段（未来 14 天，密集） ==================== */
        // 每个工作日 4 个时段，主教练全部排，另两位教练部分排
        String[][] timeSlots = {
            {"08:00", "10:00"},
            {"10:00", "12:00"},
            {"13:30", "15:30"},
            {"15:30", "17:30"},
        };

        List<CoachSchedule> coachSchedules = new ArrayList<>();
        for (int day = 0; day <= 14; day++) {
            LocalDate date = today.plusDays(day);
            // 跳过周日，模拟真实排班
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

            for (int ci = 0; ci < coachList.length; ci++) {
                Coach c = coachList[ci];
                // 主教练全排，其他教练按规则减少
                boolean full = ci == 0;
                for (int ti = 0; ti < timeSlots.length; ti++) {
                    if (!full && (day + ti + ci) % 3 == 0) continue;

                    List<Vehicle> own = coachVehicles.stream()
                            .filter(v -> v.getCoachId().equals(c.getId()) && "可用".equals(v.getStatus()))
                            .toList();
                    Vehicle v = own.isEmpty() ? null : own.get(ti % own.size());

                    CoachSchedule s = slot(c, v, date, timeSlots[ti][0], timeSlots[ti][1]);
                    scheduleMapper.insert(s);
                    coachSchedules.add(s);
                }
            }
        }

        /* ==================== 6. 预约记录（覆盖各种状态） ==================== */
        // 主教练名下已排课时段，用于生成预约
        List<CoachSchedule> mainSlots = coachSchedules.stream()
                .filter(s -> s.getCoachId().equals(coach.getId()))
                .toList();

        // 业务规则：一名学员最多同时持有 2 条未完成预约（待确认 / 已确认），由 book() 校验。
        // 演示约定：
        //   · student 账号（李小明）不播任何预约，默认「还没有预约」，方便现场演示约车；
        //   · 其他在训学员播 0~2 条未完成预约，供教练端「待确认预约 / 预约处理」演示；
        //   · 另播 1~2 条历史记录（已完成 / 已取消 / 爽约），只用于列表展示，不占用时段容量。
        String[] historyStatuses = {"已完成", "已取消", "爽约"};
        int slotIdx = 0;
        int studentIdx = 0;
        for (Student st : allStudents) {
            if (!"在训".equals(st.getStatus())) continue;

            boolean isDemoStudent = st.getId().equals(mainStudent.getId());

            // 1) 未完成预约：0 / 1 / 2 条，覆盖「最多 2 条」的规则
            int activeCount = 0;
            if (!isDemoStudent) {
                int rem = studentIdx % 3;
                activeCount = rem == 0 ? 2 : rem == 1 ? 1 : 0;
            }
            for (int k = 0; k < activeCount && slotIdx < mainSlots.size(); k++) {
                CoachSchedule s = mainSlots.get(slotIdx % mainSlots.size());
                slotIdx += 2;
                if (scheduleMapper.findById(s.getId()).getBookedCount() > 0) continue;
                Booking b = new Booking();
                b.setStudentId(st.getId());
                b.setScheduleId(s.getId());
                b.setStatus(k == 0 ? "待确认" : "已确认");
                bookingMapper.insert(b);
                scheduleMapper.updateBooked(s.getId(), 1, "已满");
            }

            // 2) 历史记录（不占用容量，不显示为「已预约」）
            if (!isDemoStudent) {
                int historyCount = studentIdx % 2 == 0 ? 2 : 1;
                for (int k = 0; k < historyCount && slotIdx < mainSlots.size(); k++) {
                    CoachSchedule s = mainSlots.get(slotIdx % mainSlots.size());
                    slotIdx += 2;
                    try {
                        Booking b = new Booking();
                        b.setStudentId(st.getId());
                        b.setScheduleId(s.getId());
                        b.setStatus(historyStatuses[(studentIdx + k) % historyStatuses.length]);
                        bookingMapper.insert(b);
                    } catch (Exception ignore) {
                        // 唯一约束（同一学员同一时段）冲突时跳过
                    }
                }
            }
            studentIdx++;
        }

        /* ==================== 7. 学时记录（各科目覆盖不同进度） ==================== */
        // 主学员：科目一已满、科目二接近满、科目三已满、科目四未开始
        // （科目三学到 24 学时是刻意留的：学时达标但尚未报名科三，
        //   便于演示「按科目顺序报名」以及成功后科目四才会放行）
        insertHours(studentMapper.findById(mainStudent.getId()), coach.getId(), "1",
                "12.0", today.minusDays(50));
        insertHours(studentMapper.findById(mainStudent.getId()), coach.getId(), "2",
                "20.0", today.minusDays(20));
        insertHours(studentMapper.findById(mainStudent.getId()), coach.getId(), "3",
                "24.0", today.minusDays(10));
        // 待审核记录，用于演示管理员审核
        hoursMapper.insert(hours(mainStudent.getId(), coach.getId(), "3",
                new BigDecimal("2.0"), today.minusDays(1), "待审核"));

        // 其他学员按报名时长给出不同进度
        for (Student st : allStudents) {
            if (st.getId().equals(mainStudent.getId())) continue;
            if (!"在训".equals(st.getStatus()) && !"结业".equals(st.getStatus())) continue;

            Coach assignedCoach = coachMapper.findById(st.getCoachId());
            if (assignedCoach == null) continue;

            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    st.getEnrollDate() == null ? today : st.getEnrollDate(), today);

            // 进度系数：报名越久学得越多
            double ratio = Math.min(1.0, days / 100.0);
            String[][] subjectPlan = {
                {"1", "12.0"},
                {"2", "26.0"},
                {"3", "24.0"},
                {"4", "10.0"},
            };
            for (int i = 0; i < subjectPlan.length; i++) {
                double full = Double.parseDouble(subjectPlan[i][1]);
                // 结业学员全部学满
                double done = "结业".equals(st.getStatus()) ? full : full * ratio * Math.min(1.0, (i + 1) * 0.9);
                done = Math.round(done * 2) / 2.0; // 保留 0.5 学时
                if (done <= 0) continue;
                insertHours(st, assignedCoach.getId(), subjectPlan[i][0],
                        String.valueOf(done),
                        today.minusDays((long) (days * (1 - i * 0.2))));
            }

            // 让 1~2 条进入待审核，模拟教练刚登记
            if (st.getId() % 4 == 0) {
                hoursMapper.insert(hours(st.getId(), assignedCoach.getId(), "2",
                        new BigDecimal("2.0"), today.minusDays(1), "待审核"));
            }
        }

        /* ==================== 8. 考试计划 + 成绩 ==================== */
        ExamPlan plan1 = plan("1", today.minusDays(40), "09:00", "第一理论考场", 60);
        ExamPlan plan2 = plan("2", today.minusDays(15), "08:30", "场地驾驶考场 A", 30);
        ExamPlan plan3 = plan("1", today.plusDays(6), "09:00", "第一理论考场", 60);
        ExamPlan plan4 = plan("2", today.plusDays(9), "08:30", "场地驾驶考场 A", 30);
        ExamPlan plan5 = plan("3", today.plusDays(14), "08:00", "道路驾驶考场", 40);
        ExamPlan plan6 = plan("4", today.plusDays(20), "10:00", "第二理论考场", 50);
        for (ExamPlan p : List.of(plan1, plan2, plan3, plan4, plan5, plan6)) {
            examPlanMapper.insert(p);
        }

        // 给不同学员造历史成绩，覆盖 合格 / 不合格 / 补考中 / 待考
        Object[][] scoreSeed = {
            // 学员索引, 计划, 第几次, 分数, 状态
            {0, 0, 1, 96.0, "合格"},
            {0, 1, 1, 92.0, "合格"},
            {1, 0, 1, 88.0, "合格"},
            {1, 1, 1, 71.0, "补考中"},
            {1, 3, 2, null, "待考"},
            {2, 0, 1, 95.0, "合格"},
            {2, 1, 1, 91.0, "合格"},
            {3, 0, 1, 82.0, "合格"},
            {4, 0, 1, 94.0, "合格"},
            {4, 1, 1, 90.0, "合格"},
            {4, 4, 1, 88.0, "合格"},
            {4, 5, 1, 93.0, "合格"},
            {10, 0, 1, 97.0, "合格"},
            {10, 1, 1, 85.0, "合格"},
        };

        ExamPlan[] plans = {plan1, plan2, plan3, plan4, plan5, plan6};
        for (Object[] row : scoreSeed) {
            int si = (Integer) row[0];
            if (si >= allStudents.size()) continue;
            Student st = allStudents.get(si);
            if (!"在训".equals(st.getStatus()) && !"结业".equals(st.getStatus())) continue;

            ExamPlan planForRecord = plans[(Integer) row[1]];
            ExamRecord rec = new ExamRecord();
            rec.setPlanId(planForRecord.getId());
            rec.setStudentId(st.getId());
            rec.setAttemptNo((Integer) row[2]);
            rec.setScore(row[3] == null ? null : new BigDecimal(row[3].toString()));
            rec.setStatus((String) row[4]);
            rec.setExamDate(planForRecord.getExamDate());
            examRecordMapper.insert(rec);
            examPlanMapper.addEnrolled(planForRecord.getId(), 1);
        }

        /* ==================== 9. 未来考试报名（待考） ==================== */
        for (Student st : allStudents) {
            if (!"在训".equals(st.getStatus())) continue;
            if (st.getId() % 3 != 0) continue;

            ExamRecord rec = new ExamRecord();
            rec.setPlanId(plan3.getId());
            rec.setStudentId(st.getId());
            rec.setAttemptNo(1);
            rec.setStatus("待考");
            try {
                examRecordMapper.insert(rec);
                examPlanMapper.addEnrolled(plan3.getId(), 1);
            } catch (Exception ignore) {
                // 唯一约束冲突（已报过名）忽略
            }
        }

        System.out.println(">>> 演示数据初始化完成");
        System.out.println(">>> 账号：admin / coach / student，密码均为 123456");
        System.out.println(">>> 学员 " + studentMapper.findAll().size() + " 人，排课时段 "
                + scheduleMapper.findAll().size() + " 个，学时记录 "
                + hoursMapper.findAll().size() + " 条");
    }

    /* ==================== 私有构造辅助 ==================== */

    private Coach coach(Long userId, String code, String phone, String types, String status) {
        Coach c = new Coach();
        c.setUserId(userId);
        c.setCode(code);
        c.setPhone(phone);
        c.setLicenseTypes(types);
        c.setStatus(status);
        return c;
    }

    /** 创建车辆并立即入库（需要回填自增主键供后续引用） */
    private Vehicle vehicle(String plate, String model, Long coachId, String status) {
        Vehicle v = new Vehicle();
        v.setPlateNo(plate);
        v.setModel(model);
        v.setCoachId(coachId);
        v.setStatus(status);
        vehicleMapper.insert(v);
        return v;
    }

    private Student student(Long userId, String idCard, String phone, String licenseType,
                            LocalDate enrollDate, String status, Long coachId, Long vehicleId) {
        Student s = new Student();
        s.setUserId(userId);
        s.setIdCard(idCardCrypto.encrypt(idCard));
        s.setPhone(phone);
        s.setLicenseType(licenseType);
        s.setEnrollDate(enrollDate);
        s.setStatus(status);
        s.setCoachId(coachId);
        s.setVehicleId(vehicleId);
        return s;
    }

    private Payment payment(Long studentId, String item, String amount, LocalDateTime payTime,
                            String method, String status, Long operatorId) {
        Payment p = new Payment();
        p.setStudentId(studentId);
        p.setItem(item);
        p.setAmount(new BigDecimal(amount));
        p.setPayTime(payTime);
        p.setPayMethod(method);
        p.setStatus(status);
        p.setOperatorId(operatorId);
        return p;
    }

    /** 按科目写入若干条已通过学时记录（每条 2 学时，自动拆分） */
    private void insertHours(Student st, Long coachId, String subject, String totalHours, LocalDate start) {
        double total = Double.parseDouble(totalHours);
        double remain = total;
        int i = 0;
        while (remain > 0) {
            double h = Math.min(2.0, remain);
            hoursMapper.insert(hours(st.getId(), coachId, subject,
                    BigDecimal.valueOf(h), start.plusDays((long) i * 3), "已通过"));
            remain -= h;
            i++;
        }
    }

    private User user(String username, String name, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setName(name);
        u.setRole(role);
        u.setStatus(1);
        return u;
    }

    private CoachSchedule slot(Coach coach, Vehicle vehicle, LocalDate date, String start, String end) {
        CoachSchedule s = new CoachSchedule();
        s.setCoachId(coach.getId());
        s.setVehicleId(vehicle.getId());
        s.setScheduleDate(date);
        s.setStartTime(LocalTime.parse(start));
        s.setEndTime(LocalTime.parse(end));
        s.setCapacity(1);
        s.setBookedCount(0);
        s.setStatus("可约");
        return s;
    }

    private HoursRecord hours(Long studentId, Long coachId, String subject,
                              BigDecimal h, LocalDate date, String auditStatus) {
        HoursRecord r = new HoursRecord();
        r.setStudentId(studentId);
        r.setCoachId(coachId);
        r.setSubjectCode(subject);
        r.setHours(h);
        r.setTrainDate(date);
        r.setAuditStatus(auditStatus);
        return r;
    }

    private ExamPlan plan(String subject, LocalDate date, String time, String venue, int capacity) {
        ExamPlan p = new ExamPlan();
        p.setSubjectCode(subject);
        p.setExamDate(date);
        p.setExamTime(LocalTime.parse(time));
        p.setVenue(venue);
        p.setCapacity(capacity);
        p.setEnrolledCount(0);
        p.setStatus(date.isBefore(LocalDate.now()) ? "已完成" : "报名中");
        return p;
    }
}
