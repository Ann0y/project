<script setup>
/**
 * F10 工作台（管理端）
 * ---------------------------------------------------------------------------
 * 需求 F10：指标卡概览 + 待办事项。
 * 数据：GET /api/statistics/overview
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
import { useDataStore } from '../../stores/data'

const data = useDataStore()
const loading = ref(false)

const d = computed(() => data.dashboard)

onMounted(async () => {
  loading.value = true
  try {
    await data.loadDashboard()
  } catch (e) {
    ElMessage.error(e.message || '加载工作台失败')
  } finally {
    loading.value = false
  }
})

const todos = computed(() => {
  const t = d.value?.todo ?? {}
  return [
    { label: '待审核报名', count: t.pendingAudit ?? 0, to: { name: 'students' } },
    { label: '待审核学时', count: t.pendingHours ?? 0, to: { name: 'admin-training' } },
    { label: '待确认预约', count: t.pendingBooking ?? 0, to: { name: 'admin-training' } },
    { label: '待录入成绩', count: t.pendingScore ?? 0, to: { name: 'admin-exams' } },
  ]
})

/** 报名量趋势（近 12 个月） */
const trendOption = computed(() => {
  const t = chartTheme()
  const list = d.value?.enrollTrend ?? []
  return {
    grid: { left: 8, right: 14, top: 20, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: list.map((m) => String(m.month).slice(2)),
      boundaryGap: false,
      ...t.axisStyle,
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      name: '人',
      nameTextStyle: { color: t.label, fontSize: 11 },
      ...t.axisStyle,
    },
    series: [
      {
        name: '报名人数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: list.map((m) => m.count),
        itemStyle: { color: t.accent },
        lineStyle: { width: 2, color: t.accent },
        areaStyle: {
          color: t.areaGradient('rgba(43,82,212,0.16)'),
        },
      },
    ],
  }
})

/** 准驾车型分布 */
const pieOption = computed(() => {
  const t = chartTheme()
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
    legend: {
      bottom: 0,
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: t.label, fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: t.surface, borderWidth: 2 },
        label: { show: false },
        labelLine: { show: false },
        data: d.value?.licenseDistribution ?? [],
      },
    ],
  }
})

/** 教练带教工作量 */
const coachOption = computed(() => {
  const t = chartTheme()
  const list = [...(d.value?.coachWorkload ?? [])].sort((a, b) => a.students - b.students)
  return {
    grid: { left: 8, right: 30, top: 12, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      name: '人',
      nameTextStyle: { color: t.label, fontSize: 11 },
      ...t.axisStyle,
    },
    yAxis: {
      type: 'category',
      data: list.map((c) => c.coachName),
      ...t.axisStyle,
      splitLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        itemStyle: { color: t.accent, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: t.label, fontSize: 11 },
        data: list.map((c) => c.students),
      },
    ],
  }
})
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="运营工作台"
      desc="核心运营指标概览与待办事项。"
    >
      <template #actions>
        <RouterLink :to="{ name: 'reports' }">
          <el-button type="primary">统计报表</el-button>
        </RouterLink>
      </template>
    </PageHeader>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else>
      <div class="grid grid--4">
        <StatCard label="在训学员" :value="d?.trainingCount ?? 0" unit="人" />
        <StatCard label="本月报名" :value="d?.monthEnroll ?? 0" unit="人" />
        <StatCard label="本月培训学时" :value="d?.monthHours ?? 0" unit="学时" />
        <StatCard label="考试通过率" :value="d?.passRate ?? 0" unit="%" />
      </div>

      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">待办事项</span>
        </div>
        <div class="grid grid--4">
          <RouterLink v-for="todo in todos" :key="todo.label" :to="todo.to" class="todo">
            <span class="todo__count">{{ todo.count }}</span>
            <span class="todo__label">{{ todo.label }}</span>
          </RouterLink>
        </div>
      </div>

      <div class="grid grid--sidebar">
        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">近 12 个月报名量</span>
          </div>
          <BaseChart :option="trendOption" height="250px" />
        </div>

        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">准驾车型分布</span>
          </div>
          <BaseChart :option="pieOption" height="250px" />
        </div>
      </div>

      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">教练带教工作量</span>
        </div>
        <BaseChart :option="coachOption" height="230px" />
      </div>
    </template>
  </section>
</template>

<style scoped>
.todo {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-base);
  background: var(--bg-sunken);
  text-decoration: none;
  transition: border-color 0.2s var(--ease-out), background-color 0.2s var(--ease-out);
}

.todo:hover {
  border-color: var(--border-strong);
}

.todo__count {
  font-size: 24px;
  font-weight: 680;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.todo__label {
  font-size: 12.5px;
  color: var(--text-secondary);
}
</style>
