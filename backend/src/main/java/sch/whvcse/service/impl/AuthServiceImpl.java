package sch.whvcse.service.impl;

import java.time.LocalDate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sch.whvcse.common.BizException;
import sch.whvcse.entity.Coach;
import sch.whvcse.entity.Student;
import sch.whvcse.entity.User;
import sch.whvcse.mapper.CoachMapper;
import sch.whvcse.mapper.StudentMapper;
import sch.whvcse.mapper.UserMapper;
import sch.whvcse.service.AuthService;
import sch.whvcse.util.IdCardCrypto;
import sch.whvcse.vo.ChangePasswordVO;
import sch.whvcse.vo.LoginVO;
import sch.whvcse.vo.RegisterVO;
import sch.whvcse.vo.UserVO;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final CoachMapper coachMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdCardCrypto idCardCrypto;

    public AuthServiceImpl(UserMapper userMapper,
                           StudentMapper studentMapper,
                           CoachMapper coachMapper,
                           PasswordEncoder passwordEncoder,
                           IdCardCrypto idCardCrypto) {
        this.userMapper = userMapper;
        this.studentMapper = studentMapper;
        this.coachMapper = coachMapper;
        this.passwordEncoder = passwordEncoder;
        this.idCardCrypto = idCardCrypto;
    }

    @Override
    public UserVO login(LoginVO loginVO) {
        User user = userMapper.findByUsername(loginVO.getUsername());
        // 用户不存在也返回同样的提示，避免暴露账号是否存在
        if (user == null || !passwordEncoder.matches(loginVO.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "该账号已停用，请联系管理员");
        }
        return toVO(user.getId());
    }

    @Override
    @Transactional
    public UserVO register(RegisterVO registerVO) {
        if (userMapper.findByUsername(registerVO.getUsername()) != null) {
            throw new BizException(400, "该用户名已被注册");
        }

        // 1) 建登录账号（自助注册默认为学员）
        User user = new User();
        user.setUsername(registerVO.getUsername());
        user.setPassword(passwordEncoder.encode(registerVO.getPassword()));
        user.setName(registerVO.getName());
        user.setRole("student");
        user.setStatus(1);
        userMapper.insert(user);

        // 2) 同步建学员档案：状态「待审核」，等待管理员审核报名资料（需求 3.1）
        Student student = new Student();
        student.setUserId(user.getId());
        // 身份证号 AES 加密落库；未填时用占位符保证唯一约束不冲突
        String idCard = registerVO.getIdCard();
        student.setIdCard(idCardCrypto.encrypt(
                (idCard == null || idCard.isBlank()) ? "PENDING-" + user.getId() : idCard));
        student.setPhone(registerVO.getPhone());
        student.setLicenseType(
                (registerVO.getLicenseType() == null || registerVO.getLicenseType().isBlank())
                        ? "C1" : registerVO.getLicenseType());
        student.setEnrollDate(LocalDate.now());
        student.setStatus("待审核");
        studentMapper.insert(student);

        return toVO(user.getId());
    }

    @Override
    public void changePassword(Long userId, ChangePasswordVO vo) {
        User user = userMapper.findById(userId);
        if (user == null) throw new BizException(404, "用户不存在");
        if (!passwordEncoder.matches(vo.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "原密码不正确");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(vo.getNewPassword()));
    }

    @Override
    public UserVO toVO(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) return null;

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());

        // 绑定业务身份：学员带 studentId，教练带 coachId
        if ("student".equals(user.getRole())) {
            Student s = studentMapper.findByUserId(user.getId());
            if (s != null) vo.setStudentId(s.getId());
        } else if ("coach".equals(user.getRole())) {
            Coach c = coachMapper.findByUserId(user.getId());
            if (c != null) vo.setCoachId(c.getId());
        }
        return vo;
    }
}
