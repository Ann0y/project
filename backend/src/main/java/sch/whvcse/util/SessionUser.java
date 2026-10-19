package sch.whvcse.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import sch.whvcse.common.BizException;
import sch.whvcse.interceptor.LoginInterceptor;
import sch.whvcse.vo.UserVO;

/**
 * 从 Session 取当前登录用户，并提供"本人"身份解析。
 *
 * 需求文档第 7 章：三端共用的资源用 /my、/assigned 区分数据范围，
 * 服务端按登录用户角色与身份过滤 —— 这些过滤都从这里拿到身份后完成。
 */
@Component
public class SessionUser {

    /** 当前登录用户；未登录抛 401 */
    public UserVO current(HttpSession session) {
        Object attr = session == null ? null : session.getAttribute(LoginInterceptor.SESSION_USER);
        if (attr == null) throw new BizException(401, "未登录或登录已过期");
        return (UserVO) attr;
    }

    /** 当前登录用户 id */
    public Long userId(HttpSession session) {
        return current(session).getId();
    }

    /**
     * 当前登录学员的档案 id。
     * 若 Session 里的 VO 没带 studentId（老会话），直接报错提示重新登录。
     */
    public Long studentId(HttpSession session) {
        UserVO vo = current(session);
        if (!"student".equals(vo.getRole())) {
            throw new BizException(403, "该接口仅供学员使用");
        }
        if (vo.getStudentId() == null) throw new BizException(401, "登录信息已变更，请重新登录");
        return vo.getStudentId();
    }

    /** 当前登录教练的档案 id */
    public Long coachId(HttpSession session) {
        UserVO vo = current(session);
        if (!"coach".equals(vo.getRole())) {
            throw new BizException(403, "该接口仅供教练使用");
        }
        if (vo.getCoachId() == null) throw new BizException(401, "登录信息已变更，请重新登录");
        return vo.getCoachId();
    }

    /** 判断当前用户是否管理员 */
    public void requireAdmin(HttpSession session) {
        UserVO vo = current(session);
        if (!"admin".equals(vo.getRole())) {
            throw new BizException(403, "该操作仅管理员可执行");
        }
    }
}
