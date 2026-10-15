package sch.whvcse.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sch.whvcse.common.Result;
import sch.whvcse.interceptor.LoginInterceptor;
import sch.whvcse.service.AuthService;
import sch.whvcse.vo.LoginVO;
import sch.whvcse.vo.UserVO;

/**
 * 认证接口（Session 方案）。
 * POST /api/auth/login   登录（成功后把 UserVO 放进 Session）
 * GET  /api/auth/me      获取当前登录用户
 * POST /api/auth/logout  登出（销毁 Session）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody @Valid LoginVO loginVO, HttpSession session) {
        UserVO user = authService.login(loginVO);
        session.setAttribute(LoginInterceptor.SESSION_USER, user);
        return Result.ok(user);
    }

    @GetMapping("/me")
    public Result<UserVO> me(HttpSession session) {
        return Result.ok((UserVO) session.getAttribute(LoginInterceptor.SESSION_USER));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.ok();
    }
}
