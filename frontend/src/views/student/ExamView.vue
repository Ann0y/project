<script setup>
/**
 * F04 我的考试与成绩（学员端）
 * ---------------------------------------------------------------------------
 * 需求 F04：查看考试安排与报名状态，按科目查询成绩与补考记录。
 * 数据：GET /api/exam-scores/my、GET /api/exam-plans/open
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { shortTime } from '../../utils/format'
import { useDataStore } from '../../stores/data'
import { SUBJECTS, subjectShort } from '../../data/demo'

const data = useDataStore()
const loading = ref(false)
const registering = ref(false)

/** 我的考试记录 */
const rows = computed(() => data.examRecords)

/** 可报名的考试计划 */
const openPlans = computed(() => data.examPlans)

/** 报名条件：学时达标 + 上一科目已报名 + 本科目未合格 + 本场次未报名 */
function canRegister(plan) {
  return !plan.registered && !plan.passed && plan.qualified && plan.prerequisiteMet !== false
}

/** 按钮文案：把「不能报」的原因直接写在按钮上 */
function actionLabel(plan) {
  if (plan.registered) return '已报名'
  if (plan.passed) return '已合格'
  if (plan.prerequisiteMet === false) return `需先报名${plan.prerequisiteName ?? '上一科目'}`
  if (!plan.qualified) return '学时不足'
  return '报名'
}

async function reload() {
  loading.value = true
  try {
    await Promise.all([data.loadMyExamRecords(), data.loadOpenPlans()])
  } catch (e) {
    ElMessage.error(e.message || '加载考试数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

async function register(plan) {
  try {
    await ElMessageBox.confirm(
      `${plan.examDate} ${shortTime(plan.examTime)} · ${plan.venue}`,
      '确认报名该场考试？',
      { confirmButtonText: '确认报名', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }

  registering.value = true
  const result = await data.registerExam(plan.id)
  registering.value = false

  if (!result.ok) {
    ElMessage.warning(result.reason)
    return
  }
  ElMessage.success('考试报名已提交')
  await reload()
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="学员端"
      title="我的考试与成绩"
      desc="查看考试安排与报名状态，以及各科目成绩与补考记录。"
    />

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">考试安排</span>
        <span class="panel__hint">报名前校验学时是否达标，且需按科目顺序报名</span>
      </div>

      <el-skeleton v-if="loading" :rows="2" animated />
      <el-empty v-else-if="openPlans.length === 0" description="暂无可报名的考试计划" />

      <div v-else class="plan-grid">
        <div v-for="plan in openPlans" :key="plan.id" class="plan">
          <span class="plan__subject">{{ plan.subjectName || subjectShort(plan.subjectCode) }}</span>
          <span class="plan__venue">{{ plan.venue }}</span>
          <span class="plan__time">{{ plan.examDate }} {{ shortTime(plan.examTime) }}</span>
          <span class="u-dim plan__quota">
            学时 {{ Number(plan.myHours ?? 0) }}/{{ Number(plan.requiredHours ?? 0) }} ·
            名额 {{ plan.enrolledCount }}/{{ plan.capacity }}
          </span>
          <span
            v-if="!plan.registered && !plan.passed && plan.prerequisiteMet === false"
            class="plan__tip"
          >
            需先有{{ plan.prerequisiteName }}的报名记录
          </span>
          <el-button
            size="small"
            :type="canRegister(plan) ? 'primary' : 'default'"
            :disabled="!canRegister(plan)"
            :loading="registering"
            @click="register(plan)"
          >
            {{ actionLabel(plan) }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">成绩明细</span>
      </div>

      <el-empty v-if="!loading && rows.length === 0" description="暂无考试成绩" />

      <el-table v-else :data="rows" stripe>
        <el-table-column label="科目" width="110">
          <template #default="{ row }">{{ row.subjectName || subjectShort(row.subjectCode) }}</template>
        </el-table-column>
        <el-table-column prop="venue" label="考场" min-width="160">
          <template #default="{ row }">{{ row.venue ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="考试日期" width="130">
          <template #default="{ row }">{{ row.examDate ?? row.planDate ?? '待安排' }}</template>
        </el-table-column>
        <el-table-column label="次数" width="90">
          <template #default="{ row }">
            {{ row.attemptNo === 1 ? '初考' : `补考${row.attemptNo - 1}` }}
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="90">
          <template #default="{ row }">{{ row.score ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.plan {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-base);
  background: var(--bg-sunken);
}

.plan__subject {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-primary);
}

.plan__venue {
  font-size: 12.5px;
  color: var(--text-secondary);
}

.plan__time {
  font-size: 12px;
  color: var(--text-primary);
}

.plan__quota {
  font-size: 11px;
}

.plan__tip {
  font-size: 11.5px;
  color: var(--state-pending);
}

/* 卡片高度由网格拉齐，把按钮压到底部，避免四张卡片按钮错位 */
.plan :deep(.el-button) {
  margin-top: auto;
}
</style>
