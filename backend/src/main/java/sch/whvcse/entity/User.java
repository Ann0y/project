package sch.whvcse.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 6.3.1 sys_user 登录用户 */
@Data
public class User {

    private Long id;
    private String username;
    /** BCrypt 密文 */
    private String password;
    /** 显示姓名 */
    private String name;
    /** admin / coach / student */
    private String role;
    /** 1 启用 / 0 停用 */
    private Integer status;
    private LocalDateTime createTime;
    /** 逻辑删除 0 正常 / 1 删除 */
    private Integer delFlag;
}
