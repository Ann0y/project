<script setup>
/**
 * 页面级错误态组件
 * ---------------------------------------------------------------------------
 * 加载失败时展示统一的「失败 + 重试」空态，替代呆板的 el-empty。
 * 用法：<ErrorState message="加载培训进度失败" @retry="reload" />
 */
defineProps({
  /** 展示给用户的错误说明，缺省给出通用提示 */
  message: { type: String, default: '加载失败，请检查网络或后端服务' },
})

const emit = defineEmits(['retry'])
</script>

<template>
  <div class="error-state">
    <el-empty :image-size="72">
      <template #description>
        <span class="error-state__msg">{{ message }}</span>
      </template>
      <el-button type="primary" @click="emit('retry')">重试</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.error-state__msg {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
