package sch.whvcse.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/** 6.3.9 exam_plan 考试计划 */
@Data
public class ExamPlan {

    private Long id;
    private String subjectCode;
    private LocalDate examDate;
    private LocalTime examTime;
    private String venue;
    private Integer capacity;
    /** 已报名人数（冗余缓存） */
    private Integer enrolledCount;
    /** 报名中 / 已截止 / 已完成 */
    private String status;
    private Integer delFlag;
}
