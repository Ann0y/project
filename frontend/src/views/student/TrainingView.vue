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

/** 今天起 5 天 */
const dateOptions = computed(() => {
  const list = []
  const base = new Date()
  base.setHours(0, 0, 0, 0)
  for (let i = 0; i < BOOKING_WINDOW_DAYS; i++) {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
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
 *     （之前不区分这两者，导致用户选的日期被立刻改回去，表现为「选不了」）
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

// ① 首次与数据变化：重算默认日期（不动用户正在选的日期）
watch([dateOptions, slotsByDate], () => {
  pickDefaultDate()
}, { immediate: true })

// ② 用户切换日期后：标记为用户手动选择，并为该日期挑一个可选时段
watch(selectedDate, (val, oldVal) => {
  // 初始化时的赋值不算用户操作
  if (oldVal !== undefined && val) userPickedDate.value = true
  ensureSlotSelected()
})

// ③ 数据刷新后：保证已选时段仍然有效（例如被别人约满了）
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

async function cancel(booking) {
  if (booking.status !== '待确认') {
    ElMessage.warning('仅「待确认」的预约可以取消')
    return
  }
  try {
    await ElMessageBox.confirm('取消后该时段将被释放，确定取消吗？', '取消预约', {
      confirmButtonText: '确定取消',
      cancelButtonText: '保留',
      type: 'warning',
    })
  } catch {
    return
  }
  const result = await data.setBookingStatus(booking.id, '已取消')
  if (result !== true && !result?.ok) {
    ElMessage.error(result?.reason || '取消失败')
    return
  }
  ElMessage.success('预约已取消')
  await reload()
}
</script>

<template>
  <section class="page">
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
            <span>
              {{ b.scheduleDate }} {{ timeRange(b.startTime, b.endTime) }}
            </span>
            <span class="chip" :class="statusTone(b.status)">{{ b.status }}</span>
            <el-button
              v-if="b.status === '待确认'"
              link
              type="danger"
              size="small"
              @click="cancel(b)"
            >
              取消该预约
            </el-button>
            <span v-else class="u-dim">请等待培训完成</span>
          </li>
        </ul>
      </div>

      <div v-if="canBook" class="booking-form">
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

      <p v-if="!canBook" class="booking-form__tip">
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
        <el-table-column
          prop="createTime"
          label="提交时间"
          min-width="170"
          :formatter="(row) => dateTime(row.createTime)"
        />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '待确认'"
              link
              type="danger"
              size="small"
              @click="cancel(row)"
            >
              取消预约
            </el-button>
            <span v-else class="u-dim">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.booking-form {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}

.booking-form__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  gap: 10px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-strong);
  background: var(--bg-sunken);
  font-size: 13px;
  color: var(--text-primary);
}

.booking-block__head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.booking-block__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.booking-block__item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.booking-form__tip {
  margin: 12px 0 0;
  font-size: 12.5px;
  color: var(--text-secondary);
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
