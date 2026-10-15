package sch.whvcse.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户视图对象（UserVO）：返回给前端的用户数据。
 * 只保留前端需要的字段，绝不包含密码等敏感信息。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String name;
    /** 角色：admin / student */
    private String role;
}
