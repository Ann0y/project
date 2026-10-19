package sch.whvcse.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/** 6.3.6 coach_schedule 教练排课时段 */
@Data
public class CoachSchedule {

    private Long id;
    private Long coachId;
    private Long vehicleId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    /** 容量（可约人数） */
    private Integer capacity;
    /** 已约人数（冗余缓存） */
    private Integer bookedCount;
    /** 可约 / 已满 / 停用 */
    private String status;
    private Integer delFlag;
}
