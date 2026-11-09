<script setup>
/**
 * F02 我的培训进度（学员端首页）
 * ---------------------------------------------------------------------------
 * 需求 F02：准驾车型、报名状态、带教教练与车辆、缴费状态、
 *          已学/剩余学时、整体进度环形图、各科目进度。
 * 数据来自 GET /api/students/me/progress。
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, Tickets, Wallet } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import ErrorState from '../../components/ErrorState.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
import { statusTone } from '../../utils/status'
import { timeRange } from '../../utils/format'
import { useAuthStore } from '../../stores/auth'
import { useDataStore } from '../../stores/data'
import { subjectShort } from '../../data/demo'

const auth = useAuthStore()
const data = useDataStore()
const failed = ref(false)

/** 游客预览里展示的字段（仅占位，不编造数据） */
const guestSlots = [
  { label: '准驾车型' },
  { label: '报名状态' },
  { label: '带教教练' },
  { label: '已学 / 剩余学时' },
]

/** 预约占用中（待确认/已确认）的状态 */
const ACTIVE_STATUS = ['待确认', '已确认']

onMounted(load)

/** 加载进度 + 预约（预约用于首页「临近培训提醒」，须保证最新），可重试 */
async function load() {
  if (!auth.isAuthenticated) return
  failed.value = false
  try {
    // 预约 / 成绩 / 缴费每次都重新拉：教练确认、管理员录分、缴费状态变化都要及时反映
    await Promise.all([
      data.loadProgress(),
      data.loadMyBookings(),
      data.loadMyExamRecords(),
      data.loadMyPayments(),
    ])
  } catch (e) {
    failed.value = true
    ElMessage.error(e.message || '加载培训进度失败')
  }
}

const student = computed(() => data.progress)

/** 各科目进度（后端已按准驾车型给出 done/required/percent） */
const subjects = computed(() =>
  (student.value?.subjects ?? []).map((s) => ({
    ...s,
    short: subjectShort(s.code),
    done: Number(s.done ?? 0),
    required: Number(s.required ?? 0),
  })),
)

const doneHours = computed(() => Number(student.value?.doneHours ?? 0))
const remainHours = computed(() => Number(student.value?.remainHours ?? 0))
const percent = computed(() => student.value?.percent ?? 0)
const requiredHours = computed(() => Number(student.value?.requiredHours ?? 0))

/** 已通过科目数（按考试记录去重，合格即算通过） */
const passedSubjects = computed(
  () => new Set(data.examRecords.filter((r) => r.status === '合格').map((r) => r.subjectCode)).size,
)
/** 待考 / 补考中场次 */
const pendingExams = computed(
  () => data.examRecords.filter((r) => r.status === '待考' || r.status === '补考中').length,
)
/** 未完成预约数 */
const activeBookingCount = computed(
  () => data.bookings.filter((b) => ACTIVE_STATUS.includes(b.status)).length,
)

/** 待缴费用 */
const unpaidPayments = computed(() => data.payments.filter((p) => p.status === '未缴'))
const unpaidTotal = computed(() =>
  unpaidPayments.value.reduce((acc, p) => acc + Number(p.amount ?? 0), 0),
)

/** 首页快捷入口 */
const quickActions = [
  { name: 'training', title: '预约培训', desc: '选择教练与时段', icon: Calendar },
  { name: 'hours', title: '我的学时', desc: '查看学时审核明细', icon: Clock },
  { name: 'exams', title: '考试与成绩', desc: '查看交管系统同步的成绩', icon: Tickets },
  { name: 'payments', title: '学费缴纳', desc: '缴纳待缴费用', icon: Wallet },
]

function money(value) {
  return Number(value ?? 0).toFixed(2)
}

