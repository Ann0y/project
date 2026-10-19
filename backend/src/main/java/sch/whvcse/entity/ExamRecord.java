package sch.whvcse.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 6.3.10 exam_record 考试报名与成绩 */
@Data
public class ExamRecord {

    private Long id;
    private Long planId;
    private Long studentId;
    private LocalDateTime registerTime;
    /** 第几次（补考递增） */
    private Integer attemptNo;
    private BigDecimal score;
    /** 待考 / 合格 / 不合格 / 补考中 */
    private String status;
    private LocalDate examDate;
}
