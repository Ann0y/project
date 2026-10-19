<script setup>
/**
 * F01 个人中心（学员端）
 * ---------------------------------------------------------------------------
 * 需求 F01：个人中心含我的信息、修改密码。
 * 数据：GET /api/students/me/progress、GET /api/payments/my
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { statusTone } from '../../utils/status'
import { dateTime } from '../../utils/format'
import { useAuthStore } from '../../stores/auth'
import { useDataStore } from '../../stores/data'

const auth = useAuthStore()
const data = useDataStore()

const student = computed(() => data.progress)
const myPayments = computed(() => data.payments)

onMounted(async () => {
  try {
    await Promise.all([
      data.loadProgress(),
      data.loadMyPayments(),
    ])
  } catch (e) {
    ElMessage.error(e.message || '加载个人信息失败')
  }
})

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
      desc="查看我的信息与缴费记录，并可修改登录密码。"
    />

    <div class="grid grid--sidebar">
      <div class="u-stack">
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
        </div>

        <div class="glass panel">
          <div class="panel__head">
            <span class="panel__title">缴费记录</span>
          </div>
          <el-empty v-if="myPayments.length === 0" description="暂无缴费记录" />
          <el-table v-else :data="myPayments" stripe>
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
        </div>
      </div>

      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">修改密码</span>
        </div>

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
      </div>
    </div>
  </section>
</template>
