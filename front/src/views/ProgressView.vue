<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

// 当前为演示数据；接入后端后改为按登录学员 ID 拉取即可。
const student = {
  licenseType: 'C1 小型汽车',
  status: '在训',
  enrollDate: '2026-09-01',
  coach: '王建国',
  vehicle: '沪A·1234学',
  totalHours: 70,
  doneHours: 38,
  payment: '已缴清',
  subjects: [
    { name: '科目一 · 道路交通安全法律法规', done: 12, total: 12, status: '合格' },
    { name: '科目二 · 场地驾驶技能', done: 16, total: 24, status: '培训中' },
    { name: '科目三 · 道路驾驶技能', done: 10, total: 24, status: '培训中' },
    { name: '科目四 · 安全文明驾驶常识', done: 0, total: 10, status: '未开始' },
  ],
  records: [
    { date: '2026-10-08', type: '培训', content: '完成科目三道路驾驶第 10 学时培训' },
    { date: '2026-10-01', type: '考试', content: '科目二场地驾驶模拟考试通过' },
    { date: '2026-09-20', type: '培训', content: '完成科目二场地驾驶第 16 学时培训' },
    { date: '2026-09-05', type: '考试', content: '科目一理论考试 96 分，合格' },
    { date: '2026-09-01', type: '报名', content: '完成报名与缴费，分配教练王建国' },
  ],
}

const name = computed(() => auth.user?.name || '李小明')
const remainingHours = computed(() => student.totalHours - student.doneHours)
const overall = computed(() => Math.round((student.doneHours / student.totalHours) * 100))
const passedCount = computed(() => student.subjects.filter((item) => item.status === '合格').length)

function percent(done, total) {
  return total ? Math.round((done / total) * 100) : 0
}

function statusType(status) {
  return { 合格: 'success', 培训中: 'primary', 未开始: 'info' }[status] ?? 'info'
}

function recordType(type) {
  return { 培训: 'primary', 考试: 'success', 报名: 'info' }[type] ?? 'primary'
}
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">我的培训进度</h1>
      <p class="page__summary">查看报名状态、学时完成情况与各科目培训进度。</p>
    </header>

    <el-row :gutter="16">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="page__card">
          <div class="stat">
            <span class="stat__value">{{ student.doneHours }}<small>/{{ student.totalHours }}</small></span>
            <span class="stat__label">已学学时</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="page__card">
          <div class="stat">
            <span class="stat__value">{{ remainingHours }}</span>
            <span class="stat__label">剩余学时</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="page__card">
          <div class="stat">
            <span class="stat__value">{{ passedCount }}<small>/{{ student.subjects.length }}</small></span>
            <span class="stat__label">已合格科目</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="page__card">
          <div class="stat">
            <span class="stat__value stat__value--text">{{ student.payment }}</span>
            <span class="stat__label">缴费状态</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="subjects-row">
      <el-col :xs="24" :md="15">
        <el-card shadow="never" class="page__card">
          <template #header>
            <div class="page__card-header"><span>各科目培训进度</span></div>
          </template>

          <div v-for="item in student.subjects" :key="item.name" class="subject">
            <div class="subject__head">
              <span class="subject__name">{{ item.name }}</span>
              <el-tag :type="statusType(item.status)" size="small">{{ item.status }}</el-tag>
            </div>
            <el-progress :percentage="percent(item.done, item.total)" :stroke-width="8" />
            <div class="subject__meta">已完成 {{ item.done }} / {{ item.total }} 学时</div>
          </div>
        </el-card>

        <el-card shadow="never" class="page__card records">
          <template #header>
            <div class="page__card-header"><span>最近培训记录</span></div>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="record in student.records"
              :key="record.date + record.content"
              :timestamp="record.date"
              placement="top"
              :type="recordType(record.type)"
            >
              <div class="record">
                <el-tag :type="recordType(record.type)" size="small" effect="plain">
                  {{ record.type }}
                </el-tag>
                <span>{{ record.content }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="9">
        <el-card shadow="never" class="page__card">
          <template #header>
            <div class="page__card-header"><span>整体进度</span></div>
          </template>

          <div class="overall">
            <el-progress type="circle" :percentage="overall" :width="140" />
            <p class="overall__text">共 {{ student.doneHours }} / {{ student.totalHours }} 学时</p>
          </div>
        </el-card>

        <el-card shadow="never" class="page__card profile">
          <template #header>
            <div class="page__card-header"><span>学员信息</span></div>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ name }}</el-descriptions-item>
            <el-descriptions-item label="准驾车型">{{ student.licenseType }}</el-descriptions-item>
            <el-descriptions-item label="报名状态">
              <el-tag type="success" size="small">{{ student.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="报名日期">{{ student.enrollDate }}</el-descriptions-item>
            <el-descriptions-item label="带教教练">{{ student.coach }}</el-descriptions-item>
            <el-descriptions-item label="教练车辆">{{ student.vehicle }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat__value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.1;
  color: var(--color-text);
}

.stat__value small {
  font-size: 14px;
  font-weight: 400;
  color: var(--color-text-muted);
}

.stat__value--text {
  font-size: 22px;
  color: var(--el-color-success);
}

.stat__label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.subjects-row {
  margin-top: 16px;
}

.subject + .subject {
  margin-top: 20px;
}

.subject__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.subject__name {
  font-size: 14px;
  color: var(--color-text);
}

.subject__meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.records {
  margin-top: 16px;
}

.record {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.6;
}

.overall {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.overall__text {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.profile {
  margin-top: 16px;
}
</style>
