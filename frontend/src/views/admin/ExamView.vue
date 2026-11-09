<script setup>
/**
 * F14 考试与成绩（管理端 · 只读）
 * ---------------------------------------------------------------------------
 * 科目考试统一在交管系统（交管 12123 等）中报考，驾校不参与考试报名、
 * 考场编排和名单确认，因此管理端只做两件事：
 *   1. 查看各学员各科目的考试成绩；
 *   2. 从交管系统同步成绩（成绩回传后写入本系统）。
 * 接口：GET /api/exam-scores、GET /api/exam-plans、POST /api/exam-scores/sync
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Refresh, Upload } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { EXAM_STATUS, SUBJECTS, subjectShort } from '../../data/demo'

const data = useDataStore()
const loading = ref(false)

const scoreRows = computed(() => data.examScorePage.list ?? [])
const scoreTotal = computed(() => data.examScorePage.total ?? 0)
const scorePage = ref(1)
const statusFilter = ref('')

async function reload() {
  loading.value = true
  try {
    await data.loadExamScores({
      status: statusFilter.value || undefined,
      page: scorePage.value,
      size: 10,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载成绩数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

function attemptText(attemptNo) {
  const n = Number(attemptNo ?? 1)
  return n <= 1 ? '初考' : `补考${n - 1}`
}

/* ------------------------------ 成绩同步 ------------------------------ */
const syncDialog = ref(false)
const syncFormRef = ref()
const syncForm = reactive({
  studentName: '',
  subjectCode: '1',
  examDate: new Date().toISOString().slice(0, 10),
  score: null,
})

