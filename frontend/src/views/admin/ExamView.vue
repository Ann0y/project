<script setup>
/**
 * F14 考试管理（管理端）
 * ---------------------------------------------------------------------------
 * 需求 F14：考试计划发布；成绩录入与合格状态管理；补考记录维护。
 * 接口：/api/exam-plans、/api/exam-scores
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import BaseChart from '../../components/BaseChart.vue'
import { chartTheme } from '../../utils/chart'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { EXAM_STATUS, PLAN_STATUS, SUBJECTS, subjectShort } from '../../data/demo'

const data = useDataStore()
const tab = ref('plan')
const loading = ref(false)

const planRows = computed(() => data.examPlans)
const scoreRows = computed(() => data.examScorePage.list ?? [])
const scoreTotal = computed(() => data.examScorePage.total ?? 0)
const scorePage = ref(1)
const statusFilter = ref('')

async function reload() {
  loading.value = true
  try {
    await Promise.all([
      data.loadExamPlans(),
      data.loadExamScores({ status: statusFilter.value || undefined, page: scorePage.value, size: 10 }),
    ])
  } catch (e) {
    ElMessage.error(e.message || '加载考试数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

/* ------------------------------ 考试计划 ------------------------------ */
const planDialog = ref(false)
const editingPlanId = ref(null)
const planFormRef = ref()
const planForm = reactive({
  subjectCode: '1',
  examDate: new Date().toISOString().slice(0, 10),
  examTime: '09:00',
  venue: '',
  capacity: 40,
  status: '报名中',
})

