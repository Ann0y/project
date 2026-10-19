/**
 * 状态色标工具
 * ---------------------------------------------------------------------------
 * 需求 8「易用性」要求：预约 / 考试状态色标化。
 * 全站以冷调中性灰为主，状态语义用低饱和色区分：
 *   pending  待处理 → 琥珀
 *   progress 进行中 → 墨蓝
 *   ok       完成   → 墨绿
 *   solid    结业等强调态 → 墨蓝实心
 *   muted    已结束 / 停用 → 中性灰
 *   warn     异常   → 琥珀描边
 *   danger   驳回 / 不合格 → 砖红
 */

const TONE_MAP = {
  // 待处理
  待审核: 'pending',
  待确认: 'pending',
  待考: 'pending',
  未缴: 'pending',

  // 进行中
  报名中: 'progress',
  培训中: 'progress',
  在训: 'progress',
  补考中: 'progress',

  // 完成 / 正常
  已通过: 'strong',
  已确认: 'strong',
  合格: 'strong',
  已完成: 'strong',
  已缴: 'strong',
  可约: 'strong',
  在职: 'strong',
  可用: 'strong',
  结业: 'solid',

  // 中性 / 结束
  已取消: 'muted',
  已满: 'muted',
  已截止: 'muted',
  停用: 'muted',
  未开始: 'muted',
  退费: 'muted',

  // 异常
  爽约: 'warn',

  // 驳回 / 不合格
  已驳回: 'danger',
  不合格: 'danger',
  退学: 'danger',
}

/** 返回 chip 的修饰类名 */
export function statusTone(status) {
  const tone = TONE_MAP[status] ?? 'muted'
  return `chip--${tone}`
}
