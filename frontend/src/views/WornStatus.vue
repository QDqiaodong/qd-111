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
      <el-button @click="clearFilters">
        <el-icon><RefreshLeft /></el-icon>重置筛选
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
              v-model="row.wornStatus"
              size="small"
              @change="(val) => handleQuickMark(row, val)"
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { accessoryApi, dictApi, groupApi } from '@/api'

const loading = ref(false)
const tableRef = ref(null)
const selectedRows = ref([])
const groupList = ref([])
const accessoryTypes = ref([])
const activeStatus = ref('')
const allAccessories = ref([])

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
  wornStatus: ''
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
      loadMockList()
    }
  } catch {
    loadMockList()
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
      loadMockAllList()
    }
  } catch {
    loadMockAllList()
  }
}

const loadMockAllList = () => {
  allAccessories.value = [
    { id: 1, wornStatus: 'slight' },
    { id: 2, wornStatus: 'good' },
    { id: 3, wornStatus: 'good' },
    { id: 4, wornStatus: 'slight' },
    { id: 5, wornStatus: 'good' },
    { id: 6, wornStatus: 'severe' },
    { id: 7, wornStatus: 'broken' },
    { id: 8, wornStatus: 'good' }
  ]
}

const loadMockList = () => {
  tableData.value = [
    { id: 1, name: '木吉他琴弦', specification: '012-053 磷铜覆膜', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 1, groupName: '弹奏配件', wornStatus: 'slight', purchaseDate: '2026-04-01', imageUrl: '', usageDays: 74 },
    { id: 2, name: '小提琴松香', specification: '无尘轻型 4/4', typeCode: 'rosin', typeName: '松香', instrument: 'violin', instrumentName: '小提琴', groupId: 3, groupName: '养护耗材', wornStatus: 'good', purchaseDate: '2026-05-01', imageUrl: '', usageDays: 44 },
    { id: 3, name: '电吉他拨片', specification: '0.88mm 尼龙防滑', typeCode: 'pick', typeName: '拨片', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, groupName: '弹奏配件', wornStatus: 'good', purchaseDate: '2026-05-10', imageUrl: '', usageDays: 35 },
    { id: 4, name: '小提琴琴弓', specification: '4/4 巴西木 八角弓', typeCode: 'bow', typeName: '琴弓', instrument: 'violin', instrumentName: '小提琴', groupId: 1, groupName: '弹奏配件', wornStatus: 'slight', purchaseDate: '2026-01-15', imageUrl: '', usageDays: 150 },
    { id: 5, name: '吉他变调夹', specification: '弹簧式 金属款', typeCode: 'capo', typeName: '变调夹', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 2, groupName: '辅助工具', wornStatus: 'good', purchaseDate: '2025-11-20', imageUrl: '', usageDays: 206 },
    { id: 6, name: '指板清洁剂', specification: '柠檬油 100ml', typeCode: 'cleaner', typeName: '清洁用品', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 3, groupName: '养护耗材', wornStatus: 'severe', purchaseDate: '2025-08-01', imageUrl: '', usageDays: 317 },
    { id: 7, name: '贝斯琴弦', specification: '045-105 镍钢', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-bass', instrumentName: '贝斯', groupId: 1, groupName: '弹奏配件', wornStatus: 'broken', purchaseDate: '2025-06-01', imageUrl: '', usageDays: 378 },
    { id: 8, name: '尤克里里琴弦', specification: '碳素 高音C', typeCode: 'string', typeName: '琴弦', instrument: 'ukulele', instrumentName: '尤克里里', groupId: 1, groupName: '弹奏配件', wornStatus: 'good', purchaseDate: '2026-03-15', imageUrl: '', usageDays: 91 }
  ]
  pagination.total = tableData.value.length
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
    const allRow = allAccessories.value.find(a => a.id === row.id)
    if (allRow) allRow.wornStatus = status
  } catch (e) {
    row.wornStatus = originalStatus
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '标注失败，请稍后重试')
    }
  }
}

const handleBatchMark = async (code) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择配件')
    return
  }
  ElMessageBox.confirm(
    `确定将选中的 ${selectedRows.value.length} 个配件标注为「${getWornLabel(code)}」吗？`,
    '批量标注',
    { type: 'warning', confirmButtonText: '确定标注', cancelButtonText: '取消' }
  ).then(async () => {
    const originalStatuses = selectedRows.value.map(r => ({ id: r.id, wornStatus: r.wornStatus }))
    try {
      await accessoryApi.batchUpdateStatus(selectedRows.value.map(r => r.id), code)
      ElMessage.success('批量标注成功')
      tableRef.value.clearSelection()
      loadAllList()
      loadList()
    } catch (e) {
      originalStatuses.forEach(os => {
        const row = tableData.value.find(r => r.id === os.id)
        if (row) row.wornStatus = os.wornStatus
      })
      if (e?.message !== 'cancel') {
        ElMessage.error(e?.message || '批量标注失败，请稍后重试')
      }
    }
  }).catch(() => {})
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
</style>
