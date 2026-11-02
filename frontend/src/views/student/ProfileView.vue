<script setup>
/**
 * F01 个人中心（学员端）
 * ---------------------------------------------------------------------------
 * 需求 F01：个人中心含我的信息、修改密码。
 * 数据：GET /api/students/me/progress、GET /api/payments/my
 * 页面仅展示「我的信息」，缴费记录与修改密码分别通过按钮弹出弹窗查看/操作。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import ErrorState from '../../components/ErrorState.vue'
import { statusTone } from '../../utils/status'
import { dateTime } from '../../utils/format'
import { useAuthStore } from '../../stores/auth'
import { useDataStore } from '../../stores/data'

const auth = useAuthStore()
const data = useDataStore()

const student = computed(() => data.progress)
const myPayments = computed(() => data.payments)
const failed = ref(false)

/* ---------- 弹窗开关 ---------- */
const paymentsDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const paymentsLoading = ref(false)

onMounted(load)

/** 已缓存 progress 时不再重复请求（约车页也是同样约定），失败可点重试 */
async function load() {
  failed.value = false
  if (data.progress) return
  try {
    await data.loadProgress()
  } catch (e) {
    failed.value = true
    ElMessage.error(e.message || '加载个人信息失败')
  }
}

/** 点开弹窗时才请求缴费数据 */
async function openPaymentsDialog() {
  paymentsDialogVisible.value = true
  paymentsLoading.value = true
  try {
    await data.loadMyPayments()
  } catch (e) {
    ElMessage.error(e.message || '加载缴费记录失败')
  } finally {
    paymentsLoading.value = false
  }
}

/* ---------- 缴费汇总（弹窗顶部） ---------- */
const paymentsSummary = computed(() => {
  const paid = myPayments.value.filter((p) => p.status === '已缴')
  const unpaid = myPayments.value.filter((p) => p.status === '未缴')
  const sum = (list) => list.reduce((acc, p) => acc + Number(p.amount ?? 0), 0)
  return {
    count: myPayments.value.length,
    paidTotal: sum(paid),
    paidCount: paid.length,
    unpaidTotal: sum(unpaid),
    unpaidCount: unpaid.length,
  }
})

/* ---------- 修改密码 ---------- */
const formRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
        value === pwdForm.newPassword ? callback() : callback(new Error('两次输入的新密码不一致')),
      trigger: 'blur',
    },
  ],
}

function openPasswordDialog() {
  // 每次打开都清空上一次的输入
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await auth.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    formRef.value.resetFields()
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  }
}
</script>

<template>
  <section class="page">
    <PageHeader
      eyebrow="学员端"
      title="个人中心"
      desc="查看我的信息；缴费记录与修改密码请点击对应按钮查看。"
    />

    <ErrorState v-if="failed" message="个人信息加载失败，请检查后端服务后重试" @retry="load" />
    <template v-else>
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">我的信息</span>
        </div>
        <el-skeleton v-if="!student" :rows="5" animated />
        <div v-else class="meta-list">
          <div class="meta-item">
            <span class="meta-item__k">姓名</span>
            <span class="meta-item__v">{{ student.name }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">登录账号</span>
            <span class="meta-item__v">{{ auth.user?.username }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">身份证号（脱敏）</span>
            <span class="meta-item__v">{{ student.idCardMasked }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">联系电话</span>
            <span class="meta-item__v">{{ student.phone }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">准驾车型</span>
            <span class="meta-item__v">{{ student.licenseType }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">报名日期</span>
            <span class="meta-item__v">{{ student.enrollDate }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">带教教练</span>
            <span class="meta-item__v">{{ student.coachName || '待分配' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-item__k">教练车辆</span>
            <span class="meta-item__v">{{ student.vehiclePlate || '待分配' }}</span>
          </div>
        </div>

        <div class="u-inline">
          <el-button type="primary" :loading="paymentsLoading" @click="openPaymentsDialog">
            查看缴费记录
          </el-button>
          <el-button @click="openPasswordDialog">修改密码</el-button>
        </div>
      </div>
    </template>
    <!-- 缴费记录弹窗 -->
    <el-dialog v-model="paymentsDialogVisible" title="我的缴费记录" width="640px">
      <div v-if="myPayments.length" class="pay-summary">
        <div class="pay-summary__item">
          <span class="pay-summary__k">已缴金额</span>
          <span class="pay-summary__v">¥{{ paymentsSummary.paidTotal }}（{{ paymentsSummary.paidCount }} 笔）</span>
        </div>
        <div class="pay-summary__item">
          <span class="pay-summary__k">待缴金额</span>
          <span class="pay-summary__v">¥{{ paymentsSummary.unpaidTotal }}（{{ paymentsSummary.unpaidCount }} 笔）</span>
        </div>
        <div class="pay-summary__item">
          <span class="pay-summary__k">缴费笔数</span>
          <span class="pay-summary__v">{{ paymentsSummary.count }} 笔</span>
        </div>
      </div>

      <el-empty v-if="!paymentsLoading && myPayments.length === 0" description="暂无缴费记录" />
      <el-table v-else :data="myPayments" stripe v-loading="paymentsLoading">
        <el-table-column prop="item" label="项目" width="110" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="payTime" label="缴费时间" min-width="160">
          <template #default="{ row }">{{ row.payTime ? dateTime(row.payTime) : '未缴费' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="chip" :class="statusTone(row.status)">
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px">
      <el-form
        ref="formRef"
        :model="pwdForm"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="u-full" native-type="submit">保存修改</el-button>
      </el-form>
    </el-dialog>
  </section>
</template>

<style scoped>
/* 弹窗顶部缴费汇总 */
.pay-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.pay-summary__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-base);
  background: var(--bg-sunken);
}

.pay-summary__k {
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--text-tertiary);
}

.pay-summary__v {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-primary);
}

@media (max-width: 560px) {
  .pay-summary {
    grid-template-columns: 1fr;
  }
}
</style>
