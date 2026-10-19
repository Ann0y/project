<script setup>
/**
 * F13 培训与约车管理（管理端）
 * ---------------------------------------------------------------------------
 * 需求 F13：预约的确认 / 取消 / 爽约处理；学时记录查询与审核（通过 / 驳回）；
 *          培训课程 / 科目学时配置。
 * 接口：/api/bookings、/api/hours、/api/courses
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { shortTime } from '../../utils/format'
import { useDataStore } from '../../stores/data'
import { AUDIT_STATUS, BOOKING_STATUS } from '../../data/demo'

const data = useDataStore()
const tab = ref('booking')
const loading = ref(false)

/* ------------------------------ 预约管理 ------------------------------ */
const bookingStatus = ref('')
const bookingRows = computed(() => data.bookingPage.list ?? [])
const bookingTotal = computed(() => data.bookingPage.total ?? 0)
const bookingPage = ref(1)

/* ------------------------------ 学时审核 ------------------------------ */
const hourStatus = ref('待审核')
const hourRows = computed(() => data.hoursPage.list ?? [])
const hourTotal = computed(() => data.hoursPage.total ?? 0)
const hourPage = ref(1)

/* ------------------------------ 科目配置 ------------------------------ */
const courseRows = computed(() => data.courses)

async function loadBookings() {
  loading.value = true
  try {
    await data.loadAdminBookings({
      status: bookingStatus.value || undefined,
      page: bookingPage.value,
      size: 10,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载预约失败')
  } finally {
    loading.value = false
  }
}

async function loadHours() {
  loading.value = true
  try {
    await data.loadAdminHours({
      auditStatus: hourStatus.value || undefined,
      page: hourPage.value,
      size: 10,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载学时失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadBookings(), loadHours(), data.loadCourses().catch(() => {})])
})

async function changeBookingStatus(row, next) {
  try {
    await ElMessageBox.confirm(
      `学员：${row.studentName}　时段：${row.scheduleDate} ${row.startTime}`,
      `确认将预约置为「${next}」？`,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  const result = await data.changeAdminBookingStatus(row.id, next)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(`预约已置为「${next}」`)
  await loadBookings()
}

async function audit(row, pass) {
  if (pass) {
    try {
      await ElMessageBox.confirm(
        `${row.studentName} · ${row.subjectName} · ${row.hours} 学时`,
        '审核通过该学时记录？',
        { confirmButtonText: '通过', cancelButtonText: '取消', type: 'info' },
      )
    } catch {
      return
    }
    const result = await data.auditHoursRecord(row.id, true)
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success('已通过，学时计入学员进度')
    await loadHours()
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回学时记录', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空'),
    })
    const result = await data.auditHoursRecord(row.id, false, value)
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.warning('已驳回该学时记录')
    await loadHours()
  } catch {
    /* 取消 */
  }
}

const pendingCount = computed(
  () => (data.hoursPage.list ?? []).filter((r) => r.auditStatus === '待审核').length,
)
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="培训与约车管理"
      desc="预约处理、学时审核与科目学时配置。"
    />

    <div class="glass panel">
      <el-tabs v-model="tab">
        <el-tab-pane label="预约管理" name="booking">
          <div class="panel__head">
            <span class="panel__hint">全量预约列表（共 {{ bookingTotal }} 条）</span>
            <el-select
              v-model="bookingStatus"
              placeholder="全部状态"
              clearable
              size="small"
              style="width: 140px"
              @change="loadBookings"
            >
              <el-option v-for="s in BOOKING_STATUS" :key="s" :label="s" :value="s" />
            </el-select>
          </div>

          <el-table v-loading="loading" :data="bookingRows" stripe>
            <el-table-column prop="studentName" label="学员" width="100" />
            <el-table-column prop="coachName" label="教练" width="100" />
            <el-table-column label="车辆" width="130">
              <template #default="{ row }">{{ row.vehiclePlate ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="培训时间" width="190">
              <template #default="{ row }">{{ row.scheduleDate }} {{ shortTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="200">
              <template #default="{ row }">
                <template v-if="row.status === '待确认'">
                  <el-button link type="primary" size="small" @click="changeBookingStatus(row, '已确认')">
                    确认
                  </el-button>
                  <el-button link type="danger" size="small" @click="changeBookingStatus(row, '已取消')">
                    取消
                  </el-button>
                  <el-button link size="small" @click="changeBookingStatus(row, '爽约')">爽约</el-button>
                </template>
                <el-button
                  v-else-if="row.status === '已确认'"
                  link
                  type="primary"
                  size="small"
                  @click="changeBookingStatus(row, '已完成')"
                >
                  完成
                </el-button>
                <span v-else class="u-dim">-</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="bookingPage"
              :total="bookingTotal"
              :page-size="10"
              layout="total, prev, pager, next"
              background
              @current-change="loadBookings"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="学时审核" name="hours">
          <div class="panel__head">
            <span class="panel__hint">审核通过后学时计入学员进度，驳回需说明原因</span>
            <el-select
              v-model="hourStatus"
              placeholder="全部状态"
              clearable
              size="small"
              style="width: 140px"
              @change="loadHours"
            >
              <el-option v-for="s in AUDIT_STATUS" :key="s" :label="s" :value="s" />
            </el-select>
          </div>

          <el-table v-loading="loading" :data="hourRows" stripe>
            <el-table-column prop="studentName" label="学员" width="100" />
            <el-table-column prop="coachName" label="登记教练" width="110" />
            <el-table-column prop="subjectName" label="科目" width="220" />
            <el-table-column label="学时" width="80">
              <template #default="{ row }">{{ row.hours }}</template>
            </el-table-column>
            <el-table-column prop="trainDate" label="培训日期" width="130" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <span class="chip" :class="statusTone(row.auditStatus)">{{ row.auditStatus }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="150">
              <template #default="{ row }">
                <template v-if="row.auditStatus === '待审核'">
                  <el-button link type="primary" size="small" @click="audit(row, true)">通过</el-button>
                  <el-button link type="danger" size="small" @click="audit(row, false)">驳回</el-button>
                </template>
                <span v-else class="u-dim">{{ row.auditOpinion || '已处理' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="hourPage"
              :total="hourTotal"
              :page-size="10"
              layout="total, prev, pager, next"
              background
              @current-change="loadHours"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="科目学时配置" name="course">
          <div class="panel__head">
            <span class="panel__hint">按《机动车驾驶培训教学大纲》配置各准驾车型学时</span>
          </div>

          <el-table :data="courseRows" stripe>
            <el-table-column prop="licenseType" label="准驾车型" width="110" />
            <el-table-column label="科目代码" width="100">
              <template #default="{ row }">{{ row.subjectCode }}</template>
            </el-table-column>
            <el-table-column prop="subjectName" label="科目名称" min-width="240" />
            <el-table-column label="大纲要求学时" width="130">
              <template #default="{ row }">{{ row.requiredHours }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<style scoped>
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
