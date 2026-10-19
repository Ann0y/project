import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, GaugeChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GaugeChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer,
])

/**
 * 图表色板（与 main.css 令牌保持一致）
 * 单一墨蓝强调色打头，其余走冷调中性阶，语义色仅在需要区分时使用。
 */
export const CHART_COLORS = {
  accent: '#2b52d4',
  accentDeep: '#2242aa',
  accentSoft: '#eaeefb',
  ink900: '#14171c',
  ink600: '#454c56',
  ink400: '#7b8391',
  ink200: '#c6ccd4',
  ink100: '#eceff3',
  ok: '#1f7a5c',
  pending: '#96631b',
  danger: '#b23b3b',
  surface: '#ffffff',
}

const PALETTE = [
  CHART_COLORS.accent,
  CHART_COLORS.ink600,
  CHART_COLORS.ink400,
  CHART_COLORS.ink200,
  CHART_COLORS.ok,
  CHART_COLORS.pending,
]

/**
 * 读取图表基础样式与语义色。
 * 所有图表都用它保证与页面配色体系一致。
 */
export function chartTheme() {
  const axis = CHART_COLORS.ink200
  const label = '#5f6771'
  const text = CHART_COLORS.ink900
  const split = 'rgba(20,23,28,0.06)'

  return {
    color: PALETTE,
    axis,
    label,
    text,
    split,

    /* 语义色，供各图表直接取用 */
    accent: CHART_COLORS.accent,
    accentDeep: CHART_COLORS.accentDeep,
    accentSoft: CHART_COLORS.accentSoft,
    muted: CHART_COLORS.ink400,
    soft: CHART_COLORS.ink200,
    track: CHART_COLORS.ink100,
    surface: CHART_COLORS.surface,
    ok: CHART_COLORS.ok,
    pending: CHART_COLORS.pending,
    danger: CHART_COLORS.danger,

    /** 面积图渐变（强调色 → 透明） */
    areaGradient(from = CHART_COLORS.accent, to = 'rgba(43,82,212,0)') {
      return {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: from },
          { offset: 1, color: to },
        ],
      }
    },

    tooltip: {
      backgroundColor: 'rgba(255,255,255,0.94)',
      borderColor: 'rgba(20,23,28,0.08)',
      borderWidth: 1,
      padding: [8, 12],
      textStyle: { color: text, fontSize: 12 },
      extraCssText: 'box-shadow: 0 8px 24px rgba(16,20,30,0.10); border-radius: 10px;',
    },
    axisStyle: {
      axisLine: { lineStyle: { color: axis } },
      axisTick: { show: false },
      axisLabel: { color: label, fontSize: 11 },
      splitLine: { lineStyle: { color: split, type: 'dashed' } },
    },
  }
}

/**
 * 合并基础主题与自定义配置。
 * @param {object} option 业务配置
 */
export function withTheme(option) {
  const t = chartTheme()
  return {
    color: t.color,
    textStyle: { fontFamily: 'inherit', color: t.text },
    tooltip: { ...t.tooltip, ...(option.tooltip ?? {}) },
    ...option,
  }
}

/**
 * ECharts 通用容器组件。
 * 自动处理：初始化、尺寸自适应、卸载销毁。
 */
export function useChart(optionRef) {
  const el = ref(null)
  const instance = shallowRef(null)
  let observer = null

  function render() {
    if (!el.value || !optionRef.value) return
    if (!instance.value) {
      instance.value = echarts.init(el.value, null, { renderer: 'canvas' })
    }
    instance.value.setOption(withTheme(optionRef.value), true)
  }

  function resize() {
    instance.value?.resize()
  }

  onMounted(() => {
    render()
    observer = new ResizeObserver(resize)
    observer.observe(el.value)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    instance.value?.dispose()
    instance.value = null
  })

  watch(optionRef, render, { deep: true })

  return { el, instance, resize }
}
