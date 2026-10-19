<script setup>
/**
 * F15 统计报表（管理端）· E2 运营数据可视化看板
 * ---------------------------------------------------------------------------
 * 需求 E2：报名量趋势 / 准驾车型分布 / 考试通过率 / 教练工作量。
 * 数据：GET /api/statistics/report（支持统一筛选）
 */
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
import { useDataStore } from '../../stores/data'
import { LICENSE_TYPES, SUBJECTS, subjectShort } from '../../data/demo'

const data = useDataStore()
const loading = ref(false)

const filters = ref({ licenseType: '', subject: '', coachId: null })

const r = computed(() => data.report)

async function load() {
  loading.value = true
  try {
    await data.loadReport({
      licenseType: filters.value.licenseType || undefined,
      subject: filters.value.subject || undefined,
      coachId: filters.value.coachId || undefined,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载报表失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  try {
    await data.loadCoaches()
  } catch {
    /* 教练筛选项加载失败不影响主视图 */
  }
})

watch(() => [filters.value.licenseType, filters.value.subject, filters.value.coachId], load)

/* ------------------------------- 图表 ------------------------------- */

const trendOption = computed(() => {
  const t = chartTheme()
  const list = r.value?.enrollTrend ?? []
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
      },
    ],
  }
})

const licenseOption = computed(() => {
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
        label: { show: true, formatter: '{b}\n{d}%', color: t.label, fontSize: 11 },
        data: r.value?.licenseDistribution ?? [],
      },
    ],
  }
})

const passOption = computed(() => {
  const t = chartTheme()
  const list = r.value?.subjectPassRate ?? []
  return {
    grid: { left: 8, right: 14, top: 30, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${v}%` },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: t.label, fontSize: 11 },
    },
    xAxis: {
      type: 'category',
      data: list.map((d) => d.subject),
      ...t.axisStyle,
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      max: 100,
      name: '%',
      nameTextStyle: { color: t.label, fontSize: 11 },
      ...t.axisStyle,
    },
    series: [
      {
        name: '通过率',
        type: 'bar',
        barWidth: 20,
        itemStyle: { color: t.accent, borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', color: t.label, fontSize: 11, formatter: '{c}%' },
        data: list.map((d) => d.passRate),
      },
      {
        name: '补考率',
        type: 'bar',
        barWidth: 20,
        itemStyle: { color: t.soft, borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', color: t.label, fontSize: 11, formatter: '{c}%' },
        data: list.map((d) => d.retakeRate),
      },
    ],
  }
})

const workloadOption = computed(() => {
  const t = chartTheme()
  const list = [...(r.value?.coachWorkload ?? [])].sort(
    (a, b) => Number(a.hours) - Number(b.hours),
  )
  return {
    grid: { left: 8, right: 44, top: 12, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      name: '学时',
      nameTextStyle: { color: t.label, fontSize: 11 },
      ...t.axisStyle,
    },
    yAxis: {
      type: 'category',
      data: list.map((d) => d.coach),
      ...t.axisStyle,
      splitLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        itemStyle: { color: t.accent, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: t.label, fontSize: 11 },
        data: list.map((d) => Number(d.hours)),
      },
    ],
  }
})

const summary = computed(() => r.value?.summary ?? {})
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="运营数据可视化看板"
      desc="报名、考试通过率与教练工作量统计，支持准驾车型、科目、教练筛选。"
    />

    <div class="glass panel filters">
      <div class="filters__row">
        <div class="filters__field">
          <span class="filters__label">准驾车型</span>
          <el-select v-model="filters.licenseType" placeholder="全部" clearable size="small" style="width: 120px">
            <el-option v-for="t in LICENSE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </div>
        <div class="filters__field">
          <span class="filters__label">科目</span>
          <el-select v-model="filters.subject" placeholder="全部" clearable size="small" style="width: 120px">
            <el-option
              v-for="s in SUBJECTS"
              :key="s.code"
              :label="subjectShort(s.code)"
              :value="s.code"
            />
          </el-select>
        </div>
        <div class="filters__field">
          <span class="filters__label">教练</span>
          <el-select v-model="filters.coachId" placeholder="全部" clearable size="small" style="width: 140px">
            <el-option v-for="c in data.coaches" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
      </div>

      <div class="filters__summary">
        <span>学员总数 <strong>{{ summary.studentCount ?? 0 }}</strong></span>
        <span>在训学员 <strong>{{ summary.trainingCount ?? 0 }}</strong></span>
        <span>已通过学时 <strong>{{ summary.approvedHours ?? 0 }}</strong></span>
        <span>考试通过率 <strong>{{ summary.passRate ?? 0 }}%</strong></span>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else>
      <div class="grid grid--sidebar">
        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">报名量趋势</span>
          </div>
          <BaseChart :option="trendOption" height="260px" />
        </div>

        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">准驾车型分布</span>
          </div>
          <BaseChart :option="licenseOption" height="260px" />
        </div>
      </div>

      <div class="grid grid--2">
        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">各科目考试通过率</span>
          </div>
          <BaseChart :option="passOption" height="260px" />
        </div>

        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">教练工作量</span>
          </div>
          <BaseChart :option="workloadOption" height="260px" />
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.filters {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filters__row {
  display: flex;
  align-items: center;
  gap: 26px;
  flex-wrap: wrap;
}

.filters__field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filters__label {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.filters__summary {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
  font-size: 12.5px;
  color: var(--text-secondary);
}

.filters__summary strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
</style>
