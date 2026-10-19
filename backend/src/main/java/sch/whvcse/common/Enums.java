package sch.whvcse.common;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举常量（需求文档 6.5「枚举值约定」）。
 * 保持与数据库存储值完全一致，避免前后端出现"码值对不上"。
 */
public final class Enums {

    private Enums() {}

    /** 准驾车型 */
    public static final List<String> LICENSE_TYPES = List.of("C1", "C2", "B2", "A2");

    /** 科目 */
    public static final Map<String, String> SUBJECTS = Map.of(
            "1", "科目一 · 道路交通安全法律法规",
            "2", "科目二 · 场地驾驶技能",
            "3", "科目三 · 道路驾驶技能",
            "4", "科目四 · 安全文明驾驶常识"
    );

    /** 科目简称（用于提示文案与表格列，如「科目二」） */
    public static final Map<String, String> SUBJECT_SHORT = Map.of(
            "1", "科目一",
            "2", "科目二",
            "3", "科目三",
            "4", "科目四"
    );

    /** 取科目简称，未知科目回退为「科目X」 */
    public static String subjectShort(String code) {
        if (code == null) return "科目";
        return SUBJECT_SHORT.getOrDefault(code, "科目" + code);
    }

    /** 角色 */
    public static final List<String> ROLES = List.of("admin", "coach", "student");

    /** 缴费项目 / 方式 */
    public static final List<String> PAY_ITEMS = List.of("培训费", "补考费", "其它");
    public static final List<String> PAY_METHODS = List.of("现金", "微信", "支付宝", "银行");

    /**
     * 汇总给前端「字典接口」用的完整枚举。
     */
    public static Map<String, Object> dictionary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("licenseTypes", LICENSE_TYPES);
        map.put("subjects", SUBJECTS);
        map.put("roles", ROLES);
        map.put("studentStatus", List.of("待审核", "在训", "结业", "退学"));
        map.put("bookingStatus", List.of("待确认", "已确认", "已完成", "已取消", "爽约"));
        map.put("auditStatus", List.of("待审核", "已通过", "已驳回"));
        map.put("examStatus", List.of("待考", "合格", "不合格", "补考中"));
        map.put("coachStatus", List.of("在职", "停用"));
        map.put("vehicleStatus", List.of("可用", "维修", "停用"));
        map.put("scheduleStatus", List.of("可约", "已满", "停用"));
        map.put("planStatus", List.of("报名中", "已截止", "已完成"));
        map.put("paymentStatus", List.of("未缴", "已缴", "退费"));
        map.put("payItems", PAY_ITEMS);
        map.put("payMethods", PAY_METHODS);
        return map;
    }

    /** 把逗号分隔的准教车型拆成 List，便于前端直接渲染 */
    public static List<String> splitTypes(String types) {
        if (types == null || types.isBlank()) return List.of();
        return Arrays.stream(types.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
