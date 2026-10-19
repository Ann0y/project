package sch.whvcse.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 6.3.7 booking 培训预约 */
@Data
public class Booking {

    private Long id;
    private Long studentId;
    private Long scheduleId;
    /** 待确认 / 已确认 / 已完成 / 已取消 / 爽约 */
    private String status;
    private LocalDateTime createTime;
}
