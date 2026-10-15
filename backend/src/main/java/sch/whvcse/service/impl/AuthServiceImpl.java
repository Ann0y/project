package sch.whvcse.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sch.whvcse.common.BizException;
import sch.whvcse.entity.User;
import sch.whvcse.mapper.UserMapper;
import sch.whvcse.service.AuthService;
import sch.whvcse.vo.LoginVO;
import sch.whvcse.vo.UserVO;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserVO login(LoginVO loginVO) {
        User user = userMapper.findByUsername(loginVO.getUsername());
        // 用户不存在也返回同样的提示，避免暴露账号是否存在
        if (user == null || !passwordEncoder.matches(loginVO.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        // Entity -> VO：只把该给前端看的字段带出去，密码自然不出现在响应里
        return toVO(user);
    }

    /** 实体转视图对象 */
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        return vo;
    }
}
