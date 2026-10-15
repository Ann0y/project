package sch.whvcse.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册参数（前端提交）。自助注册的账号角色固定为 student。
 */
@Data
public class RegisterVO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为 3-20 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少 6 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 手机号：目前仅接收，暂不落库（user 表暂无该字段） */
    private String phone;
}
