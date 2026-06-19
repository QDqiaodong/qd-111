<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">更换前准备清单</h2>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleGenerate">
          <el-icon><Plus /></el-icon>生成准备清单
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索配件或模板名称" clearable style="width: 220px" @input="handleSearch" />
      <el-select v-model="filters.accessoryId" placeholder="选择配件" clearable filterable style="width: 200px" @change="handleSearch">
        <el-option v-for="a in accessoryList" :key="a.id" :label="a.name" :value="a.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="选择状态" clearable style="width: 140px" @change="handleSearch">
        <el-option label="待开始" value="pending" />
        <el-option label="进行中" value="in_progress" />
        <el-option label="已完成" value="completed" />
      </el-select>
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
              <el-icon :size="28" color="#c0c4cc" style="margin-right: 10px"><Goods /></el-icon>
              <div>
                <div class="accessory-name">{{ row.accessoryName }}</div>
                <div class="accessory-spec">{{ row.typeName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="templateName" label="清单模板" min-width="160" />
        <el-table-column label="完成进度" min-width="180">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="getProgressPercent(row)"
                :color="getProgressColor(row)"
                :stroke-width="8"
                style="flex: 1; margin-right: 10px"
              />
              <span class="progress-text">{{ row.completedCount }}/{{ row.totalCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="必做项" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.requiredCompletedCount >= row.requiredTotalCount ? 'success' : 'warning'">
              {{ row.requiredCompletedCount }}/{{ row.requiredTotalCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="success" link size="small" v-if="row.status === 'pending'" @click="handleStart(row)">开始</el-button>
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
      v-model="generateDialogVisible"
      title="生成准备清单"
      width="500px"
      destroy-on-close
      @close="handleGenerateDialogClose"
    >
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="100px">
        <el-form-item label="选择配件" prop="accessoryId">
          <el-select
            v-model="generateForm.accessoryId"
            placeholder="请选择要更换的配件"
            filterable
            style="width: 100%"
            @change="handleAccessorySelect"
          >
            <el-option
              v-for="a in accessoryList"
              :key="a.id"
              :label="`${a.name} - ${a.typeName}`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对应模板">
          <el-input v-model="matchedTemplateName" disabled placeholder="系统自动匹配" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="generateForm.operator" placeholder="选填" maxlength="20" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="generateForm.remark"
            type="textarea"
            :rows="2"
            placeholder="选填"
            maxlength="300"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenerateSubmit" :loading="submitting">生成清单</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      :title="`准备清单 - ${currentChecklist?.templateName || ''}`"
      width="700px"
      destroy-on-close
      @close="handleDetailDialogClose"
    >
      <div v-if="currentChecklist" class="checklist-detail">
        <div class="checklist-header">
          <div class="checklist-info">
            <div class="info-row">
              <span class="info-label">配件：</span>
              <span class="info-value">{{ currentChecklist.accessoryName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">类型：</span>
              <span class="info-value">{{ currentChecklist.typeName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">总进度：</span>
              <span class="info-value">
                <el-tag size="small" :type="getStatusTagType(currentChecklist.status)">
                  {{ currentChecklist.statusName }}
                </el-tag>
                <span style="margin-left: 8px">{{ currentChecklist.completedCount }}/{{ currentChecklist.totalCount }} 项</span>
              </span>
            </div>
          </div>
          <div class="checklist-actions" v-if="currentChecklist.status !== 'completed'">
            <el-button
              type="primary"
              size="small"
              v-if="currentChecklist.status === 'pending'"
              @click="handleStartCurrent"
            >
              <el-icon><VideoPlay /></el-icon>开始清单
            </el-button>
            <el-button
              type="success"
              size="small"
              v-if="currentChecklist.status === 'in_progress'"
              :disabled="currentChecklist.requiredCompletedCount < currentChecklist.requiredTotalCount"
              @click="handleCompleteCurrent"
            >
              <el-icon><CircleCheck /></el-icon>完成清单
            </el-button>
          </div>
        </div>

        <div class="progress-bar-section">
          <el-progress
            :percentage="getProgressPercent(currentChecklist)"
            :color="getProgressColor(currentChecklist)"
            :stroke-width="12"
          />
          <div class="progress-stats">
            <span>必做项：<b>{{ currentChecklist.requiredCompletedCount }}/{{ currentChecklist.requiredTotalCount }}</b></span>
            <span>选做项：<b>{{ currentChecklist.completedCount - currentChecklist.requiredCompletedCount }}/{{ currentChecklist.totalCount - currentChecklist.requiredTotalCount }}</b></span>
          </div>
        </div>

        <div class="category-list" v-loading="detailLoading">
          <div v-for="category in categoryData" :key="category.category" class="category-section">
            <div class="category-header">
              <div class="category-title">
                <el-icon :size="18" :color="getCategoryIconColor(category.category)">
                  <component :is="getCategoryIcon(category.category)" />
                </el-icon>
                <span>{{ category.categoryName }}</span>
              </div>
              <el-tag size="small" :type="category.completedCount >= category.totalCount ? 'success' : 'info'">
                {{ category.completedCount }}/{{ category.totalCount }}
              </el-tag>
            </div>
            <div class="item-list">
              <div
                v-for="item in category.items"
                :key="item.id"
                class="checklist-item"
                :class="{ 'is-completed': item.completed === 1, 'is-required': item.required === 1 }"
              >
                <div class="item-checkbox" @click="toggleItemComplete(item)">
                  <el-checkbox v-model="item.completed" :label="1" @change="() => handleItemComplete(item)">
                  </el-checkbox>
                </div>
                <div class="item-content">
                  <div class="item-name">
                    {{ item.name }}
                    <el-tag v-if="item.required === 1" size="small" type="danger" effect="light" style="margin-left: 8px">必做</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain" style="margin-left: 8px">选做</el-tag>
                  </div>
                  <div class="item-desc" v-if="item.description">{{ item.description }}</div>
                  <div class="item-meta" v-if="item.completed === 1">
                    <el-icon :size="12" color="#67c23a"><Clock /></el-icon>
                    <span>{{ item.completedTime }}</span>
                    <span v-if="item.completedBy" style="margin-left: 12px">
                      <el-icon :size="12" color="#67c23a"><User /></el-icon>
                      {{ item.completedBy }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Goods, VideoPlay, CircleCheck, Clock, User,
  Tools, Brush, Setting, View, Box
} from '@element-plus/icons-vue'
import { preparationChecklistApi, accessoryApi } from '@/api'
import BatchActionBar from '@/components/BatchActionBar.vue'

const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const tableRef = ref(null)
const selectedRows = ref([])
const accessoryList = ref([])

const filters = reactive({
  keyword: '',
  accessoryId: null,
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const generateDialogVisible = ref(false)
const generateFormRef = ref(null)
const generateForm = reactive({
  accessoryId: null,
  operator: '',
  remark: ''
})
const generateRules = {
  accessoryId: [{ required: true, message: '请选择配件', trigger: 'change' }]
}
const matchedTemplateName = ref('')

const detailDialogVisible = ref(false)
const currentChecklist = ref(null)
const categoryData = ref([])

const categoryIcons = {
  tool: Tools,
  clean: Brush,
  adjust: Setting,
  check: View,
  change: Box,
  other: Goods
}

const categoryColors = {
  tool: '#409eff',
  clean: '#67c23a',
  adjust: '#e6a23c',
  check: '#909399',
  change: '#f56c6c',
  other: '#909399'
}

const loadAccessories = async () => {
  try {
    const res = await accessoryApi.list()
    accessoryList.value = res.data || res || []
  } catch {
    accessoryList.value = []
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
    const res = await preparationChecklistApi.page(params)
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

const handleGenerate = () => {
  generateForm.accessoryId = null
  generateForm.operator = ''
  generateForm.remark = ''
  matchedTemplateName.value = ''
  generateDialogVisible.value = true
}

const handleAccessorySelect = async (id) => {
  if (!id) {
    matchedTemplateName.value = ''
    return
  }
  const acc = accessoryList.value.find(a => a.id === id)
  if (acc) {
    try {
      const res = await preparationTemplateApi.getByTypeCode(acc.typeCode)
      if (res && res.data) {
        matchedTemplateName.value = res.data.name || ''
      } else {
        matchedTemplateName.value = '该类型暂无模板'
      }
    } catch (e) {
      matchedTemplateName.value = '该类型暂无模板'
    }
  }
}

const handleGenerateSubmit = async () => {
  await generateFormRef.value.validate()
  submitting.value = true
  try {
    const res = await preparationChecklistApi.generate(generateForm)
    if (res && res.data) {
      ElMessage.success('清单生成成功')
      generateDialogVisible.value = false
      loadList()
      setTimeout(() => {
        handleView(res.data)
      }, 300)
    }
  } catch (e) {
    ElMessage.error(e?.message || '生成失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleGenerateDialogClose = () => {
  generateFormRef.value?.resetFields()
}

const handleView = async (row) => {
  currentChecklist.value = row
  detailDialogVisible.value = true
  loadDetailData(row.id)
}

const loadDetailData = async (id) => {
  detailLoading.value = true
  try {
    const res = await preparationChecklistApi.getChecklistWithCategories(id)
    if (res && res.data) {
      categoryData.value = res.data
    }
    const res2 = await preparationChecklistApi.getById(id)
    if (res2 && res2.data) {
      currentChecklist.value = res2.data
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleStart = async (row) => {
  try {
    await ElMessageBox.confirm(`确定开始「${row.templateName}」吗？`, '确认开始', {
      type: 'primary',
      confirmButtonText: '开始',
      cancelButtonText: '取消'
    })
    const res = await preparationChecklistApi.startChecklist(row.id)
    if (res && res.data) {
      ElMessage.success('清单已开始')
      loadList()
      if (detailDialogVisible.value) {
        loadDetailData(row.id)
      }
    }
  } catch (e) {
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

const handleStartCurrent = () => {
  handleStart(currentChecklist.value)
}

const handleCompleteCurrent = async () => {
  try {
    await ElMessageBox.confirm('确定完成此清单吗？完成后将无法修改。', '确认完成', {
      type: 'success',
      confirmButtonText: '完成',
      cancelButtonText: '取消'
    })
    const res = await preparationChecklistApi.completeChecklist(currentChecklist.value.id)
    if (res && res.data) {
      ElMessage.success('清单已完成')
      loadList()
      loadDetailData(currentChecklist.value.id)
    }
  } catch (e) {
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

const toggleItemComplete = (item) => {
  if (currentChecklist.value.status === 'completed') {
    return
  }
  if (currentChecklist.value.status === 'pending') {
    ElMessage.warning('请先开始清单')
    return
  }
  item.completed = item.completed === 1 ? 0 : 1
}

const handleItemComplete = async (item) => {
  if (currentChecklist.value.status !== 'in_progress') {
    return
  }
  try {
    const res = await preparationChecklistApi.completeItem({
      itemId: item.id,
      completed: item.completed,
      completedBy: generateForm.operator || '本人'
    })
    if (res && res.data) {
      currentChecklist.value = res.data
      loadDetailData(currentChecklist.value.id)
      loadList()
    }
  } catch (e) {
    item.completed = item.completed === 1 ? 0 : 1
    ElMessage.error(e?.message || '更新失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除此准备清单吗？', '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await preparationChecklistApi.remove([row.id])
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
      await preparationChecklistApi.remove(rows.map(r => r.id))
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

const handleDetailDialogClose = () => {
  currentChecklist.value = null
  categoryData.value = []
}

const getProgressPercent = (row) => {
  if (!row.totalCount) return 0
  return Math.round((row.completedCount / row.totalCount) * 100)
}

const getProgressColor = (row) => {
  const pct = getProgressPercent(row)
  if (pct >= 100) return '#67c23a'
  if (pct >= 80) return '#e6a23c'
  return '#409eff'
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'pending': return 'info'
    case 'in_progress': return 'warning'
    case 'completed': return 'success'
    default: return 'info'
  }
}

const getCategoryIcon = (category) => {
  return categoryIcons[category] || Goods
}

const getCategoryIconColor = (category) => {
  return categoryColors[category] || '#909399'
}

onMounted(() => {
  loadAccessories()
  loadList()
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

.progress-cell {
  display: flex;
  align-items: center;

  .progress-text {
    font-size: 12px;
    color: #606266;
    flex-shrink: 0;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}

.checklist-detail {
  .checklist-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 16px 20px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 16px;

    .info-row {
      margin-bottom: 8px;
      &:last-child { margin-bottom: 0; }

      .info-label {
        color: #909399;
        font-size: 13px;
      }
      .info-value {
        color: #303133;
        font-size: 14px;
        font-weight: 500;
      }
    }
  }

  .progress-bar-section {
    margin-bottom: 20px;

    .progress-stats {
      display: flex;
      justify-content: space-around;
      margin-top: 8px;
      font-size: 13px;
      color: #606266;
    }
  }

  .category-list {
    max-height: 500px;
    overflow-y: auto;
    padding-right: 8px;
  }

  .category-section {
    margin-bottom: 20px;

    .category-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #ebeef5;

      .category-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }
    }
  }

  .checklist-item {
    display: flex;
    align-items: flex-start;
    padding: 12px 16px;
    background: #fafafa;
    border-radius: 8px;
    margin-bottom: 8px;
    border: 1px solid #ebeef5;
    transition: all 0.2s;

    &:hover {
      background: #f5f7fa;
      border-color: #dcdfe6;
    }

    &.is-completed {
      background: #f0f9eb;
      border-color: #e1f3d8;

      .item-name {
        text-decoration: line-through;
        color: #909399;
      }
    }

    &.is-required {
      border-left: 3px solid #f56c6c;
    }

    .item-checkbox {
      margin-right: 12px;
      padding-top: 2px;
    }

    .item-content {
      flex: 1;

      .item-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 4px;
      }

      .item-desc {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }

      .item-meta {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #67c23a;
      }
    }
  }
}

@media (max-width: 768px) {
  .checklist-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
