<script setup>
/**
 * F05 教练工作台
 * ---------------------------------------------------------------------------
 * 需求 F05：概览今日排课、待确认预约、带教学员数等本人工作数据。
 * 数据：GET /api/coach/workbench
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
import { statusTone } from '../../utils/status'
import { timeRange } from '../../utils/format'
import { useAuthStore } from '../../stores/auth'
import { useDataStore } from '../../stores/data'

const auth = useAuthStore()
const data = useDataStore()
const loading = ref(false)

const wb = computed(() => data.workbench)

onMounted(async () => {
  loading.value = true
  try {
    await data.loadWorkbench()
  } catch (e) {
    ElMessage.error(e.message || '加载工作台失败')
  } finally {
    loading.value = false
  }
})

/** 近 7 天排课量趋势 */
const trendOption = computed(() => {
  const t = chartTheme()
  const list = wb.value?.scheduleTrend ?? []
  return {
    grid: { left: 8, right: 12, top: 20, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: list.map((d) => String(d.date).slice(5)),
      boundaryGap: false,
      ...t.axisStyle,
      splitLine: { show: false },
    },
    yAxis: { type: 'value', minInterval: 1, ...t.axisStyle },
    series: [
      {
        name: '排课量',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: list.map((d) => d.count),
        itemStyle: { color: t.accent },
        lineStyle: { width: 2, color: t.accent },
        areaStyle: {
          color: t.areaGradient('rgba(43,82,212,0.16)'),
        },
      },
    ],
  }
})
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="教练端"
      :title="`${auth.displayName}教练，工作概览`"
      :desc="wb?.coach ? `工号 ${wb.coach.code} · 准教车型 ${(wb.coach.licenseTypeList || []).join(' / ')}` : '本人工作数据概览'"
    >
      <template #actions>
        <RouterLink :to="{ name: 'coach-bookings' }">
          <el-button type="primary">处理预约</el-button>
        </RouterLink>
      </template>
    </PageHeader>

    <div class="grid grid--3">
      <StatCard label="今日排课" :value="wb?.todayCount ?? 0" unit="场" />
      <StatCard label="待确认预约" :value="wb?.pendingBookings ?? 0" unit="条" />
      <StatCard label="带教学员" :value="wb?.studentCount ?? 0" unit="人" />
    </div>

    <div class="grid grid--sidebar">
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">今日排课</span>
          <span class="panel__hint">{{ wb?.todayDate }}</span>
        </div>

        <el-skeleton v-if="loading" :rows="2" animated />
        <el-empty v-else-if="(wb?.todaySchedules ?? []).length === 0" description="今日暂无排课" />

        <div v-else class="u-stack">
          <div v-for="s in wb.todaySchedules" :key="s.id" class="slot-row">
            <span class="slot-row__time">{{ timeRange(s.startTime, s.endTime) }}</span>
            <span class="u-dim">已约 {{ s.bookedCount }}/{{ s.capacity }}</span>
            <span class="chip" :class="statusTone(s.status)">{{ s.status }}</span>
          </div>
        </div>
      </div>

      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">近 7 天排课量</span>
        </div>
        <BaseChart :option="trendOption" height="210px" />
      </div>
    </div>

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">待确认预约</span>
        <RouterLink :to="{ name: 'coach-bookings' }" class="link">前往处理 →</RouterLink>
      </div>

      <el-empty v-if="(wb?.pendingList ?? []).length === 0" description="暂无待确认预约" />

      <el-table v-else :data="wb.pendingList" stripe>
        <el-table-column prop="studentName" label="学员" width="120" />
        <el-table-column prop="licenseType" label="准驾车型" width="110" />
        <el-table-column prop="scheduleDate" label="培训日期" width="130" />
        <el-table-column label="时段" width="150">
          <template #default="{ row }">{{ timeRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.slot-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 13px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  background: var(--bg-sunken);
}

.slot-row__time {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.link {
  font-size: 12px;
  color: var(--text-secondary);
  text-decoration: none;
}

.link:hover {
  color: var(--text-primary);
}
</style>
