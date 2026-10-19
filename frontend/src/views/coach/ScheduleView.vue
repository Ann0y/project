<script setup>
/**
 * F06 我的排课（教练端）
 * ---------------------------------------------------------------------------
 * 需求 F06：维护本人可预约时段（日期、时段、容量、关联车辆）。
 * 接口：/api/coach/schedules
 *
 * 车辆下拉：后端教练接口未单列车辆列表，这里读取管理员车辆接口不可行（权限不足），
 * 因此以「当前教练的车辆」为可选范围：由后端 workbench/coach 信息扩展，
 * 若不可用则允许留空（后端 vehicleId 可为 null）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { timeRange } from '../../utils/format'
import { useDataStore } from '../../stores/data'

const data = useDataStore()
const loading = ref(false)

const rows = computed(() =>
  [...data.schedules].sort((a, b) =>
    a.scheduleDate === b.scheduleDate
      ? String(a.startTime).localeCompare(String(b.startTime))
      : b.scheduleDate.localeCompare(a.scheduleDate),
  ),
)

/** 可关联车辆：来自 workbench 中教练档案附带的车辆（后端暂未提供则留空） */
const myVehicles = computed(() => data.workbench?.vehicles ?? [])

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  scheduleDate: '',
  startTime: '08:00',
  endTime: '10:00',
  capacity: 1,
  vehicleId: null,
})

const rules = {
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

async function reload() {
  loading.value = true
  try {
    await data.loadCoachSchedules()
  } catch (e) {
    ElMessage.error(e.message || '加载排课失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // workbench 里带教练档案（含准教车型），顺带加载一次
  if (!data.workbench) {
    try {
      await data.loadWorkbench()
    } catch {
      /* 忽略 */
    }
  }
  await reload()
})

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    scheduleDate: new Date().toISOString().slice(0, 10),
    startTime: '08:00',
    endTime: '10:00',
    capacity: 1,
    vehicleId: myVehicles.value[0]?.id ?? null,
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    scheduleDate: row.scheduleDate,
    startTime: String(row.startTime).slice(0, 5),
    endTime: String(row.endTime).slice(0, 5),
    capacity: row.capacity,
    vehicleId: row.vehicleId,
  })
  dialogVisible.value = true
}

function onSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.startTime >= form.endTime) {
      ElMessage.error('结束时间必须晚于开始时间')
      return
    }
    const result = await data.saveSchedule({ id: editingId.value, ...form })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(editingId.value ? '排课时段已更新' : '排课时段已发布')
    dialogVisible.value = false
    await reload()
  })
}

async function remove(row) {
  if (row.bookedCount > 0) {
    ElMessage.warning('该时段已有学员预约，不能删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `${row.scheduleDate} ${row.startTime}-${row.endTime}`,
      '确认删除该时段？',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  const result = await data.removeSchedule(row.id)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success('时段已删除')
  await reload()
}

function vehiclePlate(id) {
  return myVehicles.value.find((v) => v.id === id)?.plateNo ?? (id ? `车辆#${id}` : '未关联')
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="教练端"
      title="我的排课"
      desc="维护本人可预约时段，学员只能预约你发布的时段。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增时段</el-button>
      </template>
    </PageHeader>

    <div class="glass panel">
      <el-skeleton v-if="loading" :rows="3" animated />
      <el-empty v-else-if="rows.length === 0" description="暂无排课时段" />

      <el-table v-else :data="rows" stripe>
        <el-table-column prop="scheduleDate" label="日期" width="130" />
        <el-table-column label="时段" width="160">
          <template #default="{ row }">{{ timeRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="关联车辆" width="150">
          <template #default="{ row }">{{ vehiclePlate(row.vehicleId) }}</template>
        </el-table-column>
        <el-table-column label="已约 / 容量" width="120">
          <template #default="{ row }">{{ row.bookedCount }} / {{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑排课时段' : '新增排课时段'"
      width="420px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="排课日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            class="u-full"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-select
            v-model="form.startTime"
            start="06:00"
            step="00:30"
            end="21:00"
            class="u-full"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-select
            v-model="form.endTime"
            start="06:00"
            step="00:30"
            end="21:00"
            class="u-full"
          />
        </el-form-item>
        <el-form-item label="容量（可约人数）">
          <el-input-number v-model="form.capacity" :min="1" :max="10" class="u-full" />
        </el-form-item>
        <el-form-item v-if="myVehicles.length" label="关联车辆">
          <el-select v-model="form.vehicleId" class="u-full" clearable>
            <el-option
              v-for="v in myVehicles"
              :key="v.id"
              :label="`${v.plateNo} · ${v.model ?? ''}`"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