const syncRules = {
  studentName: [{ required: true, message: '请输入学员姓名', trigger: 'blur' }],
  subjectCode: [{ required: true, message: '请选择科目', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
}

function openSync() {
  Object.assign(syncForm, {
    studentName: '',
    subjectCode: '1',
    examDate: new Date().toISOString().slice(0, 10),
    score: null,
  })
  syncDialog.value = true
}

function onSyncSubmit() {
  if (!syncFormRef.value) return
  syncFormRef.value.validate(async (valid) => {
    if (!valid) return
    const result = await data.syncExamScore({
      studentName: syncForm.studentName.trim(),
      subjectCode: syncForm.subjectCode,
      examDate: syncForm.examDate,
      score: syncForm.score,
    })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(syncForm.score == null ? '已同步该场次的考试安排' : '成绩同步成功')
    syncDialog.value = false
    await reload()
  })
}

/* ------------------------------ 批量同步 ------------------------------ */
const batchDialog = ref(false)
const batchText = ref('')
const batchParsing = ref(false)
const batchResult = ref(null)

/** 模板：字段说明 + 一条样例，方便对接方照着拼 */
const BATCH_SAMPLE = `[
  { "studentId": 1, "subjectCode": "1", "examDate": "2026-11-09", "score": 95 },
  { "studentName": "王芳", "subjectCode": "2", "examDate": "2026-11-09", "score": 88 }
]`

function openBatch() {
  batchText.value = ''
  batchResult.value = null
  batchDialog.value = true
}

function fillSample() {
  batchText.value = BATCH_SAMPLE
  batchResult.value = null
}

/** 本地先解析一遍，避免把明显不是 JSON 的内容发给后端 */
function parseBatchPayload() {
  const raw = batchText.value.trim()
  if (!raw) {
    ElMessage.error('请先粘贴成绩 JSON')
    return null
  }
  let parsed
  try {
    parsed = JSON.parse(raw)
  } catch (e) {
    ElMessage.error(`JSON 解析失败：${e.message}`)
    return null
  }
  const items = Array.isArray(parsed) ? parsed : parsed?.items
  if (!Array.isArray(items) || items.length === 0) {
    ElMessage.error('内容应为成绩数组，或用 { "items": [...] } 包裹')
    return null
  }
  return items
}

async function onBatchSubmit() {
  const items = parseBatchPayload()
  if (!items) return
  batchParsing.value = true
  batchResult.value = null
  try {
    const result = await data.syncExamScoresBatch(items)
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    batchResult.value = result.data
    const { created = 0, updated = 0, skipped = 0 } = result.data ?? {}
    if (skipped > 0) {
      ElMessage.warning(`同步完成：新增 ${created} 条，更新 ${updated} 条，跳过 ${skipped} 条`)
    } else {
      ElMessage.success(`同步完成：新增 ${created} 条，更新 ${updated} 条`)
    }
    await reload()
  } finally {
    batchParsing.value = false
  }
}

function onBatchFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    batchText.value = String(reader.result ?? '')
    batchResult.value = null
  }
  reader.onerror = () => ElMessage.error('读取文件失败')
  reader.readAsText(file, 'utf-8')
  event.target.value = ''
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="考试与成绩"
      desc="考试由学员在交管系统报考，本页只查看同步回来的成绩，不涉及考试报名与考场安排。"
    >
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
        <el-button :icon="DocumentCopy" @click="openBatch">批量同步</el-button>
        <el-button type="primary" :icon="Upload" @click="openSync">同步一条</el-button>
      </template>
    </PageHeader>

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__hint">
          成绩来源于交管系统同步，合格线 90 分；不合格由交管系统重新约考，本系统只记录结果
        </span>
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
        <el-table-column prop="studentName" label="学员" width="110" />
        <el-table-column label="科目" width="90">
          <template #default="{ row }">{{ subjectShort(row.subjectCode) }}</template>
        </el-table-column>
        <el-table-column label="考试日期" width="130">
          <template #default="{ row }">{{ row.examDate ?? '未安排' }}</template>
        </el-table-column>
        <el-table-column label="次数" width="90">
          <template #default="{ row }">{{ attemptText(row.attemptNo) }}</template>
        </el-table-column>
        <el-table-column label="成绩" width="90">
          <template #default="{ row }">
            <strong>{{ row.score ?? '-' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="同步时间" min-width="160">
          <template #default="{ row }">{{ row.registerTime ?? '-' }}</template>
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
    </div>

    <!-- 成绩同步 -->
    <el-dialog v-model="syncDialog" title="同步一条成绩" width="420px">
      <el-form ref="syncFormRef" :model="syncForm" :rules="syncRules" label-position="top">
        <el-form-item label="学员姓名" prop="studentName">
          <el-input v-model="syncForm.studentName" placeholder="请输入学员姓名" />
        </el-form-item>
        <el-form-item label="科目" prop="subjectCode">
          <el-select v-model="syncForm.subjectCode" class="u-full">
            <el-option v-for="s in SUBJECTS" :key="s.code" :label="s.name" :value="s.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试日期" prop="examDate">
          <el-date-picker
            v-model="syncForm.examDate"
            type="date"
            value-format="YYYY-MM-DD"
            class="u-full"
          />
        </el-form-item>
        <el-form-item label="成绩（0-100，留空表示仅同步考试安排）">
          <el-input-number v-model="syncForm.score" :min="0" :max="100" class="u-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="syncDialog = false">取消</el-button>
        <el-button type="primary" @click="onSyncSubmit">同步</el-button>
      </template>
    </el-dialog>

    <!-- 批量同步 -->
    <el-dialog v-model="batchDialog" title="批量同步交管系统成绩" width="640px">
      <div class="batch-hint">
        <p>
          粘贴数组即可，每条含
          <code>studentId</code>（或 <code>studentName</code>）、<code>subjectCode</code>（1~4）、
          <code>examDate</code>（yyyy-MM-dd）、<code>score</code>（0-100，留空表示仅同步考试安排）。
        </p>
        <p>同一学员同科目同日期已有记录时覆盖，不会重复；单条出错只跳过该条。</p>
      </div>

      <div class="batch-toolbar">
        <el-button size="small" @click="fillSample">填入样例</el-button>
        <label class="batch-file">
          <input type="file" accept=".json,application/json" @change="onBatchFile" />
          <span>从文件导入</span>
        </label>
      </div>

      <el-input
        v-model="batchText"
        type="textarea"
        :rows="10"
        placeholder='[ { "studentId": 1, "subjectCode": "1", "examDate": "2026-11-09", "score": 95 } ]'
        class="batch-input"
      />

      <div v-if="batchResult" class="batch-result">
        <div class="batch-result__head">
          共 {{ batchResult.total }} 条：新增 {{ batchResult.created }} 条，
          更新 {{ batchResult.updated }} 条，跳过 {{ batchResult.skipped }} 条
        </div>
        <el-table
          v-if="batchResult.errors?.length"
          :data="batchResult.errors"
          size="small"
          max-height="200"
        >
          <el-table-column prop="row" label="行号" width="70" />
          <el-table-column prop="reason" label="失败原因" min-width="320" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="batchDialog = false">关闭</el-button>
        <el-button type="primary" :loading="batchParsing" @click="onBatchSubmit">
          开始同步
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

/* 批量同步 */
.batch-hint {
  margin-bottom: 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-tertiary);
}

.batch-hint p {
  margin: 0 0 2px;
}

.batch-hint code {
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--bg-sunken);
  font-size: 11px;
  color: var(--text-secondary);
}

.batch-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.batch-file {
  display: inline-flex;
  align-items: center;
  padding: 5px 11px;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--glass-bg-strong);
  font-size: 12px;
  color: var(--text-primary);
  cursor: pointer;
  transition: border-color 0.2s var(--ease-out);
}

.batch-file:hover {
  border-color: var(--border-strong);
}

.batch-file input {
  display: none;
}

.batch-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
}

.batch-result {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-sunken);
}

.batch-result__head {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
</style>
