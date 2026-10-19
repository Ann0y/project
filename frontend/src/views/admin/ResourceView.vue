<script setup>
/**
 * F12 教练与车辆管理（管理端）
 * ---------------------------------------------------------------------------
 * 需求 F12：教练信息、账号与车辆档案维护（含可用状态维护）。
 * 接口：/api/coaches、/api/vehicles
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { useDataStore } from '../../stores/data'
import { LICENSE_TYPES, VEHICLE_STATUS } from '../../data/demo'

const data = useDataStore()
const tab = ref('coach')
const loading = ref(false)

const coachRows = computed(() => data.coaches)
const vehicleRows = computed(() => data.vehicles)

async function reload() {
  loading.value = true
  try {
    await Promise.all([data.loadCoaches(), data.loadVehicles()])
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)

/* -------------------------------- 教练 -------------------------------- */
const coachDialog = ref(false)
const editingCoachId = ref(null)
const coachFormRef = ref()
const coachForm = reactive({
  code: '',
  name: '',
  phone: '',
  licenseTypes: ['C1'],
  status: '在职',
})

const coachRules = {
  code: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

function openCoachCreate() {
  editingCoachId.value = null
  Object.assign(coachForm, {
    code: `C${String(coachRows.value.length + 1).padStart(3, '0')}`,
    name: '',
    phone: '',
    licenseTypes: ['C1'],
    status: '在职',
  })
  coachDialog.value = true
}

function openCoachEdit(row) {
  editingCoachId.value = row.id
  Object.assign(coachForm, {
    code: row.code,
    name: row.name,
    phone: row.phone,
    licenseTypes: [...(row.licenseTypeList || [])],
    status: row.status,
  })
  coachDialog.value = true
}

function onCoachSubmit() {
  if (!coachFormRef.value) return
  coachFormRef.value.validate(async (valid) => {
    if (!valid) return
    const result = await data.saveCoach({ id: editingCoachId.value, ...coachForm })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(editingCoachId.value ? '教练信息已更新' : '教练已新增（初始密码 123456）')
    coachDialog.value = false
    await reload()
  })
}

/* -------------------------------- 车辆 -------------------------------- */
const vehicleDialog = ref(false)
const editingVehicleId = ref(null)
const vehicleFormRef = ref()
const vehicleForm = reactive({ plateNo: '', model: '', coachId: null, status: '可用' })

const vehicleRules = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  model: [{ required: true, message: '请输入车型', trigger: 'blur' }],
}

function openVehicleCreate() {
  editingVehicleId.value = null
  Object.assign(vehicleForm, {
    plateNo: '',
    model: '',
    coachId: coachRows.value[0]?.id ?? null,
    status: '可用',
  })
  vehicleDialog.value = true
}

function openVehicleEdit(row) {
  editingVehicleId.value = row.id
  Object.assign(vehicleForm, { ...row })
  vehicleDialog.value = true
}

function onVehicleSubmit() {
  if (!vehicleFormRef.value) return
  vehicleFormRef.value.validate(async (valid) => {
    if (!valid) return
    const result = await data.saveVehicle({ id: editingVehicleId.value, ...vehicleForm })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(editingVehicleId.value ? '车辆档案已更新' : '车辆已新增')
    vehicleDialog.value = false
    await reload()
  })
}

async function setVehicleStatus(row, status) {
  const result = await data.changeVehicleStatus(row.id, status)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(`车辆状态已置为「${status}」`)
  await reload()
}

function coachName(id) {
  return coachRows.value.find((c) => c.id === id)?.name ?? '未分配'
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="教练与车辆管理"
      desc="维护教练信息与账号状态，管理教练车辆档案及其可用状态。"
    >
      <template #actions>
        <el-button
          type="primary"
          :icon="Plus"
          @click="tab === 'coach' ? openCoachCreate() : openVehicleCreate()"
        >
          {{ tab === 'coach' ? '新增教练' : '新增车辆' }}
        </el-button>
      </template>
    </PageHeader>

    <div class="glass panel">
      <el-tabs v-model="tab">
        <el-tab-pane label="教练信息" name="coach">
          <el-table v-loading="loading" :data="coachRows" stripe>
            <el-table-column prop="code" label="工号" width="90" />
            <el-table-column prop="name" label="姓名" width="110">
              <template #default="{ row }"><strong>{{ row.name }}</strong></template>
            </el-table-column>
            <el-table-column label="准教车型" width="130">
              <template #default="{ row }">{{ (row.licenseTypeList || []).join(' / ') }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="140" />
            <el-table-column label="带教" width="90">
              <template #default="{ row }">{{ row.studentCount }} 人</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openCoachEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="教练车辆" name="vehicle">
          <el-table v-loading="loading" :data="vehicleRows" stripe>
            <el-table-column prop="plateNo" label="车牌号" width="150">
              <template #default="{ row }"><strong>{{ row.plateNo }}</strong></template>
            </el-table-column>
            <el-table-column prop="model" label="车型" width="140" />
            <el-table-column label="所属教练" width="120">
              <template #default="{ row }">{{ coachName(row.coachId) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column label="可用状态维护" min-width="220">
              <template #default="{ row }">
                <el-button
                  v-for="s in VEHICLE_STATUS"
                  :key="s"
                  link
                  size="small"
                  :disabled="row.status === s"
                  @click="setVehicleStatus(row, s)"
                >
                  {{ s }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openVehicleEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 教练弹窗 -->
    <el-dialog
      v-model="coachDialog"
      :title="editingCoachId ? '编辑教练信息' : '新增教练'"
      width="440px"
    >
      <el-form ref="coachFormRef" :model="coachForm" :rules="coachRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="工号" prop="code">
            <el-input v-model="coachForm.code" :disabled="!!editingCoachId" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="coachForm.name" />
          </el-form-item>
        </div>
        <el-form-item label="联系电话">
          <el-input v-model="coachForm.phone" />
        </el-form-item>
        <el-form-item label="准教车型">
          <el-checkbox-group v-model="coachForm.licenseTypes">
            <el-checkbox v-for="t in LICENSE_TYPES" :key="t" :value="t">{{ t }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="!editingCoachId" label="初始密码">
          <span class="u-dim">默认 123456，登录账号与工号一致（小写）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="coachDialog = false">取消</el-button>
        <el-button type="primary" @click="onCoachSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 车辆弹窗 -->
    <el-dialog
      v-model="vehicleDialog"
      :title="editingVehicleId ? '编辑车辆档案' : '新增车辆'"
      width="420px"
    >
      <el-form ref="vehicleFormRef" :model="vehicleForm" :rules="vehicleRules" label-position="top">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="vehicleForm.plateNo" placeholder="如 沪A·1234学" />
        </el-form-item>
        <el-form-item label="车型" prop="model">
          <el-input v-model="vehicleForm.model" placeholder="如 大众桑塔纳" />
        </el-form-item>
        <el-form-item label="所属教练">
          <el-select v-model="vehicleForm.coachId" class="u-full">
            <el-option v-for="c in coachRows" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="vehicleForm.status">
            <el-radio-button v-for="s in VEHICLE_STATUS" :key="s" :value="s">{{ s }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vehicleDialog = false">取消</el-button>
        <el-button type="primary" @click="onVehicleSubmit">保存</el-button>
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
