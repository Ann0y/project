/**
 * 展示层格式化工具
 * ---------------------------------------------------------------------------
 * 统一表格 / 卡片里的时间与空值呈现，避免各页面各写一套：
 *   · 时段只显示到分钟（秒数对排课与预约没有意义，去掉更干净）
 *   · 空值统一用短横线占位
 */

/** 空值占位符 */
export const EMPTY = '-'

/** "13:30:00" → "13:30"（去秒） */
export function shortTime(value) {
  return String(value ?? '').slice(0, 5)
}

/** 时段文案："13:30 - 15:30" */
export function timeRange(start, end) {
  const s = shortTime(start)
  const e = shortTime(end)
  if (s && e) return `${s} - ${e}`
  return s || e || EMPTY
}

/** 日期时间："2026-10-17T15:11:42" → "2026-10-17 15:11" */
export function dateTime(value) {
  if (!value) return EMPTY
  return String(value).replace('T', ' ').slice(0, 16)
}