const planRules = {
  subjectCode: [{ required: true, message: '请选择科目', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  venue: [{ required: true, message: '请输入考场', trigger: 'blur' }],
}

function openPlanCreate() {
  editingPlanId.value = null
  Object.assign(planForm, {
    subjectCode: '1',
    examDate: new Date().toISOString().slice(0, 10),
    examTime: '09:00',
    venue: '',
    capacity: 40,
    status: '报名中',
  })
  planDialog.value = true
}

function openPlanEdit(row) {
  editingPlanId.value = row.id
  Object.assign(planForm, {
    subjectCode: row.subjectCode,
    examDate: row.examDate,
    examTime: String(row.examTime ?? '09:00').slice(0, 5),
    venue: row.venue,
    capacity: row.capacity,
    status: row.status,
  })
  planDialog.value = true
}

function onPlanSubmit() {
  if (!planFormRef.value) return
  planFormRef.value.validate(async (valid) => {
    if (!valid) return
    const result = await data.saveExamPlan({ id: editingPlanId.value, ...planForm })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(editingPlanId.value ? '考试计划已更新' : '考试计划已发布')
    planDialog.value = false
    await reload()
  })
}

async function changePlanStatus(row, status) {
  const result = await data.changeExamPlanStatus(row.id, status)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(`计划状态已置为「${status}」`)
  await reload()
}

async function confirmRoster(row) {
  const result = await data.confirmRoster(row.id)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success('已确认名单，该场次截止报名')
  await reload()
}

/* ------------------------------ 成绩录入 ------------------------------ */
const scoreDialog = ref(false)
const scoreTarget = ref(null)
const scoreForm = reactive({ score: null })

function openScore(row) {
  scoreTarget.value = row
  scoreForm.score = row.score
  scoreDialog.value = true
}

async function onScoreSubmit() {
  const score = Number(scoreForm.score)
  if (Number.isNaN(score) || score < 0 || score > 100) {
    ElMessage.error('请输入 0-100 之间的成绩')
    return
  }
  const result = await data.saveExamScore(scoreTarget.value.id, score)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(score >= 90 ? '成绩已录入，判定为合格' : `成绩 ${score} 分不合格，已生成补考记录`)
  scoreDialog.value = false
  await reload()
}

/* ------------------------------ 通过率图表 ------------------------------ */
const passOption = computed(() => {
  const t = chartTheme()
  const list = SUBJECTS.map((s) => {
    const scored = scoreRows.value.filter((r) => r.subjectCode === s.code && r.score != null)
    return scored.length
      ? Math.round((scored.filter((r) => r.status === '合格').length / scored.length) * 100)
      : 0
  })
  return {
    grid: { left: 8, right: 14, top: 20, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${v}%` },
    xAxis: {
      type: 'category',
      data: SUBJECTS.map((s) => subjectShort(s.code)),
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
        type: 'bar',
        barWidth: 26,
        itemStyle: { color: t.accent, borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top', color: t.label, fontSize: 11, formatter: '{c}%' },
        data: list,
      },
    ],
  }
})
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="考试管理"
      desc="考试计划发布、名单确认、成绩录入与补考记录维护。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openPlanCreate">发布考试计划</el-button>
      </template>
    </PageHeader>

    <div class="grid grid--sidebar">
      <div class="glass panel">
        <el-tabs v-model="tab">
          <el-tab-pane label="考试计划" name="plan">
            <el-table v-loading="loading" :data="planRows" stripe>
              <el-table-column label="科目" width="90">
                <template #default="{ row }">{{ subjectShort(row.subjectCode) }}</template>
              </el-table-column>
              <el-table-column prop="examDate" label="考试日期" width="125" />
              <el-table-column label="时间" width="90">
                <template #default="{ row }">{{ String(row.examTime ?? '').slice(0, 5) }}</template>
              </el-table-column>
              <el-table-column prop="venue" label="考场" min-width="150" />
              <el-table-column label="报名 / 容量" width="120">
                <template #default="{ row }">{{ row.enrolledCount }}/{{ row.capacity }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openPlanEdit(row)">编辑</el-button>
                  <el-button link size="small" @click="confirmRoster(row)">确认名单</el-button>
                  <el-button
                    v-if="row.status === '报名中'"
                    link
                    size="small"
                    @click="changePlanStatus(row, '已截止')"
                  >
                    截止
                  </el-button>
                  <el-button
                    v-else-if="row.status === '已截止'"
                    link
                    size="small"
                    @click="changePlanStatus(row, '已完成')"
                  >
                    完成
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="报名与成绩" name="record">
            <div class="panel__head">
              <span class="panel__hint">录入成绩后自动判定合格，不合格自动生成补考记录</span>
              <el-select
                v-model="statusFilter"
                placeholder="全部状态"
                clearable
                size="small"
                style="width: 130px"
                @change="reload"
              >
                <el-option v-for="s in EXAM_STATUS" :key="s" :label="s" :value="s" />
              </el-select>
            </div>

            <el-table v-loading="loading" :data="scoreRows" stripe>
              <el-table-column prop="studentName" label="学员" width="100" />
              <el-table-column prop="subjectCode" label="科目" width="80">
                <template #default="{ row }">{{ subjectShort(row.subjectCode) }}</template>
              </el-table-column>
              <el-table-column prop="examDate" label="考试日期" width="125">
                <template #default="{ row }">{{ row.examDate ?? '待安排' }}</template>
              </el-table-column>
              <el-table-column label="次数" width="90">
                <template #default="{ row }">
                  {{ row.attemptNo === 1 ? '初考' : `补考${row.attemptNo - 1}` }}
                </template>
              </el-table-column>
              <el-table-column label="成绩" width="80">
                <template #default="{ row }">{{ row.score ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="120">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openScore(row)">
                    {{ row.score == null ? '录入成绩' : '修改成绩' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager">
              <el-pagination
                v-model:current-page="scorePage"
                :total="scoreTotal"
                :page-size="10"
                layout="total, prev, pager, next"
                background
                @current-change="reload"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">各科目通过率</span>
          <span class="panel__hint">合格线 90 分</span>
        </div>
        <BaseChart :option="passOption" height="240px" />
      </div>
    </div>

    <!-- 计划弹窗 -->
    <el-dialog
      v-model="planDialog"
      :title="editingPlanId ? '编辑考试计划' : '发布考试计划'"
      width="440px"
    >
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-position="top">
        <el-form-item label="科目" prop="subjectCode">
          <el-select v-model="planForm.subjectCode" class="u-full">
            <el-option v-for="s in SUBJECTS" :key="s.code" :label="s.name" :value="s.code" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="考试日期" prop="examDate">
            <el-date-picker
              v-model="planForm.examDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="u-full"
            />
          </el-form-item>
          <el-form-item label="考试时间">
            <el-time-select
              v-model="planForm.examTime"
              start="06:00"
              step="00:30"
              end="20:00"
              class="u-full"
            />
          </el-form-item>
        </div>
        <el-form-item label="考场" prop="venue">
          <el-input v-model="planForm.venue" placeholder="如 第一理论考场" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="容量">
            <el-input-number v-model="planForm.capacity" :min="1" :max="200" class="u-full" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="planForm.status" class="u-full">
              <el-option v-for="s in PLAN_STATUS" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="planDialog = false">取消</el-button>
        <el-button type="primary" @click="onPlanSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 成绩录入 -->
    <el-dialog v-model="scoreDialog" title="录入考试成绩" width="380px">
      <el-form label-position="top">
        <el-form-item label="学员">
          <el-input :model-value="scoreTarget?.studentName" disabled />
        </el-form-item>
        <el-form-item label="科目">
          <el-input :model-value="subjectShort(scoreTarget?.subjectCode ?? '')" disabled />
        </el-form-item>
        <el-form-item label="成绩（0-100，90 分及以上合格）">
          <el-input-number v-model="scoreForm.score" :min="0" :max="100" class="u-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreDialog = false">取消</el-button>
        <el-button type="primary" @click="onScoreSubmit">提交成绩</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
