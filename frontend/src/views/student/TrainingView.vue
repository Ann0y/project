<script setup>
/**
 * F03 培训与约车（学员端）
 * ---------------------------------------------------------------------------
 * 需求 F03：查看本人带教教练的可预约时段并提交 / 取消预约。
 * 固定带教：只能预约本人带教教练发布的时段（需求 2）。
 *
 * 交互：改为「日期下拉 + 时段下拉」两步选择，避免一次铺开大量时段卡片。
 * 限制：只能选择今天起 5 天内的时段（与后端 BOOKING_WINDOW_DAYS 保持一致）；
 *       一名学员最多同时持有 2 条未完成预约（待确认 / 已确认），与后端 MAX_ACTIVE_BOOKINGS 一致。
 * 冲突检测（E1）由后端 book() 完成，前端只展示结果。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { EMPTY, timeRange, dateTime } from '../../utils/format'
import { useDataStore } from '../../stores/data'

/** 可预约时间窗口（天）：今天 ~ 第 4 天 */
const BOOKING_WINDOW_DAYS = 5

/** 允许同时持有的未完成预约数（与后端 StudentPortalServiceImpl.MAX_ACTIVE_BOOKINGS 一致） */
const MAX_ACTIVE_BOOKINGS = 2

const data = useDataStore()
const loading = ref(false)
const submitting = ref(false)

/** 下拉选择状态 */
const selectedDate = ref('')
const selectedSlotId = ref(null)

/* ------------------------------ 日期选项 ------------------------------ */

/** 本地时区的 yyyy-MM-dd（不用 toISOString，避免时区偏移一天） */
function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const WEEK_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

/** 今天起 5 天（周六、周日不排课，直接不出现在可选日期里） */
const dateOptions = computed(() => {
  const list = []
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  for (let i = 0; i < BOOKING_WINDOW_DAYS; i++) {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
    // 0 = 周日，6 = 周六
    if (d.getDay() === 0 || d.getDay() === 6) continue
    const value = formatDate(d)
    const label = i === 0 ? '今天' : i === 1 ? '明天' : WEEK_LABELS[d.getDay()]
    list.push({ value, label: `${value}（${label}）` })
  }
  return list
})

/* ------------------------------ 时段选项 ------------------------------ */

/** 把所有可约时段按日期索引，便于下拉快速取用 */
const slotsByDate = computed(() => {
  const map = {}
  for (const s of data.schedules) {
    const key = s.scheduleDate
    ;(map[key] ??= []).push(s)
  }
  for (const key of Object.keys(map)) {
    map[key].sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
  }
  return map
})

/**
 * 我当前占用的时段 id。
 * 只有「待确认 / 已确认」才算占用：已完成、已取消、爽约都不再占用，
 * 否则下拉里会堆满「已预约」，看不出哪些还能选。
 */
const ACTIVE_STATUS = ['待确认', '已确认']

const bookedIds = computed(
  () =>
    new Set(
      data.bookings
        .filter((b) => ACTIVE_STATUS.includes(b.status))
        .map((b) => b.scheduleId),
    ),
)

/** 当前未完成的预约（最多 MAX_ACTIVE_BOOKINGS 条） */
const activeBookings = computed(() =>
  data.bookings.filter((b) => ACTIVE_STATUS.includes(b.status)),
)

/** 是否还能继续预约（未完成预约数未达上限） */
const canBook = computed(() => activeBookings.value.length < MAX_ACTIVE_BOOKINGS)

/** 把某天的时段转成下拉选项 */
function toOptions(dateKey) {
  const list = slotsByDate.value[dateKey] ?? []
  return list.map((s) => {
    const mine = bookedIds.value.has(s.id)
    const full = s.status !== '可约'
    let suffix = ''
    if (mine) suffix = ' · 我的预约'
    else if (full) suffix = ` · ${s.status}`
    return {
      value: s.id,
      label: `${timeRange(s.startTime, s.endTime)}${suffix}`,
      disabled: mine || full,
      slot: s,
    }
  })
}

