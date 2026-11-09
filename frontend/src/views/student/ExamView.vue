<script setup>
/**
 * F04 我的考试与成绩（学员端）
 * ---------------------------------------------------------------------------
 * 学员端只读：查看各科目考试成绩。
 * 科目考试由学员本人通过交管系统（交管 12123 等）预约报考，本系统不提供报名入口，
 * 也不负责考场安排；成绩由交管系统同步回来后在这里展示。
 * 数据：GET /api/exam-scores/my
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import ErrorState from '../../components/ErrorState.vue'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { SUBJECTS, EXAM_STATUS, subjectShort } from '../../data/demo'

const data = useDataStore()
const loading = ref(false)
const failed = ref(false)

/** 我的考试记录 */
const rows = computed(() => data.examRecords)

/* ------------------------------ 筛选 ------------------------------ */
const subjectCode = ref('')
const status = ref('')

const filteredRows = computed(() =>
  rows.value.filter(
    (r) =>
      (!subjectCode.value || r.subjectCode === subjectCode.value) &&
      (!status.value || r.status === status.value),
  ),
)

/* ------------------------------ 汇总 ------------------------------ */
const passedSubjects = computed(
  () => new Set(rows.value.filter((r) => r.status === '合格').map((r) => r.subjectCode)).size,
)
const pendingCount = computed(
  () => rows.value.filter((r) => r.status === '待考' || r.status === '补考中').length,
)
const failedCount = computed(() => rows.value.filter((r) => r.status === '不合格').length)

async function reload() {
  loading.value = true
  failed.value = false
  try {
    await data.loadMyExamRecords()
  } catch (e) {
    failed.value = true
    ElMessage.error(e.message || '加载考试成绩失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

function resetFilter() {
  subjectCode.value = ''
  status.value = ''
}

function attemptText(attemptNo) {
  const n = Number(attemptNo ?? 1)
  return n <= 1 ? '初考' : `补考${n - 1}`
}
</script>

<template>
  <section class="page page--student">
    <PageHeader
      eyebrow="学员端"
      title="我的考试与成绩"
      desc="科目考试请在交管系统（交管 12123 等）预约报考，本页只展示同步回来的考试成绩。"
    />

    <ErrorState v-if="failed" message="考试成绩加载失败，请检查后端服务后重试" @retry="reload" />

    <template v-else>
      <!-- 成绩汇总 -->
      <div class="exam-stats">
        <div class="glass exam-stat">
          <span class="exam-stat__k">已通过科目</span>
          <span class="exam-stat__v">{{ passedSubjects }}<small>/ 4</small></span>
        </div>
        <div class="glass exam-stat">
          <span class="exam-stat__k">待考 / 补考中</span>
          <span class="exam-stat__v">{{ pendingCount }}<small>场</small></span>
        </div>
        <div class="glass exam-stat">
          <span class="exam-stat__k">不合格记录</span>
          <span class="exam-stat__v" :class="{ 'exam-stat__v--danger': failedCount > 0 }">
            {{ failedCount }}<small>条</small>
          </span>
        </div>
      </div>

      <!-- 成绩明细 -->
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">成绩明细</span>
          <span class="panel__hint">共 {{ filteredRows.length }} 条记录</span>
        </div>

        <div class="query">
          <el-select
            v-model="subjectCode"
            placeholder="全部科目"
            clearable
            style="width: 190px"
          >
            <el-option
              v-for="s in SUBJECTS"
              :key="s.code"
              :label="s.short"
              :value="s.code"
            />
          </el-select>
          <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="s in EXAM_STATUS" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button plain @click="resetFilter">重置</el-button>
        </div>

        <el-skeleton v-if="loading" :rows="4" animated />
        <el-empty
          v-else-if="filteredRows.length === 0"
          description="暂无可显示的成绩，考试请在交管系统报考，成绩同步后会自动出现在这里"
        />

        <el-table v-else :data="filteredRows" stripe>
          <el-table-column label="科目" width="140">
            <template #default="{ row }">
              {{ row.subjectName || subjectShort(row.subjectCode) }}
            </template>
          </el-table-column>
          <el-table-column label="考试日期" width="130">
            <template #default="{ row }">{{ row.examDate ?? row.planDate ?? '待同步' }}</template>
          </el-table-column>
          <el-table-column label="次数" width="100">
            <template #default="{ row }">{{ attemptText(row.attemptNo) }}</template>
          </el-table-column>
          <el-table-column label="成绩" width="90">
            <template #default="{ row }">{{ row.score ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              {{ row.status === '不合格' ? '请在交管系统重新预约补考' : '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </section>
</template>

<style scoped>
.exam-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.exam-stat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px 24px;
}

.exam-stat__k {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.exam-stat__v {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-primary);
}

.exam-stat__v small {
  margin-left: 6px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.exam-stat__v--danger {
  color: var(--state-danger);
}

.query {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

@media (max-width: 640px) {
  .exam-stats {
    grid-template-columns: 1fr;
  }
}
</style>
