package sch.whvcse.service;

import sch.whvcse.vo.LoginVO;
import sch.whvcse.vo.RegisterVO;
import sch.whvcse.vo.UserVO;

/**
 * 认证业务接口。
 */
public interface AuthService {

    /** 登录：校验账号密码，成功返回用户视图对象（不含密码） */
    UserVO login(LoginVO loginVO);

    /** 注册：新增学员账号，成功返回用户视图对象（不含密码） */
    UserVO register(RegisterVO registerVO);
}