/** 当前日期下的时段下拉选项 */
const slotOptions = computed(() => toOptions(selectedDate.value))

/**
 * 首次加载时挑一个「还有空位」的日期作为默认值。
 *
 * 注意与「用户手动选择」区分：
 *   - 首次初始化（用户尚未手动选过）→ 自动跳到有空的日期，避免一进来就是满的
 *   - 用户手动选过之后           → 尊重用户选择，即使当天已满也不改回
 */
const userPickedDate = ref(false)

function pickDefaultDate() {
  const opts = dateOptions.value
  if (!opts.length) return
  // 用户已手动选择 → 不再干预
  if (userPickedDate.value) return
  // 当前已选日期仍有空位 → 保持
  const currentHasFree = toOptions(selectedDate.value).some((x) => !x.disabled)
  if (opts.some((o) => o.value === selectedDate.value) && currentHasFree) return
  // 否则选第一个还有空位的日期
  const firstAvailable = opts.find((o) => toOptions(o.value).some((x) => !x.disabled))
  selectedDate.value = (firstAvailable ?? opts[0]).value
}

/** 时段默认值：仅当当前时段不可用时补一个可用的 */
function ensureSlotSelected() {
  const options = toOptions(selectedDate.value)
  const current = options.find((o) => o.value === selectedSlotId.value)
  if (current && !current.disabled) return
  const firstFree = options.find((o) => !o.disabled)
  selectedSlotId.value = firstFree ? firstFree.value : null
}

// 首次与数据变化：重算默认日期（不动用户正在选的日期）
watch([dateOptions, slotsByDate], () => {
  pickDefaultDate()
}, { immediate: true })

// 用户切换日期后：标记为用户手动选择，并为该日期挑一个可选时段
watch(selectedDate, (val, oldVal) => {
  // 初始化时的赋值不算用户操作
  if (oldVal !== undefined && val) userPickedDate.value = true
  ensureSlotSelected()
})

// 数据刷新后：保证已选时段仍然有效（例如被别人约满了）
watch([slotOptions], () => {
  ensureSlotSelected()
})

/* ------------------------------ 我的预约 ------------------------------ */

const myBookings = computed(() =>
  [...data.bookings].sort((a, b) =>
    a.scheduleDate === b.scheduleDate
      ? String(b.startTime).localeCompare(String(a.startTime))
      : a.scheduleDate < b.scheduleDate ? 1 : -1,
  ),
)

const coachName = computed(() => data.progress?.coachName ?? '')
const vehiclePlate = computed(() => data.progress?.vehiclePlate ?? '')
/** 是否已分配带教教练 / 是否处于可预约的「在训」状态 */
const hasCoach = computed(() => Boolean(data.progress?.coachId || data.progress?.coachName))
const isTraining = computed(() => data.progress?.status === '在训')

/** 距某日期的天数标签：今天 / 明天 / 后天 / N 天后（过期不显示） */
function daysLabel(dateKey) {
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  const diff = Math.round((new Date(`${dateKey}T00:00:00`) - base) / 86400000)
  if (diff < 0) return ''
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === 2) return '后天'
  return `${diff} 天后`
}

const dayLabelOf = (b) => daysLabel(b.scheduleDate)

/** 当天可约 / 总时段数，用于提示 */
const daySummary = computed(() => {
  const options = slotOptions.value
  return { total: options.length, free: options.filter((o) => !o.disabled).length }
})

/* ------------------------------ 数据加载 ------------------------------ */

async function reload() {
  loading.value = true
  try {
    await Promise.all([data.loadMySchedules(), data.loadMyBookings()])
  } catch (e) {
    ElMessage.error(e.message || '加载约车数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!data.progress) {
    try {
      await data.loadProgress()
    } catch {
      /* 教练信息缺失不影响约车主流程 */
    }
  }
  await reload()
})

