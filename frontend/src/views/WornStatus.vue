<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">损耗状态标注</h2>
      <div class="table-toolbar">
        <el-dropdown trigger="click" @command="handleBatchMark" v-show="selectedRows.length > 0">
          <el-button type="primary">
            <el-icon><Rank /></el-icon>批量标注 ({{ selectedRows.length }})
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="w in wornStatuses" :key="w.code" :command="w.code">
                <span class="status-option">
                  <span class="status-dot" :style="{ background: getWornColor(w.code) }"></span>
                  {{ w.label }}
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-card class="card-shadow heatmap-card" shadow="never" body-style="padding: 20px">
      <div class="heatmap-card-header">
        <div class="heatmap-title">
          <el-icon color="#f56c6c"><DataAnalysis /></el-icon>
          <span class="title-text">损耗热区分布</span>
          <span class="title-subtitle">按乐器×配件类型交叉呈现损耗状态</span>
        </div>
      </div>
      <WornHeatmap :data="heatmapData" :loading="heatmapLoading" :showLegend="true" />
    </el-card>

    <el-row :gutter="16" class="status-stats">
      <el-col v-for="status in statusOverview" :key="status.code" :xs="12" :sm="6">
        <div
          class="status-card"
          :class="{ active: activeStatus === status.code }"
          @click="toggleStatusFilter(status.code)"
        >
          <div class="status-indicator" :style="{ background: status.color }"></div>
          <div class="status-info">
            <div class="status-count">{{ status.count }}</div>
            <div class="status-label">{{ status.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索名称/规格" clearable style="width: 200px" @input="handleSearch" />
      <el-select v-model="filters.groupId" placeholder="物资分组" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-select v-model="filters.typeCode" placeholder="配件类型" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="t in accessoryTypes" :key="t.code" :label="t.label" :value="t.code" />
      </el-select>
      <el-select v-model="filters.instrument" placeholder="适配乐器" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="ins in instrumentList" :key="ins.code" :label="ins.label" :value="ins.code" />
      </el-select>
      <el-button @click="clearFilters">
        <el-icon><RefreshLeft /></el-icon>重置筛选
      </el-button>
    </div>

    <div class="batch-select-bar">
      <span class="batch-select-label">快速选择：</span>
      <el-dropdown trigger="click" @command="selectByInstrument">
        <el-button size="small" plain>
          <el-icon><Tickets /></el-icon>按乐器全选<el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="ins in instrumentList" :key="ins.code" :command="ins.code">
              {{ ins.label }}
              <span class="batch-count-hint">({{ getInstrumentCount(ins.code) }})</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown trigger="click" @command="selectByGroup">
        <el-button size="small" plain>
          <el-icon><FolderOpened /></el-icon>按分组全选<el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="g in groupList" :key="g.id" :command="g.id">
              {{ g.name }}
              <span class="batch-count-hint">({{ getGroupCount(g.id) }})</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button size="small" plain @click="selectAllCurrent">
        <el-icon><Check /></el-icon>选择当前页全部
      </el-button>
      <el-button size="small" plain type="danger" @click="clearSelection" v-show="selectedRows.length > 0">
        <el-icon><Close /></el-icon>取消选择 ({{ selectedRows.length }})
      </el-button>
    </div>

    <el-card class="card-shadow" shadow="never" body-style="padding: 0">
      <el-table
        ref="tableRef"
        :data="tableData"
        stripe
        style="width: 100%"
        v-loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="配件" min-width="180">
          <template #default="{ row }">
            <div class="accessory-cell">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                fit="cover"
                style="width: 40px; height: 40px; border-radius: 6px; margin-right: 12px; flex-shrink: 0"
              />
              <el-icon v-else :size="32" color="#c0c4cc" style="margin-right: 12px"><Goods /></el-icon>
              <div>
                <div class="accessory-name">{{ row.name }}</div>
                <div class="accessory-spec">{{ row.specification }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="配件类型" width="110" />
        <el-table-column prop="instrumentName" label="适配乐器" width="100" />
        <el-table-column prop="groupName" label="所属分组" width="110" />
        <el-table-column label="当前损耗状态" width="180" align="center">
          <template #default="{ row }">
            <el-tag :type="getWornTagType(row.wornStatus)" effect="light" size="large">
              <span class="tag-dot" :style="{ background: getWornColor(row.wornStatus) }"></span>
              {{ getWornLabel(row.wornStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="购入时间" width="120" />
        <el-table-column label="使用状态" width="140">
          <template #default="{ row }">
            <span v-if="row.usageDays">
              <el-tag size="small" type="info">已使用 {{ row.usageDays }} 天</el-tag>
            </span>
            <span v-else>
              <el-tag size="small" type="info">暂无记录</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="标注状态" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-radio-group
              :model-value="row.wornStatus"
              size="small"
              @update:model-value="(val) => handleQuickMark(row, val)"
            >
              <el-radio-button
                v-for="w in wornStatuses"
                :key="w.code"
                :value="w.code"
                :label="w.code"
              >
                {{ w.label }}
              </el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="confirmDialogVisible"
      title="批量标注确认"
      width="720px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="confirm-dialog-body">
        <div class="confirm-summary-header">
          <el-icon :size="20" color="#e6a23c"><WarningFilled /></el-icon>
          <span>即将将 <b>{{ pendingBatchRows.length }}</b> 个配件的状态统一标注为</span>
          <el-tag :type="getWornTagType(pendingBatchStatus)" effect="dark" size="large">
            <span class="tag-dot" :style="{ background: getWornColor(pendingBatchStatus) }"></span>
            {{ getWornLabel(pendingBatchStatus) }}
          </el-tag>
        </div>

        <div class="confirm-change-summary">
          <div class="change-summary-title">状态变化摘要</div>
          <div class="change-summary-grid">
            <div
              v-for="change in batchChangeSummary"
              :key="change.from"
              class="change-summary-item"
            >
              <div class="change-from">
                <span class="change-label">原状态</span>
                <el-tag :type="getWornTagType(change.from)" effect="light" size="small">
                  {{ getWornLabel(change.from) }}
                </el-tag>
              </div>
              <div class="change-arrow">
                <el-icon><Right /></el-icon>
              </div>
              <div class="change-to">
                <span class="change-label">新状态</span>
                <el-tag :type="getWornTagType(pendingBatchStatus)" effect="dark" size="small">
                  {{ getWornLabel(pendingBatchStatus) }}
                </el-tag>
              </div>
              <div class="change-count">
                <span class="count-num">{{ change.count }}</span>
                <span class="count-label">个配件</span>
              </div>
            </div>
          </div>
          <div class="change-summary-note" v-if="unchangedCount > 0">
            其中 {{ unchangedCount }} 个配件状态无变化
          </div>
        </div>

        <div class="confirm-list-section">
          <div class="confirm-list-title">
            受影响配件清单
            <span class="confirm-list-count">共 {{ affectedRows.length }} 项</span>
          </div>
          <div class="confirm-list-scroll">
            <div
              v-for="row in affectedRows"
              :key="row.id"
              class="confirm-list-item"
            >
              <div class="confirm-item-main">
                <span class="confirm-item-name">{{ row.name }}</span>
                <span class="confirm-item-spec">{{ row.specification }}</span>
              </div>
              <div class="confirm-item-meta">
                <span class="confirm-item-instrument">{{ row.instrumentName }}</span>
                <span class="confirm-item-group">{{ row.groupName }}</span>
              </div>
              <div class="confirm-item-status-change">
                <el-tag :type="getWornTagType(row.wornStatus)" effect="light" size="small">
                  {{ getWornLabel(row.wornStatus) }}
                </el-tag>
                <el-icon class="change-arrow-icon"><Right /></el-icon>
                <el-tag :type="getWornTagType(pendingBatchStatus)" effect="dark" size="small">
                  {{ getWornLabel(pendingBatchStatus) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeBatchMark" :loading="batchSubmitting">
          确认标注
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Rank, ArrowDown, Tickets, FolderOpened, Check, Close, WarningFilled, Right } from '@element-plus/icons-vue'
import { accessoryApi, dictApi, groupApi, dashboardApi } from '@/api'
import WornHeatmap from '@/components/WornHeatmap.vue'

const loading = ref(false)
const tableRef = ref(null)
const selectedRows = ref([])
const groupList = ref([])
const accessoryTypes = ref([])
const instrumentList = ref([])
const activeStatus = ref('')
const allAccessories = ref([])
const heatmapData = ref({})
const heatmapLoading = ref(false)

const confirmDialogVisible = ref(false)
const pendingBatchStatus = ref('')
const pendingBatchRows = ref([])
const batchSubmitting = ref(false)

const wornStatuses = ref([
  { code: 'good', label: '完好', color: '#67c23a' },
  { code: 'slight', label: '轻微磨损', color: '#e6a23c' },
  { code: 'severe', label: '严重损耗', color: '#f56c6c' },
  { code: 'broken', label: '已损坏', color: '#909399' }
])

const filters = reactive({
  keyword: '',
  groupId: null,
  typeCode: '',
  wornStatus: '',
  instrument: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const statusOverview = computed(() => {
  const counts = { good: 0, slight: 0, severe: 0, broken: 0 }
  allAccessories.value.forEach(row => {
    if (counts[row.wornStatus] !== undefined) counts[row.wornStatus]++
  })
  return wornStatuses.value.map(w => ({
    ...w,
    count: counts[w.code] || 0
  }))
})

const affectedRows = computed(() => {
  return pendingBatchRows.value.filter(r => r.wornStatus !== pendingBatchStatus.value)
})

const unchangedCount = computed(() => {
  return pendingBatchRows.value.filter(r => r.wornStatus === pendingBatchStatus.value).length
})

const batchChangeSummary = computed(() => {
  const map = {}
  affectedRows.value.forEach(r => {
    if (!map[r.wornStatus]) map[r.wornStatus] = 0
    map[r.wornStatus]++
  })
  return Object.entries(map).map(([from, count]) => ({ from, count }))
})

const getInstrumentCount = (code) => {
  return tableData.value.filter(r => r.instrument === code).length
}

const getGroupCount = (groupId) => {
  return tableData.value.filter(r => r.groupId === groupId).length
}

const selectByInstrument = (instrumentCode) => {
  tableRef.value.clearSelection()
  nextTick(() => {
    tableData.value.forEach(row => {
      if (row.instrument === instrumentCode) {
        tableRef.value.toggleRowSelection(row, true)
      }
    })
  })
}

const selectByGroup = (groupId) => {
  tableRef.value.clearSelection()
  nextTick(() => {
    tableData.value.forEach(row => {
      if (row.groupId === groupId) {
        tableRef.value.toggleRowSelection(row, true)
      }
    })
  })
}

const selectAllCurrent = () => {
  tableData.value.forEach(row => {
    tableRef.value.toggleRowSelection(row, true)
  })
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
}

const loadDict = async () => {
  try {
    const [t, w, g] = await Promise.all([
      dictApi.accessoryTypes(),
      dictApi.wornStatuses(),
      groupApi.list()
    ])
    accessoryTypes.value = t.data || t || []
    if (w.data || w) wornStatuses.value = w.data || w
    groupList.value = g.data || g || []
  } catch {}
  if (accessoryTypes.value.length === 0) {
    accessoryTypes.value = [
      { code: 'string', label: '琴弦' },
      { code: 'bow', label: '琴弓' },
      { code: 'pick', label: '拨片' },
      { code: 'rosin', label: '松香' },
      { code: 'capo', label: '变调夹' },
      { code: 'strap', label: '背带' },
      { code: 'cleaner', label: '清洁用品' },
      { code: 'other', label: '其他' }
    ]
  }
  if (groupList.value.length === 0) {
    groupList.value = [
      { id: 1, name: '弹奏配件' },
      { id: 2, name: '辅助工具' },
      { id: 3, name: '养护耗材' }
    ]
  }
  try {
    const insRes = await dictApi.instruments()
    instrumentList.value = insRes.data || insRes || []
  } catch {}
  if (instrumentList.value.length === 0) {
    instrumentList.value = [
      { code: 'guitar-acoustic', label: '木吉他' },
      { code: 'guitar-electric', label: '电吉他' },
      { code: 'guitar-bass', label: '贝斯' },
      { code: 'violin', label: '小提琴' },
      { code: 'ukulele', label: '尤克里里' },
      { code: 'erhu', label: '二胡' }
    ]
  }
}

const loadHeatmap = async () => {
  heatmapLoading.value = true
  try {
    const res = await dashboardApi.wornHeatmap()
    if (res && res.data) {
      heatmapData.value = res.data
    } else {
      heatmapData.value = {}
    }
  } catch {
    heatmapData.value = {}
  } finally {
    heatmapLoading.value = false
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await accessoryApi.page({
      ...filters,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res && res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || tableData.value.length
    } else {
      tableData.value = []
      pagination.total = 0
    }
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const loadAllList = async () => {
  try {
    const res = await accessoryApi.list({
      keyword: filters.keyword,
      groupId: filters.groupId,
      typeCode: filters.typeCode
    })
    if (res && res.data) {
      allAccessories.value = res.data.records || res.data.list || res.data || []
    } else {
      allAccessories.value = []
    }
  } catch {
    allAccessories.value = []
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadAllList()
  loadList()
}

const clearFilters = () => {
  filters.keyword = ''
  filters.groupId = null
  filters.typeCode = ''
  filters.wornStatus = ''
  filters.instrument = ''
  activeStatus.value = ''
  handleSearch()
}

const toggleStatusFilter = (code) => {
  activeStatus.value = activeStatus.value === code ? '' : code
  filters.wornStatus = activeStatus.value
  pagination.pageNum = 1
  loadList()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleQuickMark = async (row, status) => {
  const originalStatus = row.wornStatus
  try {
    await accessoryApi.updateStatus(row.id, status)
    ElMessage.success(`已更新为「${getWornLabel(status)}」`)
    row.wornStatus = status
    const allRow = allAccessories.value.find(a => a.id === row.id)
    if (allRow) allRow.wornStatus = status
    await loadHeatmap()
  } catch (e) {
    row.wornStatus = originalStatus
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '标注失败，请稍后重试')
    }
  }
}

const handleBatchMark = (code) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择配件')
    return
  }
  pendingBatchStatus.value = code
  pendingBatchRows.value = [...selectedRows.value]
  confirmDialogVisible.value = true
}

const executeBatchMark = async () => {
  batchSubmitting.value = true
  const originalStatuses = pendingBatchRows.value.map(r => ({ id: r.id, wornStatus: r.wornStatus }))
  const ids = pendingBatchRows.value.map(r => r.id)
  const code = pendingBatchStatus.value
  try {
    await accessoryApi.batchUpdateStatus(ids, code)
    ElMessage.success('批量标注成功')
    tableRef.value.clearSelection()
    confirmDialogVisible.value = false
    await Promise.all([
      loadAllList(),
      loadList(),
      loadHeatmap()
    ])
  } catch (e) {
    originalStatuses.forEach(os => {
      const row = tableData.value.find(r => r.id === os.id)
      if (row) row.wornStatus = os.wornStatus
    })
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '批量标注失败，请稍后重试')
    }
  } finally {
    batchSubmitting.value = false
  }
}

const getWornLabel = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.label : code
}

const getWornColor = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.color : '#909399'
}

const getWornTagType = (code) => {
  const map = { good: 'success', slight: 'warning', severe: 'danger', broken: 'info' }
  return map[code] || 'info'
}

onMounted(() => {
  loadDict()
  loadAllList()
  loadList()
  loadHeatmap()
})
</script>

<style lang="scss" scoped>
.status-stats {
  margin-bottom: 16px;
}

.status-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.25s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }

  &.active {
    border-color: #409eff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  }

  .status-indicator {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    opacity: 0.15;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      inset: 12px;
      border-radius: 50%;
      background: inherit;
      opacity: 1;
    }
  }

  .status-info {
    .status-count {
      font-size: 24px;
      font-weight: 700;
      color: #303133;
      line-height: 1.1;
    }
    .status-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.accessory-cell {
  display: flex;
  align-items: center;

  .accessory-name {
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  }
  .accessory-spec {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}

.status-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 5px;
  vertical-align: middle;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}

.heatmap-card {
  margin-bottom: 16px;

  .heatmap-card-header {
    margin-bottom: 16px;
  }

  .heatmap-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;

    .title-text {
      font-size: 16px;
    }

    .title-subtitle {
      font-size: 12px;
      font-weight: 400;
      color: #909399;
      margin-left: 8px;
    }
  }
}

.batch-select-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  flex-wrap: wrap;

  .batch-select-label {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
    white-space: nowrap;
  }
}

.batch-count-hint {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.confirm-dialog-body {
  .confirm-summary-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #606266;
    padding: 14px 16px;
    background: #fdf6ec;
    border: 1px solid #f5dab1;
    border-radius: 8px;
    margin-bottom: 20px;

    b {
      color: #e6a23c;
      font-size: 18px;
    }
  }

  .confirm-change-summary {
    margin-bottom: 20px;

    .change-summary-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
      padding-left: 8px;
      border-left: 3px solid #409eff;
    }

    .change-summary-grid {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .change-summary-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      background: #fafbfc;
      border: 1px solid #ebeef5;
      border-radius: 6px;

      .change-from,
      .change-to {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;

        .change-label {
          font-size: 11px;
          color: #909399;
        }
      }

      .change-arrow {
        color: #c0c4cc;
        font-size: 16px;
      }

      .change-count {
        margin-left: auto;
        display: flex;
        align-items: baseline;
        gap: 4px;

        .count-num {
          font-size: 20px;
          font-weight: 700;
          color: #f56c6c;
        }

        .count-label {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .change-summary-note {
      font-size: 12px;
      color: #909399;
      margin-top: 8px;
      padding-left: 4px;
    }
  }

  .confirm-list-section {
    .confirm-list-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
      padding-left: 8px;
      border-left: 3px solid #f56c6c;
      display: flex;
      align-items: center;
      gap: 8px;

      .confirm-list-count {
        font-size: 12px;
        font-weight: 400;
        color: #909399;
      }
    }

    .confirm-list-scroll {
      max-height: 280px;
      overflow-y: auto;
      border: 1px solid #ebeef5;
      border-radius: 6px;
    }

    .confirm-list-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      border-bottom: 1px solid #ebeef5;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background: #f5f7fa;
      }

      .confirm-item-main {
        flex: 1;
        min-width: 0;

        .confirm-item-name {
          font-size: 13px;
          font-weight: 500;
          color: #303133;
          display: block;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .confirm-item-spec {
          font-size: 11px;
          color: #909399;
          display: block;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .confirm-item-meta {
        display: flex;
        gap: 8px;
        flex-shrink: 0;

        .confirm-item-instrument,
        .confirm-item-group {
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 4px;
          background: #f0f2f5;
          color: #606266;
        }
      }

      .confirm-item-status-change {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;

        .change-arrow-icon {
          color: #c0c4cc;
          font-size: 14px;
        }
      }
    }
  }
}
</style>
