<script setup>
const features = [
  '教练基本信息维护',
  '准教车型与带教学员情况',
  '教练车辆档案管理',
  '车辆可用状态维护',
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">教练与车辆</h1>
      <p class="page__summary">
        维护教练基本信息、准教车型与带教学员情况，管理教练车辆档案及其可用状态，为排课与约车提供资源支撑。
      </p>
    </header>

    <el-card shadow="never" class="page__card">
      <template #header>
        <div class="page__card-header">
          <span>功能清单</span>
          <el-tag size="small">开发中</el-tag>
        </div>
      </template>
      <ul class="page__features">
        <li v-for="feature in features" :key="feature">{{ feature }}</li>
      </ul>
    </el-card>
  </section>
</template>
