<script setup>
/**
 * F07 预约处理（教练端）
 * ---------------------------------------------------------------------------
 * 需求 F07：查看并确认 / 取消本人带教学员的预约。
 * 接口：GET /api/coach/bookings、PATCH /api/coach/bookings/{id}/status
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { timeRange } from '../../utils/format'
import { useDataStore } from '../../stores/data'
import { BOOKING_STATUS } from '../../data/demo'

const data = useDataStore()
const statusFilter = ref('')
const studentName = ref('')
const loading = ref(false)

const rows = computed(() => data.bookings)

async function reload() {
  loading.value = true
  try {
    await data.loadCoachBookings({
      status: statusFilter.value || undefined,
      studentName: studentName.value || undefined,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载预约失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

async function changeStatus(row, next) {
  const label = { 已确认: '确认预约', 已取消: '取消预约', 已完成: '标记完成', 爽约: '标记爽约' }[next]
  try {
    await ElMessageBox.confirm(
      `学员：${row.studentName}　时段：${row.scheduleDate} ${row.startTime}-${row.endTime}`,
      `${label}？`,
      { confirmButtonText: label, cancelButtonText: '返回', type: 'info' },
    )
  } catch {
    return
  }
  const result = await data.changeCoachBookingStatus(row.id, next)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(`预约已${label}`)
  await reload()
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="教练端"
      title="预约处理"
      desc="查看并确认 / 取消本人带教学员的预约。"
    />

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">预约记录</span>
        <div class="u-row">
          <el-input
            v-model="studentName"
            placeholder="按学员姓名"
            clearable
            size="small"
            style="width: 140px"
            @change="reload"
          />
          <el-select
            v-model="statusFilter"
            placeholder="全部状态"
            clearable
            size="small"
            style="width: 140px"
            @change="reload"
          >
            <el-option v-for="s in BOOKING_STATUS" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="3" animated />
      <el-empty v-else-if="rows.length === 0" description="没有符合条件的预约记录" />

      <el-table v-else :data="rows" stripe>
        <el-table-column prop="studentName" label="学员" width="110" />
        <el-table-column prop="licenseType" label="准驾车型" width="100" />
        <el-table-column prop="scheduleDate" label="培训日期" width="130" />
        <el-table-column label="时段" width="150">
          <template #default="{ row }">{{ timeRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="210">
          <template #default="{ row }">
            <template v-if="row.status === '待确认'">
              <el-button link type="primary" size="small" @click="changeStatus(row, '已确认')">确认</el-button>
              <el-button link type="danger" size="small" @click="changeStatus(row, '已取消')">取消</el-button>
              <el-button link size="small" @click="changeStatus(row, '爽约')">爽约</el-button>
            </template>
            <template v-else-if="row.status === '已确认'">
              <el-button link type="primary" size="small" @click="changeStatus(row, '已完成')">标记完成</el-button>
              <el-button link size="small" @click="changeStatus(row, '爽约')">爽约</el-button>
            </template>
            <span v-else class="u-dim">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
