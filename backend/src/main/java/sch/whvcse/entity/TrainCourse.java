package sch.whvcse.entity;

import java.math.BigDecimal;
import lombok.Data;

/** 6.3.5 train_course 培训课程 / 科目学时配置 */
@Data
public class TrainCourse {

    private Long id;
    private String licenseType;
    /** 科目代码 1/2/3/4 */
    private String subjectCode;
    private String subjectName;
    /** 大纲要求学时 */
    private BigDecimal requiredHours;
    private Integer delFlag;
}
