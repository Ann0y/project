package sch.whvcse.entity;

import lombok.Data;

/** 6.3.4 vehicle 教练车辆 */
@Data
public class Vehicle {

    private Long id;
    private String plateNo;
    private String model;
    private Long coachId;
    /** 可用 / 维修 / 停用 */
    private String status;
    private Integer delFlag;
}
