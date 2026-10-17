package sch.whvcse.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册参数（前端提交）。自助注册的账号角色固定为 student，
 * 同时创建一份「待审核」的学员档案（需求 3.1 报名流程起点）。
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

    /** 联系电话 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 身份证号（明文提交，后端 AES 加密落库） */
    private String idCard;

    /** 意向准驾车型，默认 C1 */
    private String licenseType;
}
