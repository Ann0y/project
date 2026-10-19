<script setup>
/**
 * F09 我的学员（教练端）
 * ---------------------------------------------------------------------------
 * 需求 F09：查看带教学员的档案与培训进度。
 * 接口：GET /api/coach/students
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'

const data = useDataStore()
const loading = ref(false)

const rows = computed(() => data.students)

onMounted(async () => {
  loading.value = true
  try {
    await data.loadCoachStudents()
  } catch (e) {
    ElMessage.error(e.message || '加载学员失败')
  } finally {
    loading.value = false
  }
})

/** 各学员学时完成度对比 */
const barOption = computed(() => {
  const t = chartTheme()
  const list = [...rows.value].sort((a, b) => Number(a.doneHours) - Number(b.doneHours))
  const max = Math.max(1, ...list.map((s) => Number(s.requiredHours) || 0))
  return {
    grid: { left: 8, right: 46, top: 12, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      max,
      name: '学时',
      nameTextStyle: { color: t.label, fontSize: 11 },
      ...t.axisStyle,
    },
    yAxis: {
      type: 'category',
      data: list.map((s) => s.name),
      ...t.axisStyle,
      splitLine: { show: false },
    },
    series: [
      {
        type: 'bar',
        barWidth: 14,
        itemStyle: { color: t.accent, borderRadius: [0, 4, 4, 0] },
        label: {
          show: true,
          position: 'right',
          color: t.label,
          fontSize: 11,
          formatter: '{c}',
        },
        data: list.map((s) => Number(s.doneHours) || 0),
      },
    ],
  }
})
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="教练端"
      title="我的学员"
      desc="查看带教学员的档案与培训进度（固定带教关系）。"
    />

    <div class="grid grid--sidebar">
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">学员档案</span>
          <span class="panel__hint">共 {{ rows.length }} 人</span>
        </div>

        <el-skeleton v-if="loading" :rows="3" animated />
        <el-empty v-else-if="rows.length === 0" description="暂无带教学员" />

        <el-table v-else :data="rows" stripe>
          <el-table-column prop="name" label="姓名" width="100">
            <template #default="{ row }"><strong>{{ row.name }}</strong></template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="licenseType" label="车型" width="80" />
          <el-table-column label="车辆" width="130">
            <template #default="{ row }">{{ row.vehiclePlate ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="进度" width="130">
            <template #default="{ row }">
              {{ row.doneHours }} / {{ row.requiredHours }}（{{ row.percent }}%）
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">学员学时对比</span>
        </div>
        <BaseChart :option="barOption" height="260px" :empty="rows.length === 0" />
      </div>
    </div>
  </section>
</template>