function hoursText(value) {
  const n = Number(value ?? 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

/* -------- 临近培训提醒：最近一条「今天及以后」的未完成预约 -------- */

/** 本地时区的 yyyy-MM-dd（与 TrainingView 保持一致，不用 toISOString） */
function localDateKey(d) {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** 每次计算都取当天，避免页面跨天停留后日期偏移 */
const nextBooking = computed(() => {
  const todayKey = localDateKey(new Date())
  const upcoming = data.bookings
    .filter(
      (b) => ACTIVE_STATUS.includes(b.status) && b.scheduleDate && b.scheduleDate >= todayKey,
    )
    .sort((a, b) =>
      a.scheduleDate === b.scheduleDate
        ? String(a.startTime).localeCompare(String(b.startTime))
        : a.scheduleDate < b.scheduleDate ? -1 : 1,
    )
  return upcoming[0] ?? null
})

/** 距离最近预约的天数标签：今天 / 明天 / 后天 / N 天后 */
function daysLabel(dateKey) {
  const todayKey = localDateKey(new Date())
  const diff = Math.round((new Date(`${dateKey}T00:00:00`) - new Date(`${todayKey}T00:00:00`)) / 86400000)
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === 2) return '后天'
  return `${diff} 天后`
}

const nextBookingIn = computed(() => (nextBooking.value ? daysLabel(nextBooking.value.scheduleDate) : ''))

/** 整体进度环形图 */
const gaugeOption = computed(() => {
  const t = chartTheme()
  return {
    series: [
      {
        type: 'pie',
        radius: ['70%', '86%'],
        center: ['50%', '50%'],
        silent: true,
        label: {
          show: true,
          position: 'center',
          formatter: `{v|${percent.value}%}\n{k|整体完成率}`,
          rich: {
            v: { fontSize: 28, fontWeight: 700, color: t.text, lineHeight: 34 },
            k: { fontSize: 11, color: t.label, lineHeight: 18 },
          },
        },
        labelLine: { show: false },
        data: [
          { value: percent.value, itemStyle: { color: t.accent } },
          {
            value: Math.max(0, 100 - percent.value),
            itemStyle: { color: t.track },
          },
        ],
      },
    ],
  }
})

/** 各科目学时：已完成 vs 大纲要求 */
const subjectOption = computed(() => {
  const t = chartTheme()
  return {
    grid: { left: 8, right: 12, top: 32, bottom: 4, containLabel: true },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: t.label, fontSize: 11 },
    },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: subjects.value.map((s) => s.short),
      ...t.axisStyle,
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      name: '学时',
      nameTextStyle: { color: t.label, fontSize: 11 },
      ...t.axisStyle,
    },
    series: [
      {
        name: '已完成',
        type: 'bar',
        barWidth: 18,
        itemStyle: { color: t.accent, borderRadius: [4, 4, 0, 0] },
        data: subjects.value.map((s) => s.done),
      },
      {
        name: '大纲要求',
        type: 'bar',
        barWidth: 18,
        itemStyle: { color: t.soft, borderRadius: [4, 4, 0, 0] },
        data: subjects.value.map((s) => s.required),
      },
    ],
  }
})
</script>

