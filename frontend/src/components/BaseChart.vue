<script setup>
/**
 * ECharts 图表容器
 * ---------------------------------------------------------------------------
 * 用法：<BaseChart :option="option" height="280px" />
 * 负责初始化、响应式尺寸与销毁，业务侧只关心 option。
 */
import { toRef } from 'vue'
import { useChart } from '../utils/chart'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '280px' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
})

const { el, instance } = useChart(toRef(props, 'option'))

defineExpose({ instance })
</script>

<template>
  <div class="chart-wrap" :style="{ height }">
    <div v-show="!empty" ref="el" class="chart-wrap__canvas" />
    <div v-if="empty" class="chart-wrap__empty">
      <span>{{ emptyText }}</span>
    </div>
  </div>
</template>

<style scoped>
.chart-wrap {
  position: relative;
  width: 100%;
}

.chart-wrap__canvas {
  width: 100%;
  height: 100%;
}

.chart-wrap__empty {
  display: grid;
  place-items: center;
  height: 100%;
  font-size: 13px;
  color: var(--text-tertiary);
  border: 1px dashed var(--border-base);
  border-radius: var(--radius-md);
}
</style>
