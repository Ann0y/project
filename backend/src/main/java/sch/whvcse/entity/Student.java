package sch.whvcse.entity;

import java.time.LocalDate;
import lombok.Data;

/** 6.3.2 student 学员档案 */
@Data
public class Student {

    private Long id;
    private Long userId;
    /** 身份证号（AES 密文） */
    private String idCard;
    private String phone;
    /** 准驾车型 */
    private String licenseType;
    private LocalDate enrollDate;
    /** 待审核 / 在训 / 结业 / 退学 */
    private String status;
    /** 固定带教教练 */
    private Long coachId;
    /** 固定带教车辆 */
    private Long vehicleId;
    private Integer delFlag;
}
