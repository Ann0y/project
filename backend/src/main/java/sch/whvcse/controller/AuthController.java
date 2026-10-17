package sch.whvcse.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sch.whvcse.common.Enums;
import sch.whvcse.common.Result;
import sch.whvcse.interceptor.LoginInterceptor;
import sch.whvcse.service.AuthService;
import sch.whvcse.util.SessionUser;
import sch.whvcse.vo.ChangePasswordVO;
import sch.whvcse.vo.LoginVO;
import sch.whvcse.vo.RegisterVO;
import sch.whvcse.vo.UserVO;

/**
 * 认证接口（Session 方案，需求文档 7 章）。
 * POST /api/auth/register  注册（自助注册，角色固定为学员）
 * POST /api/auth/login     登录（成功后把 UserVO 放进 Session）
 * GET  /api/auth/me        获取当前登录用户
 * POST /api/auth/logout    登出（销毁 Session）
 * PUT  /api/auth/password  修改本人密码
 * GET  /api/enums          字典枚举
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final SessionUser sessionUser;

    public AuthController(AuthService authService, SessionUser sessionUser) {
        this.authService = authService;
        this.sessionUser = sessionUser;
    }

    @PostMapping("/auth/register")
    public Result<UserVO> register(@RequestBody @Valid RegisterVO registerVO) {
        return Result.ok(authService.register(registerVO));
    }

    @PostMapping("/auth/login")
    public Result<UserVO> login(@RequestBody @Valid LoginVO loginVO, HttpSession session) {
        UserVO user = authService.login(loginVO);
        // 需求 2：Session 承载登录态，滑动过期由容器负责续期
        session.setAttribute(LoginInterceptor.SESSION_USER, user);
        return Result.ok(user);
    }

    @GetMapping("/auth/me")
    public Result<UserVO> me(HttpSession session) {
        UserVO vo = (UserVO) session.getAttribute(LoginInterceptor.SESSION_USER);
        return Result.ok(vo);
    }

    @PostMapping("/auth/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.ok();
    }

    @PutMapping("/auth/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordVO vo, HttpSession session) {
        authService.changePassword(sessionUser.userId(session), vo);
        return Result.ok();
    }

    /** 需求 6.5：准驾车型 / 科目 / 各状态等枚举，前端经此接口读取 */
    @GetMapping("/enums")
    public Result<Object> enums() {
        return Result.ok(Enums.dictionary());
    }
}
