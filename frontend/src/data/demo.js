/**
 * 前端本地常量
 * ---------------------------------------------------------------------------
 * 只保留「枚举文案 / 科目名称」这类与后端约定一致的静态常量。
 * 业务数据一律通过 @/stores/data 调后端接口获取。
 *
 * 需要以后端为准的动态枚举时可调用 GET /api/enums；
 * 这里保留本地常量是为了首屏无闪烁（下拉框立即有选项）。
 */

export const LICENSE_TYPES = ['C1', 'C2', 'B2', 'A2']

export const SUBJECTS = [
  { code: '1', short: '科目一', name: '科目一 · 道路交通安全法律法规' },
  { code: '2', short: '科目二', name: '科目二 · 场地驾驶技能' },
  { code: '3', short: '科目三', name: '科目三 · 道路驾驶技能' },
  { code: '4', short: '科目四', name: '科目四 · 安全文明驾驶常识' },
]

export const STUDENT_STATUS = ['待审核', '在训', '结业', '退学']
export const BOOKING_STATUS = ['待确认', '已确认', '已完成', '已取消', '爽约']
export const AUDIT_STATUS = ['待审核', '已通过', '已驳回']
export const EXAM_STATUS = ['待考', '合格', '不合格', '补考中']
export const VEHICLE_STATUS = ['可用', '维修', '停用']
export const PLAN_STATUS = ['报名中', '已截止', '已完成']

export function subjectShort(code) {
  const hit = SUBJECTS.find((s) => s.code === String(code))
  return hit ? hit.short : `科目${code}`
}
