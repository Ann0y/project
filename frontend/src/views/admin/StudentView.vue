<script setup>
/**
 * F11 学员管理（管理端）
 * ---------------------------------------------------------------------------
 * 需求 F11：报名登记、档案维护、带教教练 / 车辆分配；
 *          多条件组合查询 + 分页；状态流转（在训 / 结业 / 退学）。
 * 接口：/api/students（分页查询在服务端完成）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { LICENSE_TYPES, STUDENT_STATUS } from '../../data/demo'

const data = useDataStore()
const loading = ref(false)

const query = reactive({ name: '', status: '', licenseType: '', coachId: null })
const page = ref(1)
const pageSize = ref(10)

const rows = computed(() => data.studentPage.list ?? [])
const total = computed(() => data.studentPage.total ?? 0)

async function load() {
  loading.value = true
  try {
    await data.loadStudents({
      name: query.name || undefined,
      status: query.status || undefined,
      licenseType: query.licenseType || undefined,
      coachId: query.coachId || undefined,
      page: page.value,
      size: pageSize.value,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载学员失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), data.loadCoaches().catch(() => {})])
})

function onSearch() {
  page.value = 1
  load()
}

function onReset() {
  Object.assign(query, { name: '', status: '', licenseType: '', coachId: null })
  onSearch()
}

/* --------------------------- 新增 / 编辑 --------------------------- */
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref()
const form = reactive({
  name: '',
  idCard: '',
  phone: '',
  licenseType: 'C1',
  enrollDate: new Date().toISOString().slice(0, 10),
  status: '待审核',
  coachId: null,
  vehicleId: null,
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

const coachVehicles = computed(() =>
  data.vehicles.filter((v) => v.coachId === form.coachId),
)

function openCreate() {
  editing.value = null
  Object.assign(form, {
    name: '',
    idCard: '',
    phone: '',
    licenseType: 'C1',
    enrollDate: new Date().toISOString().slice(0, 10),
    status: '待审核',
    coachId: data.coaches[0]?.id ?? null,
    vehicleId: null,
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    name: row.name,
    idCard: '',
    phone: row.phone,
    licenseType: row.licenseType,
    enrollDate: row.enrollDate,
    status: row.status,
    coachId: row.coachId,
    vehicleId: row.vehicleId,
  })
  dialogVisible.value = true
}

function onSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    // 编辑时若未重填身份证号，则不提交该字段（后端保留原密文）
    const payload = { ...form, id: editing.value?.id }
    if (editing.value && !payload.idCard) delete payload.idCard
    const result = await data.saveStudent(payload)
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(editing.value ? '学员档案已更新' : '学员报名登记成功')
    dialogVisible.value = false
    await load()
  })
}

/* ----------------------------- 状态流转 ----------------------------- */
async function changeStatus(row, next) {
  try {
    await ElMessageBox.confirm(`确定将 ${row.name} 的状态改为「${next}」吗？`, '状态流转', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: next === '退学' ? 'warning' : 'info',
    })
  } catch {
    return
  }
  const result = await data.changeStudentStatus(row.id, next)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(`状态已更新为「${next}」`)
  await load()
}

/* ----------------------------- 报名审核 ----------------------------- */
async function audit(row, pass) {
  if (pass) {
    try {
      await ElMessageBox.confirm('通过后学员状态置为「在训」。', `审核通过 ${row.name} 的报名？`, {
        confirmButtonText: '审核通过',
        cancelButtonText: '取消',
        type: 'info',
      })
    } catch {
      return
    }
    const result = await data.auditStudent(row.id, true, '')
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(`${row.name} 的报名已通过审核`)
    await load()
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', `驳回 ${row.name} 的报名`, {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '如：身份证信息不清晰',
      inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因'),
    })
    const result = await data.auditStudent(row.id, false, value)
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.warning(`已驳回：${value}`)
    await load()
  } catch {
    /* 取消 */
  }
}

/* -------------------------- 分配教练与车辆 -------------------------- */
const assignVisible = ref(false)
const assignTarget = ref(null)
const assignForm = reactive({ coachId: null, vehicleId: null })
const assignVehicles = computed(() =>
  data.vehicles.filter((v) => v.coachId === assignForm.coachId),
)

async function openAssign(row) {
  assignTarget.value = row
  assignForm.coachId = row.coachId
  assignForm.vehicleId = row.vehicleId
  // 车辆下拉需要全量车辆数据
  if (data.vehicles.length === 0) {
    try {
      await data.loadVehicles()
    } catch {
      /* 忽略 */
    }
  }
  assignVisible.value = true
}

async function onAssign() {
  const result = await data.assignCoachAndVehicle(
    assignTarget.value.id,
    assignForm.coachId,
    assignForm.vehicleId,
  )
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  assignVisible.value = false
  ElMessage.success('带教教练与车辆已分配（固定带教关系）')
  await load()
}

