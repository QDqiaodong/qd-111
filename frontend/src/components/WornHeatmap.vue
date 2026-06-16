<template>
  <div class="worn-heatmap">
    <div v-if="showLegend" class="heatmap-legend">
      <span
        v-for="item in legends"
        :key="item.code"
        class="legend-item"
      >
        <span class="legend-dot" :style="{ background: item.color }"></span>
        <span class="legend-label">{{ item.label }}</span>
      </span>
    </div>

    <div class="heatmap-container" v-loading="loading">
      <div class="heatmap-header">
        <div class="corner-cell"></div>
        <div
          v-for="type in accessoryTypes"
          :key="type.code"
          class="header-cell"
          :title="type.label"
        >
          {{ type.label }}
        </div>
      </div>

      <div
        v-for="inst in instruments"
        :key="inst.code"
        class="heatmap-row"
      >
        <div class="row-label" :title="inst.label">
          {{ inst.label }}
        </div>
        <div
          v-for="type in accessoryTypes"
          :key="type.code"
          class="heatmap-cell"
          :class="{ 'cell-empty': getCell(inst.code, type.code).total === 0 }"
        >
          <div
            v-if="getCell(inst.code, type.code).total > 0"
            class="cell-stack"
          >
            <div
              v-for="status in statusOrder"
              :key="status"
              class="cell-segment"
              :style="getSegmentStyle(inst.code, type.code, status)"
              :title="getSegmentTitle(inst.code, type.code, status)"
            ></div>
          </div>
          <div v-else class="cell-empty-text">-</div>
          <div class="cell-count">
            {{ getCell(inst.code, type.code).total || '' }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  showLegend: {
    type: Boolean,
    default: true
  }
})

const statusOrder = ['good', 'slight', 'severe', 'broken']

const instruments = computed(() => props.data?.instruments || [])
const accessoryTypes = computed(() => props.data?.accessoryTypes || [])
const legends = computed(() => props.data?.legends || [])
const cells = computed(() => {
  const map = {}
  const list = props.data?.cells || []
  list.forEach(cell => {
    map[`${cell.instrumentCode}_${cell.typeCode}`] = cell
  })
  return map
})

const getCell = (instCode, typeCode) => {
  return cells.value[`${instCode}_${typeCode}`] || { total: 0 }
}

const getStatusCount = (instCode, typeCode, status) => {
  const cell = getCell(instCode, typeCode)
  const countMap = {
    good: cell.goodCount || 0,
    slight: cell.slightCount || 0,
    severe: cell.severeCount || 0,
    broken: cell.brokenCount || 0
  }
  return countMap[status] || 0
}

const getStatusColor = (status) => {
  const item = legends.value.find(l => l.code === status)
  return item ? item.color : '#909399'
}

const getSegmentStyle = (instCode, typeCode, status) => {
  const cell = getCell(instCode, typeCode)
  const total = cell.total || 0
  const count = getStatusCount(instCode, typeCode, status)
  const width = total > 0 ? (count / total) * 100 : 0
  return {
    width: width + '%',
    background: getStatusColor(status)
  }
}

const getSegmentTitle = (instCode, typeCode, status) => {
  const cell = getCell(instCode, typeCode)
  const count = getStatusCount(instCode, typeCode, status)
  const label = legends.value.find(l => l.code === status)?.label || status
  return `${cell.instrumentName || ''} / ${cell.typeName || ''} - ${label}: ${count}`
}
</script>

<style lang="scss" scoped>
.worn-heatmap {
  width: 100%;
}

.heatmap-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  padding: 0 4px;

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;
  }

  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 2px;
    display: inline-block;
  }

  .legend-label {
    white-space: nowrap;
  }
}

.heatmap-container {
  overflow-x: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.heatmap-header {
  display: flex;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 2;

  .corner-cell {
    width: 100px;
    min-width: 100px;
    flex-shrink: 0;
    border-right: 1px solid #ebeef5;
  }

  .header-cell {
    flex: 1;
    min-width: 80px;
    padding: 10px 8px;
    text-align: center;
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    border-right: 1px solid #ebeef5;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &:last-child {
      border-right: none;
    }
  }
}

.heatmap-row {
  display: flex;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    .heatmap-cell {
      background: #f5f7fa;
    }
  }

  .row-label {
    width: 100px;
    min-width: 100px;
    flex-shrink: 0;
    padding: 12px 10px;
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    background: #fafafa;
    border-right: 1px solid #ebeef5;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.heatmap-cell {
  flex: 1;
  min-width: 80px;
  padding: 10px 8px;
  border-right: 1px solid #ebeef5;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background 0.2s;

  &:last-child {
    border-right: none;
  }

  &.cell-empty {
    .cell-count {
      color: #c0c4cc;
    }
  }

  .cell-empty-text {
    color: #dcdfe6;
    font-size: 18px;
    line-height: 1;
  }

  .cell-stack {
    display: flex;
    width: 100%;
    height: 8px;
    border-radius: 2px;
    overflow: hidden;
    background: #f0f2f5;

    .cell-segment {
      height: 100%;
      transition: width 0.3s;
      cursor: help;

      &:first-child {
        border-radius: 2px 0 0 2px;
      }

      &:last-child {
        border-radius: 0 2px 2px 0;
      }
    }
  }

  .cell-count {
    font-size: 12px;
    color: #909399;
    font-weight: 500;
    line-height: 1;
  }
}
</style>
