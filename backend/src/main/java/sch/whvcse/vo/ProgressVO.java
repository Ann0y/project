package sch.whvcse.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * F02 我的培训进度（学员端）返回结构。
 * 一次返回前端首页所需的全部数据，避免多次请求。
 */
@Data
public class ProgressVO {

    /* ---------- 基本信息 ---------- */
    private Long studentId;
    private String name;
    private String licenseType;
    /** 待审核 / 在训 / 结业 / 退学 */
    private String status;
    private java.time.LocalDate enrollDate;
    private String phone;
    /** 身份证号（脱敏展示） */
    private String idCardMasked;
    /** 缴费状态：已缴 / 未缴 / 退费 */
    private String paymentStatus;

    /* ---------- 固定带教关系 ---------- */
    private Long coachId;
    private String coachName;
    private String coachPhone;
    private Long vehicleId;
    private String vehiclePlate;
    private String vehicleModel;

    /* ---------- 学时汇总 ---------- */
    /** 已学学时（仅统计审核通过） */
    private BigDecimal doneHours;
    /** 大纲要求总学时 */
    private BigDecimal requiredHours;
    /** 剩余学时 */
    private BigDecimal remainHours;
    /** 整体完成率 0-100 */
    private int percent;

    /** 各科目进度 */
    private List<SubjectProgress> subjects;

    /** 是否满足结业条件（系统只做判断与提示，最终由管理员手动确认） */
    private boolean graduationReady;
    private boolean hoursOk;
    private boolean examsOk;

    /** 最近培训 / 考试记录时间轴 */
    private List<TimelineItem> timeline;

    /** 各科目已通过学时 map，便于前端画图 */
    private Map<String, BigDecimal> doneHoursBySubject;

    /** 科目进度明细 */
    @Data
    public static class SubjectProgress {
        private String code;
        private String name;
        private BigDecimal done;
        private BigDecimal required;
        private int percent;
    }

    /** 时间轴条目 */
    @Data
    public static class TimelineItem {
        private String date;
        /** 培训 / 考试 / 报名 */
        private String type;
        private String text;
    }
}