<template>
  <section class="page page--student">
    <PageHeader
      eyebrow="学员端"
      :title="auth.isAuthenticated ? `${auth.displayName}，欢迎回来` : '培训进度（演示视图）'"
      :desc="
        auth.isAuthenticated
          ? '查看报名状态、学时完成情况、考试成绩与待缴费用。'
          : '首页默认面向学员展示，登录后可查看个人数据并在线约车。'
      "
    >
      <template #actions>
        <template v-if="auth.isAuthenticated">
          <RouterLink :to="{ name: 'hours' }">
            <el-button plain>查看学时明细</el-button>
          </RouterLink>
          <RouterLink :to="{ name: 'training' }">
            <el-button type="primary">去约车</el-button>
          </RouterLink>
        </template>
        <RouterLink v-else :to="{ name: 'login', query: { need: 'student' } }">
          <el-button type="primary">登录学员账号</el-button>
        </RouterLink>
      </template>
    </PageHeader>

    <!-- 游客：结构化预览空态，说明登录后能看到什么 -->
    <template v-if="!auth.isAuthenticated">
      <div class="glass panel guest">
        <div class="guest__head">
          <h2 class="guest__title">登录后查看你的培训进度</h2>
          <p class="guest__desc">
            报名状态、带教教练与车辆、已学与剩余学时、整体完成率以及各科目进度，登录学员账号后即可查看。
          </p>
        </div>

        <div class="guest__grid">
          <div v-for="item in guestSlots" :key="item.label" class="guest__slot">
            <span class="guest__slot-label">{{ item.label }}</span>
            <span class="guest__slot-value">-</span>
          </div>
        </div>
      </div>
    </template>

    <el-skeleton v-else-if="data.loading && !student" :rows="6" animated />

    <template v-else-if="student">
      <!-- 关键信息 -->
      <div class="glass panel">
        <div class="info-grid">
          <div class="info">
            <span class="info__k">姓名</span>
            <span class="info__v">{{ student.name }}</span>
          </div>
          <div class="info">
            <span class="info__k">准驾车型</span>
            <span class="info__v">{{ student.licenseType }}</span>
          </div>
          <div class="info">
            <span class="info__k">报名状态</span>
            <span class="info__v">{{ student.status }}</span>
          </div>
          <div class="info">
            <span class="info__k">带教教练</span>
            <span class="info__v">{{ student.coachName || '待分配' }}</span>
          </div>
          <div class="info">
            <span class="info__k">教练车辆</span>
            <span class="info__v">{{ student.vehiclePlate || '待分配' }}</span>
          </div>
          <div class="info">
            <span class="info__k">已学 / 剩余学时</span>
            <span class="info__v">{{ doneHours }} / {{ remainHours }}</span>
          </div>
          <div class="info">
            <span class="info__k">缴费状态</span>
            <span class="info__v">
              <span class="chip" :class="statusTone(student.paymentStatus ?? '未缴')">
                {{ student.paymentStatus ?? '未缴' }}
              </span>
            </span>
          </div>
        </div>
      </div>

      <!-- 关键指标 -->
      <div class="home-stats">
        <div class="glass stat-card">
          <span class="stat-card__label">已学学时</span>
          <span class="stat-card__value">
            {{ hoursText(doneHours) }}<small>/ {{ hoursText(requiredHours) }} 学时</small>
          </span>
          <span class="stat-card__foot">仅统计审核通过的培训记录</span>
        </div>
        <div class="glass stat-card">
          <span class="stat-card__label">剩余学时</span>
          <span class="stat-card__value">{{ hoursText(remainHours) }}<small>学时</small></span>
          <RouterLink class="stat-card__link" :to="{ name: 'hours' }">查看学时明细 ›</RouterLink>
        </div>
        <div class="glass stat-card">
          <span class="stat-card__label">整体完成率</span>
          <span class="stat-card__value">{{ percent }}<small>%</small></span>
          <span class="stat-card__foot">
            {{ student.graduationReady ? '已满足结业条件' : '未满足结业条件' }}
          </span>
        </div>
        <div class="glass stat-card">
          <span class="stat-card__label">考试进度</span>
          <span class="stat-card__value">{{ passedSubjects }}<small>/ 4 科已通过</small></span>
          <RouterLink class="stat-card__link" :to="{ name: 'exams' }">
            {{ pendingExams > 0 ? `待考 ${pendingExams} 场 ›` : '查看考试成绩 ›' }}
          </RouterLink>
        </div>
      </div>

      <!-- 待缴学费提醒 -->
      <RouterLink v-if="unpaidPayments.length" class="fee-alert" :to="{ name: 'payments' }">
        <span class="fee-alert__icon">
          <el-icon><Wallet /></el-icon>
        </span>
        <span class="fee-alert__body">
          <span class="fee-alert__title">
            你有 {{ unpaidPayments.length }} 笔待缴费用，合计 ¥{{ money(unpaidTotal) }}
          </span>
          <span class="fee-alert__desc">点击前往学费缴纳，在线完成缴费后状态会实时更新</span>
        </span>
        <span class="fee-alert__action">去缴费 ›</span>
      </RouterLink>

      <!-- 临近培训提醒：最近一条今天及以后的未完成预约 -->
      <RouterLink v-if="nextBooking" class="upcoming" :to="{ name: 'training' }">
        <span class="chip chip--pending">{{ nextBookingIn }}</span>
        <span class="upcoming__text">
          {{ nextBooking.scheduleDate }} {{ timeRange(nextBooking.startTime, nextBooking.endTime) }}
          开始培训（{{ nextBooking.status }}）
        </span>
        <span class="upcoming__more">去约车 ›</span>
      </RouterLink>

      <!-- 快捷入口 -->
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">快捷入口</span>
          <span class="panel__hint">常用操作一键直达</span>
        </div>
        <div class="quick-grid">
          <RouterLink
            v-for="action in quickActions"
            :key="action.name"
            class="quick-card"
            :to="{ name: action.name }"
          >
            <span class="quick-card__icon">
              <el-icon><component :is="action.icon" /></el-icon>
            </span>
            <span class="quick-card__body">
              <span class="quick-card__title">{{ action.title }}</span>
              <span class="quick-card__desc">{{ action.desc }}</span>
            </span>
            <span class="quick-card__arrow">›</span>
          </RouterLink>
        </div>
      </div>

      <!-- 图表 -->
      <div class="grid grid--sidebar">
        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">各科目学时进度</span>
            <span class="panel__hint">已完成 vs 大纲要求</span>
          </div>
          <BaseChart :option="subjectOption" height="260px" :empty="subjects.length === 0" />
        </div>

        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">整体完成率</span>
          </div>
          <BaseChart :option="gaugeOption" height="210px" />
          <div class="u-center">
            <span class="chip" :class="student.graduationReady ? 'chip--solid' : 'chip--muted'">
              {{ student.graduationReady ? '已满足结业条件' : '未满足结业条件' }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <ErrorState v-else-if="failed" message="培训进度加载失败，请检查后端服务后重试" @retry="load" />

    <el-empty v-else description="暂无培训进度数据" />
  </section>
</template>

<style scoped>
.guest {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 32px 30px;
}

.guest__head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  max-width: 620px;
}

