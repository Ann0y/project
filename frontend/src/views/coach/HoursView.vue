<script setup>
/**
 * F08 学时登记（教练端）
 * ---------------------------------------------------------------------------
 * 需求 F08：为带教学员登记培训学时，提交后进入管理员审核。
 * 接口：GET/POST /api/coach/hours
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { SUBJECTS } from '../../data/demo'

const data = useDataStore()
const loading = ref(false)

const myStudents = computed(() => data.students)
const rows = computed(() => data.hoursRecords)

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  studentId: null,
  subjectCode: '2',
  hours: 2,
  trainDate: new Date().toISOString().slice(0, 10),
})

const rules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  subjectCode: [{ required: true, message: '请选择科目', trigger: 'change' }],
  trainDate: [{ required: true, message: '请选择培训日期', trigger: 'change' }],
}

async function reload() {
  loading.value = true
  try {
    await Promise.all([data.loadCoachStudents(), data.loadCoachHours()])
  } catch (e) {
    ElMessage.error(e.message || '加载学时数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

function openCreate() {
  Object.assign(form, {
    studentId: myStudents.value[0]?.id ?? null,
    subjectCode: '2',
    hours: 2,
    trainDate: new Date().toISOString().slice(0, 10),
  })
  dialogVisible.value = true
}

function onSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const result = await data.createHoursRecord({ ...form })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    dialogVisible.value = false
    ElMessage.success('学时已登记，等待管理员审核')
    await reload()
  })
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="教练端"
      title="学时登记"
      desc="为带教学员登记培训学时，提交后进入管理员审核。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">登记学时</el-button>
      </template>
    </PageHeader>

    <div class="glass panel">
      <el-skeleton v-if="loading" :rows="3" animated />
      <el-empty v-else-if="rows.length === 0" description="暂无学时记录" />

      <el-table v-else :data="rows" stripe>
        <el-table-column prop="studentName" label="学员" width="110" />
        <el-table-column prop="subjectName" label="科目" width="220" />
        <el-table-column label="本次学时" width="100">
          <template #default="{ row }">{{ row.hours }}</template>
        </el-table-column>
        <el-table-column prop="trainDate" label="培训日期" width="130" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.auditStatus)">{{ row.auditStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="200">
          <template #default="{ row }">{{ row.auditOpinion || '-' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="登记培训学时" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="学员" prop="studentId">
          <el-select v-model="form.studentId" class="u-full">
            <el-option
              v-for="s in myStudents"
              :key="s.id"
              :label="`${s.name}（${s.licenseType}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目" prop="subjectCode">
          <el-select v-model="form.subjectCode" class="u-full">
            <el-option v-for="s in SUBJECTS" :key="s.code" :label="s.name" :value="s.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="本次学时">
          <el-input-number v-model="form.hours" :min="0.5" :max="8" :step="0.5" class="u-full" />
        </el-form-item>
        <el-form-item label="培训日期" prop="trainDate">
          <el-date-picker
            v-model="form.trainDate"
            type="date"
            value-format="YYYY-MM-DD"
            class="u-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">提交审核</el-button>
      </template>
    </el-dialog>
  </section>
</template>
