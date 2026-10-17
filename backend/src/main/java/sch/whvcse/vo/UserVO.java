package sch.whvcse.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户视图对象（UserVO）：返回给前端的用户数据。
 * 只保留前端需要的字段，绝不包含密码等敏感信息。
 *
 * 补充字段（studentId / coachId）让前端登录后直接知道自己的业务身份，
 * 避免再查一次接口。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String name;
    /** admin / coach / student */
    private String role;
    /** 学员档案 id（仅 role=student 时非空） */
    private Long studentId;
    /** 教练档案 id（仅 role=coach 时非空） */
    private Long coachId;
}
