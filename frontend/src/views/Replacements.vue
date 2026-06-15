<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">更换周期登记</h2>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>登记更换
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索配件名称" clearable style="width: 200px" @input="handleSearch" />
      <el-select v-model="filters.accessoryId" placeholder="选择配件" clearable filterable style="width: 200px" @change="handleSearch">
        <el-option v-for="a in accessoryList" :key="a.id" :label="a.name" :value="a.id" />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="handleSearch"
      />
    </div>

    <BatchActionBar
      :selected="selectedRows"
      @batch-delete="handleBatchDelete"
      @clear="clearSelection"
    />

    <el-card class="card-shadow" shadow="never" body-style="padding: 0">
      <el-table
        ref="tableRef"
        :data="tableData"
        stripe
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="配件信息" min-width="180">
          <template #default="{ row }">
            <div class="accessory-cell">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                fit="cover"
                style="width: 36px; height: 36px; border-radius: 6px; margin-right: 10px; flex-shrink: 0"
              />
              <el-icon v-else :size="28" color="#c0c4cc" style="margin-right: 10px"><Goods /></el-icon>
              <div>
                <div class="accessory-name">{{ row.accessoryName }}</div>
                <div class="accessory-spec">{{ row.specification }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="instrumentName" label="适配乐器" width="100" />
        <el-table-column prop="replaceDate" label="更换日期" width="130" sortable />
        <el-table-column prop="usageDays" label="使用天数" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getUsageTagType(row)">{{ row.usageDays }}天</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="standardCycle" label="标准周期(天)" width="110" align="center" />
        <el-table-column label="周期对比" width="140">
          <template #default="{ row }">
            <el-progress
              :percentage="getCyclePercent(row)"
              :color="getCycleColor(row)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '登记更换' : '编辑更换记录'"
      width="560px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-section">
          <div class="form-section-title">更换信息</div>
          <el-form-item label="选择配件" prop="accessoryId">
            <el-select
              v-model="form.accessoryId"
              placeholder="请选择配件"
              filterable
              style="width: 100%"
              @change="handleAccessoryChange"
            >
              <el-option
                v-for="a in accessoryList"
                :key="a.id"
                :label="`${a.name} - ${a.specification}`"
                :value="a.id"
              />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="更换日期" prop="replaceDate">
                <el-date-picker
                  v-model="form.replaceDate"
                  type="date"
                  placeholder="选择更换日期"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="操作人">
                <el-input v-model="form.operator" placeholder="选填" maxlength="20" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="上次更换">
            <el-input v-model="lastReplaceInfo" disabled placeholder="系统自动计算" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="更换原因、安装位置等"
              maxlength="300"
              show-word-limit
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { replacementApi, accessoryApi } from '@/api'
import BatchActionBar from '@/components/BatchActionBar.vue'

const loading = ref(false)
const submitting = ref(false)
const tableRef = ref(null)
const formRef = ref(null)
const dialogVisible = ref(false)
const dialogMode = ref('add')
const selectedRows = ref([])
const accessoryList = ref([])
const historyList = ref([])

const filters = reactive({
  keyword: '',
  accessoryId: null,
  dateRange: []
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const form = reactive({
  id: null,
  accessoryId: null,
  replaceDate: '',
  operator: '',
  remark: ''
})

const rules = {
  accessoryId: [{ required: true, message: '请选择配件', trigger: 'change' }],
  replaceDate: [{ required: true, message: '请选择更换日期', trigger: 'change' }]
}

const lastReplaceInfo = computed(() => {
  if (!form.accessoryId || !form.replaceDate) return ''
  const excludeId = form.id
  let history = historyList.value.filter(h => h.accessoryId === form.accessoryId)
  if (excludeId) {
    history = history.filter(h => h.id !== excludeId)
  }
  const formDate = dayjs(form.replaceDate)
  const beforeCurrent = history.filter(h => dayjs(h.replaceDate).isBefore(formDate) || dayjs(h.replaceDate).isSame(formDate, 'day'))
  if (beforeCurrent.length === 0) {
    const acc = accessoryList.value.find(a => a.id === form.accessoryId)
    if (acc && acc.purchaseDate) {
      const days = formDate.diff(dayjs(acc.purchaseDate), 'day')
      return `${acc.purchaseDate}（购入日期，距本次 ${Math.max(days, 0)} 天）`
    }
    return '首次登记'
  }
  const last = beforeCurrent
    .map(h => h.replaceDate)
    .sort((a, b) => dayjs(b).valueOf() - dayjs(a).valueOf())[0]
  const days = formDate.diff(dayjs(last), 'day')
  return `${last}（距本次 ${days} 天）`
})

const loadAccessories = async () => {
  try {
    const res = await accessoryApi.list()
    accessoryList.value = res.data || res || []
  } catch {
    accessoryList.value = [
      { id: 1, name: '木吉他琴弦', specification: '012-053 磷铜覆膜', instrumentName: '木吉他', standardCycle: 90, imageUrl: '', purchaseDate: '2025-12-01' },
      { id: 2, name: '小提琴松香', specification: '无尘轻型 4/4', instrumentName: '小提琴', standardCycle: 180, imageUrl: '', purchaseDate: '2025-11-15' },
      { id: 3, name: '电吉他拨片', specification: '0.88mm 尼龙防滑', instrumentName: '电吉他', standardCycle: 60, imageUrl: '', purchaseDate: '2026-01-01' },
      { id: 4, name: '小提琴琴弓', specification: '4/4 巴西木 八角弓', instrumentName: '小提琴', standardCycle: 365, imageUrl: '', purchaseDate: '2025-06-01' },
      { id: 5, name: '吉他变调夹', specification: '弹簧式 金属款', instrumentName: '木吉他', standardCycle: 730, imageUrl: '', purchaseDate: '2024-01-15' },
      { id: 6, name: '指板清洁剂', specification: '柠檬油 100ml', instrumentName: '木吉他', standardCycle: 180, imageUrl: '', purchaseDate: '2025-09-20' }
    ]
  }
}

const loadHistory = async () => {
  try {
    const res = await replacementApi.list()
    historyList.value = res.data || res || []
  } catch {
    historyList.value = []
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      ...filters,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }
    const res = await replacementApi.page(params)
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

const loadMockList = () => {
  const accessoriesMap = {}
  accessoryList.value.forEach(a => { accessoriesMap[a.id] = a })
  tableData.value = [
    { id: 1, accessoryId: 1, accessoryName: '木吉他琴弦', specification: '012-053 磷铜覆膜', instrumentName: '木吉他', imageUrl: '', replaceDate: '2026-04-15', standardCycle: 90, usageDays: 60, operator: '本人', remark: '音色变闷，及时更换' },
    { id: 2, accessoryId: 2, accessoryName: '小提琴松香', specification: '无尘轻型 4/4', instrumentName: '小提琴', imageUrl: '', replaceDate: '2026-05-10', standardCycle: 180, usageDays: 35, operator: '本人', remark: '' },
    { id: 3, accessoryId: 3, accessoryName: '电吉他拨片', specification: '0.88mm 尼龙防滑', instrumentName: '电吉他', imageUrl: '', replaceDate: '2026-05-25', standardCycle: 60, usageDays: 20, operator: '本人', remark: '丢了一个，换新的' },
    { id: 4, accessoryId: 1, accessoryName: '木吉他琴弦', specification: '012-053 磷铜覆膜', instrumentName: '木吉他', imageUrl: '', replaceDate: '2026-01-20', standardCycle: 90, usageDays: 85, operator: '本人', remark: '使用近三月' },
    { id: 5, accessoryId: 6, accessoryName: '指板清洁剂', specification: '柠檬油 100ml', instrumentName: '木吉他', imageUrl: '', replaceDate: '2026-03-01', standardCycle: 180, usageDays: 105, operator: '本人', remark: '深度保养使用' }
  ]
  pagination.total = tableData.value.length
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadList()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    accessoryId: null,
    replaceDate: '',
    operator: '',
    remark: ''
  })
  formRef.value?.resetFields()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  form.replaceDate = dayjs().format('YYYY-MM-DD')
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogMode.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
  if (form.accessoryId) {
    try {
      const res = await replacementApi.history(form.accessoryId)
      historyList.value = res.data || res || []
    } catch {}
  }
}

const handleAccessoryChange = async (id) => {
  const acc = accessoryList.value.find(a => a.id === id)
  if (acc) {
    try {
      const res = await replacementApi.history(id)
      historyList.value = res.data || res || []
    } catch {}
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除此更换记录吗？', '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await replacementApi.remove([row.id])
      ElMessage.success('删除成功')
      loadList()
    } catch (e) {
      if (e?.message !== 'cancel') {
        ElMessage.error(e?.message || '删除失败，请稍后重试')
      }
    }
  }).catch(() => {})
}

