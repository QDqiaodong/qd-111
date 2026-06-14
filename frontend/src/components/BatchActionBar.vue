<template>
  <div class="batch-action-bar" v-show="selected.length > 0">
    <span class="selected-count">已选择 <b>{{ selected.length }}</b> 项</span>
    <slot name="actions">
      <el-button type="danger" size="small" @click="handleBatchDelete">
        <el-icon><Delete /></el-icon>批量删除
      </el-button>
      <slot name="extra" />
    </slot>
    <el-button size="small" link @click="clearSelection">取消选择</el-button>
  </div>
</template>

<script setup>
const props = defineProps({
  selected: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['batch-delete', 'clear'])

const handleBatchDelete = () => {
  emit('batch-delete', props.selected)
}

const clearSelection = () => {
  emit('clear')
}
</script>

<style lang="scss" scoped>
.batch-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  margin-bottom: 16px;

  .selected-count {
    font-size: 13px;
    color: #606266;

    b {
      color: #409eff;
      margin: 0 2px;
    }
  }
}
</style>
