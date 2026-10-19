package sch.whvcse.vo;

import lombok.Data;

/**
 * 考试记录 + 考试计划信息的联合视图。
 * exam_record 表本身不含 subject_code（科目由 exam_plan 提供），
 * 因此查询时 JOIN exam_plan 一次取全，避免业务层 N+1 查询。
 */
@Data
public class ExamRecordView {

    /* ---- exam_record ---- */
    private Long id;
    private Long planId;
    private Long studentId;
    private java.time.LocalDateTime registerTime;
    private Integer attemptNo;
    private java.math.BigDecimal score;
    private String status;
    private java.time.LocalDate examDate;

    /* ---- exam_plan ---- */
    private String subjectCode;
    private java.time.LocalDate planDate;
    private java.time.LocalTime examTime;
    private String venue;

    /* ---- 学员（管理端列表用） ---- */
    private String studentName;
    private String licenseType;
}
