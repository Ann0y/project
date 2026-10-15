<script setup>
const features = [
  '报名人数统计',
  '培训学时统计',
  '考试通过率统计',
  '收费情况统计',
  'ECharts 图表可视化展示',
]
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1 class="page__title">统计报表</h1>
      <p class="page__summary">
        对报名人数、培训学时、考试通过率、收费情况等运营数据进行统计并以图表形式直观展示，辅助管理者经营决策。
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