/* ------------------------------ 提交 / 取消 ------------------------------ */

async function book() {
  const option = slotOptions.value.find((o) => o.value === selectedSlotId.value)
  if (!option) {
    ElMessage.warning('请选择预约时段')
    return
  }
  if (option.disabled) {
    ElMessage.warning('该时段不可预约')
    return
  }
  const slot = option.slot

  try {
    await ElMessageBox.confirm(
      `${slot.scheduleDate} ${slot.startTime}-${slot.endTime}，教练 ${coachName.value || '本人带教教练'}`,
      '确认预约该时段？',
      { confirmButtonText: '提交预约', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }

  submitting.value = true
  const result = await data.createBooking({ scheduleId: slot.id })
  submitting.value = false

  if (!result.ok) {
    ElMessage.warning(result.reason)
    await reload()
    return
  }
  ElMessage.success('预约已提交，等待教练确认')
  await reload()
}

/** 未完成预约可取消 / 改期 */
const activeStatus = (b) => b.status === '待确认' || b.status === '已确认'
const canCancel = activeStatus
const canReschedule = activeStatus

async function cancel(booking) {
  if (!canCancel(booking)) {
    ElMessage.warning('仅「待确认 / 已确认」的预约可以取消')
    return
  }
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      '取消后该时段将被释放。请填写取消原因（选填），便于教练了解情况。',
      '取消预约',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '保留',
        inputType: 'textarea',
        inputPlaceholder: '如：临时有事 / 身体不适 / 时间冲突',
        inputValidator: (v) => !v || v.length <= 100 || '取消原因不能超过 100 字',
        type: 'warning',
      },
    )
    reason = value ?? ''
  } catch {
    return
  }
  const result = await data.cancelBooking(booking.id, reason)
  if (!result?.ok) {
    ElMessage.error(result?.reason || '取消失败')
    return
  }
  ElMessage.success('预约已取消')
  await reload()
}
/** 已结束（非未完成）的预约允许删除记录 */
const deletable = (b) => b.status !== '待确认' && b.status !== '已确认'

/* ------------------------------ 预约改期 ------------------------------ */

const rescheduleVisible = ref(false)
const rescheduleTarget = ref(null)
const rescheduleDate = ref('')
const rescheduleSlotId = ref(null)
const rescheduling = ref(false)

/** 改期弹窗里的时段选项：排除当前预约、本人其他预约与已满 / 停用时段 */
const rescheduleSlotOptions = computed(() => {
  const target = rescheduleTarget.value
  if (!target) return []
  const list = slotsByDate.value[rescheduleDate.value] ?? []
  return list.map((s) => {
    const isCurrent = target.scheduleId === s.id
    const mine = bookedIds.value.has(s.id)
    const full = s.status !== '可约'
    let suffix = ''
    if (isCurrent) suffix = ' · 当前预约'
    else if (mine) suffix = ' · 我的预约'
    else if (full) suffix = ` · ${s.status}`
    return {
      value: s.id,
      label: `${timeRange(s.startTime, s.endTime)}${suffix}`,
      disabled: isCurrent || mine || full,
    }
  })
})

function ensureRescheduleSlot() {
  const options = rescheduleSlotOptions.value
  const current = options.find((o) => o.value === rescheduleSlotId.value)
  if (current && !current.disabled) return
  rescheduleSlotId.value = options.find((o) => !o.disabled)?.value ?? null
}

function openReschedule(booking) {
  rescheduleTarget.value = booking
  rescheduleSlotId.value = null
  // 优先选一个还有空位的日期，避免打开就是满的
  const firstAvailable = dateOptions.value.find((o) =>
    toOptions(o.value).some((x) => !x.disabled && x.value !== booking.scheduleId),
  )
  rescheduleDate.value = firstAvailable?.value ?? dateOptions.value[0]?.value ?? ''
  ensureRescheduleSlot()
  rescheduleVisible.value = true
}

