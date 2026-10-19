package sch.whvcse.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 6.3.8 hours_record 学时记录 */
@Data
public class HoursRecord {

    private Long id;
    private Long studentId;
    private Long coachId;
    /** 关联预约（可为空，如线下补录） */
    private Long bookingId;
    private String subjectCode;
    private BigDecimal hours;
    private LocalDate trainDate;
    /** 待审核 / 已通过 / 已驳回 */
    private String auditStatus;
    private Long auditorId;
    private String auditOpinion;
    private LocalDateTime auditTime;
}
