<script setup>
/**
 * F16 用户管理（管理端）
 * ---------------------------------------------------------------------------
 * 需求 F16：账号增删改查、启停用、角色分配（管理员 / 教练 / 学员）。
 * 接口：/api/users
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { dateTime } from '../../utils/format'
import { ROLES, roleName } from '../../config/roles'
import { useDataStore } from '../../stores/data'

const ROLE_KEYS = ['admin', 'coach', 'student']

const data = useDataStore()
const loading = ref(false)

const rows = computed(() => data.userPage.list ?? [])
const total = computed(() => data.userPage.total ?? 0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const roleFilter = ref('')

async function load() {
  loading.value = true
  try {
    await data.loadUsers({
      keyword: keyword.value || undefined,
      role: roleFilter.value || undefined,
      page: page.value,
      size: pageSize.value,
    })
  } catch (e) {
    ElMessage.error(e.message || '加载用户失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onSearch() {
  page.value = 1
  load()
}

/* ------------------------------ 新增 / 编辑 ------------------------------ */
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref()
const form = reactive({ username: '', name: '', role: 'student', status: 1 })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

function openCreate() {
  editing.value = null
  Object.assign(form, { username: '', name: '', role: 'student', status: 1 })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    username: row.username,
    name: row.name,
    role: row.role,
    status: row.status,
  })
  dialogVisible.value = true
}

function onSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const result = await data.saveUser({ id: editing.value?.id, ...form })
    if (!result.ok) {
      ElMessage.error(result.reason)
      return
    }
    ElMessage.success(editing.value ? '账号信息已更新' : '账号已创建，初始密码 123456')
    dialogVisible.value = false
    await load()
  })
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 1 ? `启用后 ${row.name} 可正常登录。` : `停用后 ${row.name} 将无法登录。`,
      next === 1 ? '启用账号' : '停用账号',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  const result = await data.setUserStatus(row.id, next)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success(`账号已${next === 1 ? '启用' : '停用'}`)
  await load()
}

async function resetPassword(row) {
  try {
    await ElMessageBox.confirm(`将 ${row.name} 的密码重置为 123456？`, '重置密码', {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  const result = await data.resetUserPassword(row.id, '123456')
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success('密码已重置为 123456')
}

async function remove(row) {
  if (row.username === 'admin') {
    ElMessage.warning('内置管理员账号不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除账号 ${row.username}？`, '删除账号', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  const result = await data.removeUser(row.id)
  if (!result.ok) {
    ElMessage.error(result.reason)
    return
  }
  ElMessage.success('账号已删除')
  await load()
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="管理端"
      title="用户管理"
      desc="账号增删改查、启停用与角色分配（管理员 / 教练 / 学员）。"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增账号</el-button>
      </template>
    </PageHeader>

    <div class="glass panel">
      <div class="panel__head">
        <span class="panel__title">账号列表</span>
        <div class="u-row">
          <el-input
            v-model="keyword"
            placeholder="用户名 / 姓名"
            clearable
            size="small"
            style="width: 160px"
            @change="onSearch"
          />
          <el-select
            v-model="roleFilter"
            placeholder="全部角色"
            clearable
            size="small"
            style="width: 130px"
            @change="onSearch"
          >
            <el-option v-for="key in ROLE_KEYS" :key="key" :label="ROLES[key].name" :value="key" />
          </el-select>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="username" label="用户名" width="150">
          <template #default="{ row }"><strong>{{ row.username }}</strong></template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="130" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <span class="chip" :class="row.role === 'admin' ? 'chip--solid' : 'chip--role'">
              {{ roleName(row.role) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="chip" :class="row.status === 1 ? 'chip--strong' : 'chip--muted'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          min-width="170"
          :formatter="(row) => dateTime(row.createTime)"
        />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link size="small" @click="resetPassword(row)">重置密码</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑账号' : '新增账号'" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :disabled="!!editing" placeholder="3-20 个字符" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </div>

        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio-button v-for="key in ROLE_KEYS" :key="key" :value="key">
              {{ ROLES[key].name }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="账号状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