watch(rescheduleDate, ensureRescheduleSlot)
watch(rescheduleSlotOptions, ensureRescheduleSlot)

async function submitReschedule() {
  const option = rescheduleSlotOptions.value.find((o) => o.value === rescheduleSlotId.value)
  if (!option || option.disabled) {
    ElMessage.warning('请选择一个可预约的新时段')
    return
  }
  rescheduling.value = true
  const result = await data.rescheduleBooking(rescheduleTarget.value.id, option.value)
  rescheduling.value = false
  if (!result?.ok) {
    ElMessage.warning(result?.reason || '改期失败')
    return
  }
  ElMessage.success('改期成功，等待教练重新确认')
  rescheduleVisible.value = false
  await reload()
}

async function removeBooking(booking) {
  try {
    await ElMessageBox.confirm('删除后该记录不可恢复，确定删除吗？', '删除预约记录', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  const result = await data.deleteMyBooking(booking.id)
  if (result !== true && !result?.ok) {
    ElMessage.error(result?.reason || '删除失败')
    return
  }
  ElMessage.success('预约记录已删除')
  await reload()
}
</script>

<template>
  <section class="page page--student">
    <PageHeader
      eyebrow="学员端"
      title="培训与约车"
      desc="选择日期与时段即可预约，只能预约今天起 5 天内的时段，提交时服务端自动做冲突检测。"
    />

    <!-- 预约表单 -->
    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">
          带教教练 {{ coachName || EMPTY }}<span v-if="vehiclePlate">（{{ vehiclePlate }}）</span>
        </span>
        <span class="panel__hint">可约 {{ daySummary.free }} / {{ daySummary.total }} 个时段</span>
      </div>

      <!-- 未完成预约列表：最多 2 条，达上限后不再显示预约表单 -->
      <div v-if="activeBookings.length" class="booking-block">
        <div class="booking-block__head">
          <span class="chip chip--strong">已有预约 {{ activeBookings.length }}/{{ MAX_ACTIVE_BOOKINGS }}</span>
          <span class="u-dim">未完成的预约最多 {{ MAX_ACTIVE_BOOKINGS }} 条</span>
        </div>
        <ul class="booking-block__list">
          <li v-for="b in activeBookings" :key="b.id" class="booking-block__item">
            <span v-if="dayLabelOf(b)" class="chip chip--pending">{{ dayLabelOf(b) }}</span>
            <span>
              {{ b.scheduleDate }} {{ timeRange(b.startTime, b.endTime) }}
            </span>
            <span class="chip" :class="statusTone(b.status)">{{ b.status }}</span>
            <el-button
              v-if="canReschedule(b)"
              link
              type="primary"
              size="small"
              @click="openReschedule(b)"
            >
              改期
            </el-button>
            <el-button
              v-if="canCancel(b)"
              link
              type="danger"
              size="small"
              @click="cancel(b)"
            >
              取消该预约
            </el-button>
            <span v-else-if="!canReschedule(b)" class="u-dim">请等待培训完成</span>
          </li>
        </ul>
      </div>

      <div v-if="canBook && hasCoach && isTraining" class="booking-form">
        <div class="booking-form__field">
          <span class="booking-form__label">培训日期</span>
          <el-select v-model="selectedDate" placeholder="选择日期" style="width: 200px">
            <el-option
              v-for="opt in dateOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>

        <div class="booking-form__field">
          <span class="booking-form__label">培训时段</span>
          <el-select
            v-model="selectedSlotId"
            placeholder="选择时段"
            style="width: 240px"
            :loading="loading"
            no-data-text="当天暂无可预约时段"
          >
            <el-option
              v-for="opt in slotOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
              :disabled="opt.disabled"
            />
          </el-select>
        </div>

        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!selectedSlotId"
          @click="book"
        >
          提交预约
        </el-button>
      </div>

      <p v-if="!isTraining" class="booking-form__tip">
        当前报名状态为「{{ data.progress?.status || '未知' }}」，仅「在训」学员可以预约培训。
      </p>
      <p v-else-if="!hasCoach" class="booking-form__tip">
        尚未分配带教教练，请联系管理员完成分车后再预约。
      </p>
      <p v-else-if="!canBook" class="booking-form__tip">
        已有 {{ MAX_ACTIVE_BOOKINGS }} 条未完成预约，完成或取消后才能继续预约。
      </p>
      <p v-else-if="!loading && daySummary.total === 0" class="booking-form__tip">
        该日期教练未排课，请换个日期。
      </p>
      <p v-else-if="!loading && daySummary.free === 0" class="booking-form__tip">
        该日期时段已约满，请换个日期。
      </p>
    </div>

    <!-- 我的预约 -->
    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">我的预约</span>
      </div>

      <el-empty v-if="myBookings.length === 0" description="暂无预约记录" />

      <el-table v-else :data="myBookings" stripe>
        <el-table-column prop="scheduleDate" label="培训日期" width="130" />
        <el-table-column label="时段" width="150">
          <template #default="{ row }">{{ timeRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="车辆" width="140">
          <template #default="{ row }">{{ row.vehiclePlate ?? EMPTY }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="取消原因" min-width="150">
          <template #default="{ row }">
            <span v-if="row.cancelReason">{{ row.cancelReason }}</span>
            <span v-else class="u-dim">-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="提交时间"
          min-width="170"
          :formatter="(row) => dateTime(row.createTime)"
        />
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button
              v-if="canReschedule(row)"
              link
              type="primary"
              size="small"
              @click="openReschedule(row)"
            >
              改期
            </el-button>
            <el-button
              v-if="canCancel(row)"
              link
              type="danger"
              size="small"
              @click="cancel(row)"
            >
              取消预约
            </el-button>
            <el-button
              v-else-if="deletable(row)"
              link
              type="danger"
              size="small"
              @click="removeBooking(row)"
            >
              删除记录
            </el-button>
            <span v-if="!canReschedule(row) && !deletable(row)" class="u-dim">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 改期弹窗 -->
    <el-dialog v-model="rescheduleVisible" title="预约改期" width="460px">
      <p v-if="rescheduleTarget" class="u-dim reschedule__current">
        当前预约：{{ rescheduleTarget.scheduleDate }}
        {{ timeRange(rescheduleTarget.startTime, rescheduleTarget.endTime) }}
      </p>

      <el-form label-position="top">
        <el-form-item label="新的培训日期">
          <el-select v-model="rescheduleDate" style="width: 100%">
            <el-option
              v-for="opt in dateOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="新的培训时段">
          <el-select
            v-model="rescheduleSlotId"
            placeholder="选择时段"
            style="width: 100%"
            no-data-text="当天暂无可预约时段"
          >
            <el-option
              v-for="opt in rescheduleSlotOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
              :disabled="opt.disabled"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <p v-if="!rescheduleSlotOptions.some((o) => !o.disabled)" class="booking-form__tip">
        最近 5 天暂无其他可约时段，请稍后再试。
      </p>

      <template #footer>
        <el-button @click="rescheduleVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="rescheduling"
          :disabled="!rescheduleSlotId"
          @click="submitReschedule"
        >
          确认改期
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.booking-form {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  flex-wrap: wrap;
}

.booking-form__field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.booking-form__label {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.booking-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-strong);
  background: var(--bg-sunken);
  font-size: 13px;
  color: var(--text-primary);
}

.booking-block__head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.booking-block__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.booking-block__item {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.booking-form__tip {
  margin: 16px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.reschedule__current {
  margin: 0 0 18px;
  font-size: 12.5px;
  line-height: 1.7;
}

@media (max-width: 640px) {
  .booking-form {
    flex-direction: column;
    align-items: stretch;
  }

  .booking-form__field :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
