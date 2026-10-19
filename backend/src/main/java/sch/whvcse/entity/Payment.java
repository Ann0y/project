package sch.whvcse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 6.3.11 payment 缴费记录 */
@Data
public class Payment {

    private Long id;
    private Long studentId;
    /** 培训费 / 补考费 / 其它 */
    private String item;
    private BigDecimal amount;
    private LocalDateTime payTime;
    /** 现金 / 微信 / 支付宝 / 银行 */
    private String payMethod;
    /** 未缴 / 已缴 / 退费 */
    private String status;
    private Long operatorId;
}