/* ------------------------------ 缴费登记 ------------------------------ */
async function registerPayment(row) {
  try {
    const { value } = await ElMessageBox.prompt(`为 ${row.name} 登记缴费金额`, '缴费登记', {
      confirmButtonText: '确认登记',
      cancelButtonText: '取消',
      inputValue: '5800',
      inputPattern: /^\d+(\.\d{1,2})?$/,
      inputErrorMessage: '请输入正确的金额',
    })
    const result = await data.registerPayment({
      studentId: row.id,
      item: '培训费',
      amount: Number(value),
      payMethod: '现金',
    })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(`已登记缴费 ¥${value}`)
    await load()
  } catch {
    /* 取消 */
  }
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="学员管理"
      desc="报名登记、档案维护、带教教练与车辆分配，支持多条件组合查询与分页。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">报名登记</el-button>
      </template>
    </PageHeader>

    <div class="glass panel">
      <div class="query">
        <el-input v-model="query.name" placeholder="姓名" clearable style="width: 140px" />
        <el-select v-model="query.status" placeholder="报名状态" clearable style="width: 140px">
          <el-option v-for="s in STUDENT_STATUS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.licenseType" placeholder="准驾车型" clearable style="width: 130px">
          <el-option v-for="t in LICENSE_TYPES" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="query.coachId" placeholder="带教教练" clearable style="width: 140px">
          <el-option v-for="c in data.coaches" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button plain @click="onReset">重置</el-button>
      </div>
    </div>

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">学员档案</span>
        <span class="panel__hint">共 {{ total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" label="姓名" width="100">
          <template #default="{ row }"><strong>{{ row.name }}</strong></template>
        </el-table-column>
        <el-table-column prop="idCard" label="证件号（脱敏）" width="180" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="licenseType" label="车型" width="70" />
        <el-table-column prop="enrollDate" label="报名日期" width="120" />
        <el-table-column label="教练 / 车辆" width="160">
          <template #default="{ row }">
            {{ row.coachName ?? '待分配' }} / {{ row.vehiclePlate ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缴费" width="90">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.payment)">
              {{ row.payment }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link size="small" @click="openAssign(row)">分配</el-button>
            <el-button
              v-if="row.status === '待审核'"
              link
              type="primary"
              size="small"
              @click="audit(row, true)"
            >
              审核
            </el-button>
            <el-button link size="small" @click="registerPayment(row)">缴费</el-button>
            <el-dropdown trigger="click" @command="(cmd) => changeStatus(row, cmd)">
              <el-button link size="small">状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="在训">置为在训</el-dropdown-item>
                  <el-dropdown-item command="结业">确认结业</el-dropdown-item>
                  <el-dropdown-item command="退学" divided>退学</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </div>

    <!-- 报名登记 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑学员档案' : '报名登记'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
        </div>

        <el-form-item :label="editing ? '身份证号（留空则不修改）' : '身份证号'">
          <el-input v-model="form.idCard" placeholder="存储时 AES 加密，展示脱敏" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="准驾车型">
            <el-select v-model="form.licenseType" class="u-full">
              <el-option v-for="t in LICENSE_TYPES" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="报名日期">
            <el-date-picker
              v-model="form.enrollDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="u-full"
            />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="带教教练">
            <el-select v-model="form.coachId" class="u-full" clearable>
              <el-option v-for="c in data.coaches" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="教练车辆">
            <el-select v-model="form.vehicleId" class="u-full" clearable>
              <el-option v-for="v in coachVehicles" :key="v.id" :label="v.plateNo" :value="v.id" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="报名状态">
          <el-radio-group v-model="form.status">
            <el-radio-button v-for="s in STUDENT_STATUS" :key="s" :value="s">{{ s }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配教练与车辆 -->
    <el-dialog v-model="assignVisible" title="分配带教教练与车辆" width="440px">
      <p class="assign-tip">本期实行固定带教：学员约课只能预约本人带教教练发布的时段。</p>
      <el-form label-position="top">
        <el-form-item label="带教教练">
          <el-select v-model="assignForm.coachId" class="u-full" placeholder="选择教练">
            <el-option
              v-for="c in data.coaches"
              :key="c.id"
              :label="`${c.name}（${(c.licenseTypeList || []).join('/')}）· 带教 ${c.studentCount} 人`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="教练车辆">
          <el-select v-model="assignForm.vehicleId" class="u-full" placeholder="选择车辆" clearable>
            <el-option
              v-for="v in assignVehicles"
              :key="v.id"
              :label="`${v.plateNo} · ${v.model ?? ''}（${v.status}）`"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="onAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.query {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.assign-tip {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--bg-sunken);
  border: 1px solid var(--border-subtle);
  font-size: 12.5px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
