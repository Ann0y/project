package sch.whvcse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sch.whvcse.interceptor.LoginInterceptor;

/**
 * Web 配置：跨域 + 登录拦截器。
 * 注意：Session 方案下前端必须带 Cookie（axios 的 withCredentials = true）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /** 允许前端（如 http://localhost:5173）跨域访问，并携带 Cookie */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 带 Cookie 时不能用 "*"，用 originPatterns
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)   // 关键：允许携带 JSESSIONID
                .maxAge(3600);
    }

    /** 除登录/注册/登出外，其余 /api/** 都要先登录 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/auth/logout");
    }
}