.guest__title {
  margin: 0;
  font-size: 20px;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.guest__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.guest__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.guest__slot {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px;
  border-radius: var(--radius-md);
  border: 1px dashed var(--border-base);
  background: var(--bg-sunken);
}

.guest__slot-label {
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--text-tertiary);
}

.guest__slot-value {
  font-size: 20px;
  font-weight: 650;
  color: var(--text-tertiary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info__k {
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--text-tertiary);
}

.info__v {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.u-center {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

/* 临近培训提醒条 */
.upcoming {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-base);
  background: var(--bg-sunken);
  color: var(--text-primary);
  text-decoration: none;
  transition: border-color 0.15s ease;
}

.upcoming:hover {
  border-color: var(--border-strong);
}

.upcoming__text {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
}

.upcoming__more {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 首页指标卡 */
.home-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.home-stats .stat-card {
  gap: 10px;
}

.stat-card__link {
  font-size: 12px;
  color: var(--accent);
  text-decoration: none;
}

.stat-card__link:hover {
  text-decoration: underline;
}

/* 待缴学费提醒 */
.fee-alert {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid var(--accent-border);
  border-radius: var(--radius-md);
  background: var(--accent-soft);
  color: var(--text-primary);
  text-decoration: none;
  transition: border-color 0.15s ease;
}

.fee-alert:hover {
  border-color: var(--accent);
}

.fee-alert__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: var(--white);
  color: var(--accent);
  font-size: 19px;
}

.fee-alert__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.fee-alert__title {
  font-size: 13.5px;
  font-weight: 650;
}

.fee-alert__desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.fee-alert__action {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--accent);
  white-space: nowrap;
}

/* 快捷入口 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--bg-sunken);
  color: var(--text-primary);
  text-decoration: none;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.quick-card:hover {
  border-color: var(--accent-border);
  background: var(--accent-soft);
}

.quick-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: var(--white);
  border: 1px solid var(--border-base);
  color: var(--accent);
  font-size: 17px;
  flex: none;
}

.quick-card__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.quick-card__title {
  font-size: 13.5px;
  font-weight: 650;
}

.quick-card__desc {
  font-size: 11.5px;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick-card__arrow {
  margin-left: auto;
  color: var(--text-tertiary);
  font-size: 15px;
}

@media (max-width: 1024px) {
  .home-stats,
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .guest__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .upcoming {
    flex-wrap: wrap;
  }
}
</style>
