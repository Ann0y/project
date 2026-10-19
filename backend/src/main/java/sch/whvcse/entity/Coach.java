package sch.whvcse.entity;

import lombok.Data;

/** 6.3.3 coach 教练档案 */
@Data
public class Coach {

    private Long id;
    private Long userId;
    /** 教练工号 */
    private String code;
    private String phone;
    /** 准教车型，逗号分隔（如 C1,C2） */
    private String licenseTypes;
    /** 在职 / 停用 */
    private String status;
    private Integer delFlag;
}
