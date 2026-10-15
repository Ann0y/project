<script setup>
const metrics = [
  { label: '在训学员', value: 328, unit: '人' },
  { label: '本月报名', value: 46, unit: '人' },
  { label: '本月培训学时', value: 1820, unit: '学时' },
  { label: '本月考试通过率', value: 86.5, unit: '%' },
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">工作台</h1>
      <p class="page__summary">报名人数、培训学时、考试通过率等核心运营指标概览。</p>
    </header>

    <el-row :gutter="16">
      <el-col v-for="item in metrics" :key="item.label" :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="page__card">
          <el-statistic :title="item.label" :value="item.value" :suffix="item.unit" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="page__card dashboard__todo">
      <template #header>
        <div class="page__card-header">
          <span>待办事项</span>
          <el-tag type="warning" size="small">开发中</el-tag>
        </div>
      </template>
      <el-empty description="工作台内容开发中" />
    </el-card>
  </section>
</template>

<style scoped>
.dashboard__todo {
  margin-top: 16px;
}
</style>
