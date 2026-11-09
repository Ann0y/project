<script setup>
/**
 * 学费缴纳（学员端）
 * ---------------------------------------------------------------------------
 * 学员可以查看本人待缴费用并在线完成缴费，缴费成功后状态实时更新。
 * 数据：GET /api/payments/my
 * 支付：POST /api/payments/my/{id}/pay  body={ payMethod }
 *
 * 说明：当前为演示环境，点击「确认支付」会直接把记录标记为已缴，
 *       不接入真实第三方支付，不会产生真实扣款。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import ErrorState from '../../components/ErrorState.vue'
import { statusTone } from '../../utils/status'
import { dateTime } from '../../utils/format'
import { useDataStore } from '../../stores/data'

const PAY_METHODS = ['微信', '支付宝', '银行', '现金']

const data = useDataStore()
const loading = ref(false)
const failed = ref(false)

/* ------------------------------ 缴费弹窗 ------------------------------ */
const payVisible = ref(false)
const payTarget = ref(null)
const payMethod = ref('微信')
const paying = ref(false)

const payments = computed(() => data.payments)
const unpaid = computed(() => payments.value.filter((p) => p.status === '未缴'))
const history = computed(() => payments.value.filter((p) => p.status !== '未缴'))

const unpaidTotal = computed(() =>
  unpaid.value.reduce((acc, p) => acc + Number(p.amount ?? 0), 0),
)
const paidTotal = computed(() =>
  payments.value
    .filter((p) => p.status === '已缴')
    .reduce((acc, p) => acc + Number(p.amount ?? 0), 0),
)

function money(value) {
  return Number(value ?? 0).toFixed(2)
}

async function load() {
  loading.value = true
  failed.value = false
  try {
    await data.loadMyPayments()
  } catch (e) {
    failed.value = true
    ElMessage.error(e.message || '加载缴费记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openPay(payment) {
  payTarget.value = payment
  payMethod.value = '微信'
  payVisible.value = true
}

async function submitPay() {
  if (!payTarget.value) return
  paying.value = true
  const result = await data.payMyPayment(payTarget.value.id, payMethod.value)
  paying.value = false
  if (!result.ok) {
    ElMessage.error(result.reason || '缴费失败')
    return
  }
  ElMessage.success('缴费成功，缴费状态已更新')
  payVisible.value = false
  await load()
}
</script>

<template>
  <section class="page page--student">
    <PageHeader
      eyebrow="学员端"
      title="学费缴纳"
      desc="查看待缴学费并在线完成缴费；已缴与退费记录也会保留在缴费历史中。"
    />

    <ErrorState v-if="failed" message="缴费记录加载失败，请检查后端服务后重试" @retry="load" />

    <template v-else>
      <!-- 汇总 -->
      <div class="pay-stats">
        <div class="glass pay-stat">
          <span class="pay-stat__k">待缴金额</span>
          <span class="pay-stat__v" :class="{ 'pay-stat__v--pending': unpaidTotal > 0 }">
            ¥{{ money(unpaidTotal) }}<small>{{ unpaid.length }} 笔</small>
          </span>
        </div>
        <div class="glass pay-stat">
          <span class="pay-stat__k">已缴金额</span>
          <span class="pay-stat__v">¥{{ money(paidTotal) }}</span>
        </div>
        <div class="glass pay-stat">
          <span class="pay-stat__k">缴费记录</span>
          <span class="pay-stat__v">{{ payments.length }}<small>笔</small></span>
        </div>
      </div>

      <!-- 待缴费用 -->
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">待缴费用</span>
          <span class="panel__hint">支持微信 / 支付宝 / 银行 / 现金</span>
        </div>

        <el-skeleton v-if="loading" :rows="3" animated />
        <el-empty v-else-if="unpaid.length === 0" description="暂无待缴费用，学费已结清" />

        <div v-else class="pay-list">
          <div v-for="payment in unpaid" :key="payment.id" class="pay-item">
            <div class="pay-item__main">
              <span class="pay-item__title">{{ payment.item }}</span>
              <span class="pay-item__meta">
                待缴 · {{ payment.payTime ? `生成于 ${dateTime(payment.payTime)}` : '报名登记后生成' }}
              </span>
            </div>
            <span class="pay-item__amount">¥{{ money(payment.amount) }}</span>
            <el-button type="primary" @click="openPay(payment)">立即缴费</el-button>
          </div>
        </div>
      </div>

      <!-- 缴费历史 -->
      <div class="glass panel">
        <div class="panel__head">
          <span class="panel__title">缴费记录</span>
          <span class="panel__hint">共 {{ history.length }} 笔</span>
        </div>

        <el-skeleton v-if="loading" :rows="3" animated />
        <el-empty v-else-if="history.length === 0" description="暂无已完成的缴费记录" />

        <el-table v-else :data="history" stripe>
          <el-table-column prop="item" label="缴费项目" min-width="160" />
          <el-table-column label="金额" width="130">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="缴费方式" width="110">
            <template #default="{ row }">{{ row.payMethod || '-' }}</template>
          </el-table-column>
          <el-table-column label="缴费时间" min-width="170">
            <template #default="{ row }">{{ row.payTime ? dateTime(row.payTime) : '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="chip" :class="statusTone(row.status)">{{ row.status }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 缴费确认弹窗 -->
    <el-dialog
      v-model="payVisible"
      title="确认缴费"
      width="430px"
      top="16vh"
      :close-on-click-modal="false"
    >
      <div v-if="payTarget" class="pay-dialog">
        <div class="pay-dialog__row">
          <span>缴费项目</span>
          <strong>{{ payTarget.item }}</strong>
        </div>
        <div class="pay-dialog__row">
          <span>缴费金额</span>
          <strong class="pay-dialog__amount">¥{{ money(payTarget.amount) }}</strong>
        </div>

        <el-form label-position="top">
          <el-form-item label="支付方式">
            <el-radio-group v-model="payMethod">
              <el-radio-button v-for="m in PAY_METHODS" :key="m" :value="m">
                {{ m }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <p class="pay-dialog__tip">
          演示环境：点击「确认支付」会直接将这笔费用标记为已缴，不会产生真实扣款。
        </p>
      </div>

      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="submitPay">确认支付</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

.pay-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.pay-stat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px 24px;
}

.pay-stat__k {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.pay-stat__v {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-primary);
}

.pay-stat__v small {
  margin-left: 6px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.pay-stat__v--pending {
  color: var(--state-pending);
}

.pay-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pay-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--bg-sunken);
}

.pay-item__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.pay-item__title {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-primary);
}

.pay-item__meta {
  font-size: 11.5px;
  color: var(--text-tertiary);
}

.pay-item__amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--state-pending);
  font-variant-numeric: tabular-nums;
}

.pay-dialog__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 13px;
  color: var(--text-secondary);
}

.pay-dialog__amount {
  font-size: 20px;
  color: var(--state-pending);
}

.pay-dialog :deep(.el-form) {
  margin-top: 20px;
}

.pay-dialog__tip {
  margin: 10px 0 0;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-tertiary);
}

@media (max-width: 640px) {
  .pay-stats {
    grid-template-columns: 1fr;
  }

  .pay-item {
    flex-wrap: wrap;
  }
}
</style>
