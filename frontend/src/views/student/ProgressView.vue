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
import PageHeader from '../../components/PageHeader.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
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

onMounted(async () => {
  if (!auth.isAuthenticated) return
  try {
    await data.loadProgress()
  } catch (e) {
    failed.value = true
    ElMessage.error(e.message || '加载培训进度失败')
  }
})

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
  <section class="page">
    <PageHeader
      eyebrow="学员端"
      :title="auth.isAuthenticated ? `${auth.displayName}，欢迎回来` : '培训进度（演示视图）'"
      :desc="
        auth.isAuthenticated
          ? '查看报名状态、学时完成情况与各科目进度。'
          : '首页默认面向学员展示，登录后可查看个人数据并在线约车。'
      "
    >
      <template #actions>
        <RouterLink v-if="auth.isAuthenticated" :to="{ name: 'training' }">
          <el-button type="primary">去约车</el-button>
        </RouterLink>
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

    <el-empty v-else-if="failed" description="加载失败，请检查后端服务是否启动" />

    <el-empty v-else description="暂无培训进度数据" />
  </section>
</template>

<style scoped>
.guest {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 28px 26px;
}

.guest__head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
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
  gap: 12px;
}

.guest__slot {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
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
  gap: 18px;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 3px;
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
  margin-top: 8px;
}

@media (max-width: 768px) {
  .guest__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
