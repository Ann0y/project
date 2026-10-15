package sch.whvcse.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录参数（前端提交）。
 */
@Data
public class LoginVO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
