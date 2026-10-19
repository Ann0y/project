package sch.whvcse.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改本人密码（需求 F01 个人中心） */
@Data
public class ChangePasswordVO {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, message = "新密码至少 6 位")
    private String newPassword;
}