const handleBatchDelete = (rows) => {
  ElMessageBox.confirm(`确定删除选中的 ${rows.length} 条记录吗？`, '批量删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await replacementApi.remove(rows.map(r => r.id))
      ElMessage.success('批量删除成功')
      clearSelection()
      loadList()
    } catch (e) {
      if (e?.message !== 'cancel') {
        ElMessage.error(e?.message || '批量删除失败，请稍后重试')
      }
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  const originalForm = { ...form }
  try {
    if (dialogMode.value === 'add') {
      await replacementApi.add(form)
    } else {
      await replacementApi.update(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
    loadHistory()
  } catch (err) {
    if (err?.message === '校验不通过') {
      return
    }
    if (dialogMode.value === 'edit') {
      Object.assign(form, originalForm)
    }
    ElMessage.error(err?.message || '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  resetForm()
}

const getUsageTagType = (row) => {
  if (row.usageDays >= row.standardCycle) return 'danger'
  if (row.usageDays >= row.standardCycle * 0.8) return 'warning'
  return 'success'
}

const getCyclePercent = (row) => {
  if (!row.standardCycle) return 0
  const pct = Math.round((row.usageDays / row.standardCycle) * 100)
  return Math.min(pct, 100)
}

const getCycleColor = (row) => {
  const pct = getCyclePercent(row)
  if (pct >= 100) return '#f56c6c'
  if (pct >= 80) return '#e6a23c'
  return '#67c23a'
}

onMounted(() => {
  loadAccessories().then(() => {
    loadHistory()
    loadList()
  })
})
</script>

<style lang="scss" scoped>
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}
</style>
