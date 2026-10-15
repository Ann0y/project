package sch.whvcse.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sch.whvcse.entity.User;
import sch.whvcse.mapper.UserMapper;

/**
 * 启动时初始化两个演示账号（表为空时才插入）：
 *   admin   / 123456  管理员
 *   student / 123456  学员
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userMapper.count() > 0) {
            return;
        }
        userMapper.insert(build("admin", "123456", "admin", "管理员"));
        userMapper.insert(build("student", "123456", "student", "张佛亮"));
        System.out.println(">>> 已初始化演示账号：admin/123456、student/123456");
    }

    private User build(String username, String rawPassword, String role, String name) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setName(name);
        return user;
    }
}
