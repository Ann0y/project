package sch.whvcse.service;

import sch.whvcse.vo.ChangePasswordVO;
import sch.whvcse.vo.LoginVO;
import sch.whvcse.vo.RegisterVO;
import sch.whvcse.vo.UserVO;

/** 认证业务接口 */
public interface AuthService {

    /** 登录：校验账号密码，成功返回用户视图对象（不含密码） */
    UserVO login(LoginVO loginVO);

    /** 注册：新增学员账号 + 待审核学员档案 */
    UserVO register(RegisterVO registerVO);

    /** 修改本人密码（校验原密码） */
    void changePassword(Long userId, ChangePasswordVO vo);

    /** 把 User 转成带业务身份（studentId / coachId）的 VO */
    UserVO toVO(Long userId);
}
