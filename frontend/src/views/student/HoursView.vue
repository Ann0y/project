<script setup>
/**
 * 我的学时（学员端）
 * ---------------------------------------------------------------------------
 * 数据：GET /api/hours/my
 * 展示每次培训登记的学时、审核状态与驳回原因，让学员能看懂
 * 「为什么这次培训没有计入进度」，补齐学时审核闭环。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import ErrorState from '../../components/ErrorState.vue'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { SUBJECTS, AUDIT_STATUS, subjectShort } from '../../data/demo'

const data = useDataStore()
const failed = ref(false)
const loading = ref(false)

const subjectCode = ref('')
const auditStatus = ref('')
const page = ref(1)
const size = 10

const allRows = computed(() => data.myHoursRecords)

/** 客户端筛选：顶部统计始终基于全部记录，不受筛选影响 */
const filteredRows = computed(() =>
  allRows.value.filter(
    (r) =>
      (!subjectCode.value || r.subjectCode === subjectCode.value) &&
      (!auditStatus.value || r.auditStatus === auditStatus.value),
  ),
)

const pagedRows = computed(() => {
  const start = (page.value - 1) * size
  return filteredRows.value.slice(start, start + size)
})

const stats = computed(() => {
  const sum = (rows) => rows.reduce((acc, r) => acc + Number(r.hours ?? 0), 0)
  const approved = allRows.value.filter((r) => r.auditStatus === '已通过')
  const pending = allRows.value.filter((r) => r.auditStatus === '待审核')
  const rejected = allRows.value.filter((r) => r.auditStatus === '已驳回')
  return {
    approvedHours: sum(approved),
    pendingHours: sum(pending),
    pendingCount: pending.length,
    rejectedCount: rejected.length,
  }
})

async function load() {
  loading.value = true
  failed.value = false
  try {
    await data.loadMyHours()
  } catch (e) {
    failed.value = true
    ElMessage.error(e.message || '加载学时明细失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onFilter() {
  page.value = 1
}

function resetFilter() {
  subjectCode.value = ''
  auditStatus.value = ''
  page.value = 1
}

function hoursText(v) {
  const n = Number(v ?? 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
</script>

<template>
  <section class="page page--student">
    <PageHeader
      eyebrow="学员端"
      title="我的学时"
      desc="查看每次培训登记的学时与审核结果，被驳回的记录会显示审核意见。"
    />

    <ErrorState v-if="failed" message="学时明细加载失败，请检查后端服务后重试" @retry="load" />

    <template v-else>
      <!-- 汇总 -->
      <div class="hour-stats">
        <div class="glass hour-stat">
          <span class="hour-stat__k">已通过学时</span>
          <span class="hour-stat__v">{{ hoursText(stats.approvedHours) }}<small>学时</small></span>
        </div>
        <div class="glass hour-stat">
          <span class="hour-stat__k">待审核</span>
          <span class="hour-stat__v">{{ stats.pendingCount }}<small>条 · {{ hoursText(stats.pendingHours) }} 学时</small></span>
        </div>
        <div class="glass hour-stat">
          <span class="hour-stat__k">已驳回</span>
          <span class="hour-stat__v" :class="{ 'hour-stat__v--danger': stats.rejectedCount > 0 }">
            {{ stats.rejectedCount }}<small>条</small>
          </span>
        </div>
      </div>

      <!-- 明细 -->
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">学时记录</span>
          <span class="panel__hint">共 {{ filteredRows.length }} 条</span>
        </div>

        <div class="query">
          <el-select
            v-model="subjectCode"
            placeholder="全部科目"
            clearable
            style="width: 190px"
            @change="onFilter"
          >
            <el-option
              v-for="s in SUBJECTS"
              :key="s.code"
              :label="s.short"
              :value="s.code"
            />
          </el-select>
          <el-select
            v-model="auditStatus"
            placeholder="全部审核状态"
            clearable
            style="width: 160px"
            @change="onFilter"
          >
            <el-option v-for="s in AUDIT_STATUS" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button plain @click="resetFilter">重置</el-button>
        </div>

        <el-skeleton v-if="loading" :rows="4" animated />
        <el-empty v-else-if="filteredRows.length === 0" description="暂无学时记录" />

        <template v-else>
          <el-table :data="pagedRows" stripe>
            <el-table-column prop="trainDate" label="培训日期" width="120" />
            <el-table-column label="科目" width="150">
              <template #default="{ row }">
                {{ row.subjectName || subjectShort(row.subjectCode) }}
              </template>
            </el-table-column>
            <el-table-column prop="coachName" label="登记教练" width="110">
              <template #default="{ row }">{{ row.coachName || '-' }}</template>
            </el-table-column>
            <el-table-column label="学时" width="90">
              <template #default="{ row }">{{ hoursText(row.hours) }}</template>
            </el-table-column>
            <el-table-column label="审核状态" width="110">
              <template #default="{ row }">
                <span class="chip" :class="statusTone(row.auditStatus)">{{ row.auditStatus }}</span>
              </template>
            </el-table-column>
            <el-table-column label="审核意见" min-width="220">
              <template #default="{ row }">
                <span v-if="row.auditOpinion" :class="{ 'u-danger': row.auditStatus === '已驳回' }">
                  {{ row.auditOpinion }}
                </span>
                <span v-else class="u-dim">-</span>
              </template>
            </el-table-column>
            <el-table-column label="审核时间" width="170">
              <template #default="{ row }">
                <span class="u-dim">{{ row.auditTime || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="page"
              :page-size="size"
              :total="filteredRows.length"
              layout="prev, pager, next"
              background
            />
          </div>
        </template>
      </div>
    </template>
  </section>
</template>

<style scoped>
.hour-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.hour-stat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px 24px;
}

.hour-stat__k {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.hour-stat__v {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
}

.hour-stat__v small {
  margin-left: 6px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.hour-stat__v--danger {
  color: var(--state-danger);
}

.u-danger {
  color: var(--state-danger);
}

.query {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 640px) {
  .hour-stats {
    grid-template-columns: 1fr;
  }
}
</style>
