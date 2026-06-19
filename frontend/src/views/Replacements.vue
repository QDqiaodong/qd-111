<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">更换周期登记</h2>
      <div class="table-toolbar">
        <el-radio-group v-model="viewMode" size="default" @change="handleViewModeChange">
          <el-radio-button value="list">
            <el-icon><List /></el-icon>
            <span>列表视图</span>
          </el-radio-button>
          <el-radio-button value="timeline">
            <el-icon><Clock /></el-icon>
            <span>时间线视图</span>
          </el-radio-button>
        </el-radio-group>
        <el-dropdown trigger="click" @command="handleRecalcCommand">
          <el-button>
            <el-icon><Refresh /></el-icon>
            <span>重算使用天数</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="all">
                <el-icon><Refresh /></el-icon>全部重算（含标准周期）
              </el-dropdown-item>
              <el-dropdown-item command="usage">
                <el-icon><Timer /></el-icon>仅重算使用天数
              </el-dropdown-item>
              <el-dropdown-item command="currentFilter" v-if="filters.accessoryId">
                <el-icon><Goods /></el-icon>当前配件重算
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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
      v-if="viewMode === 'list'"
      :selected="selectedRows"
      @batch-delete="handleBatchDelete"
      @clear="clearSelection"
    />

    <el-card v-if="viewMode === 'list'" class="card-shadow" shadow="never" body-style="padding: 0">
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

    <div v-else class="timeline-container" v-loading="loading">
      <div v-if="timelineData.length === 0" class="empty-state">
        <el-empty description="暂无更换记录" />
      </div>
      <div v-for="group in timelineData" :key="group.accessoryId" class="timeline-group">
        <div class="timeline-group-header">
          <div class="accessory-info">
            <el-image
              v-if="group.imageUrl"
              :src="group.imageUrl"
              fit="cover"
              class="accessory-avatar"
            />
            <el-icon v-else :size="32" color="#c0c4cc" class="accessory-avatar-icon"><Goods /></el-icon>
            <div class="accessory-detail">
              <div class="accessory-title">{{ group.accessoryName }}</div>
              <div class="accessory-meta">
                <span class="meta-item">{{ group.specification }}</span>
                <span class="meta-divider">·</span>
                <span class="meta-item">{{ group.instrumentName }}</span>
              </div>
            </div>
          </div>
          <div class="group-stats">
            <div class="stat-item">
              <span class="stat-value">{{ group.recordCount }}</span>
              <span class="stat-label">更换次数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ group.avgUsageDays || 0 }}天</span>
              <span class="stat-label">平均使用</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ group.standardCycle }}天</span>
              <span class="stat-label">标准周期</span>
            </div>
          </div>
        </div>

        <div class="timeline-list">
          <div
            v-for="(item, index) in group.items"
            :key="item.id"
            class="timeline-item"
            :class="{ 'is-first': item.isFirst }"
          >
            <div class="timeline-dot">
              <div class="dot-inner"></div>
            </div>
            <div class="timeline-line" v-if="index < group.items.length - 1"></div>

            <div class="timeline-content">
              <div class="timeline-date-row">
                <span class="timeline-date">{{ item.replaceDate }}</span>
                <el-tag
                  v-if="!item.isFirst"
                  size="small"
                  :type="getIntervalTagType(item.intervalDays, group.standardCycle)"
                  class="interval-tag"
                >
                  {{ item.intervalLabel }}
                </el-tag>
                <el-tag v-else size="small" type="info" class="interval-tag">
                  {{ item.intervalLabel }}
                </el-tag>
              </div>

              <div class="timeline-detail-row">
                <div class="detail-item">
                  <span class="detail-label">使用天数</span>
                  <el-tag size="small" :type="getUsageDaysTagType(item.usageDays, group.standardCycle)">
                    {{ item.usageDays }}天
                  </el-tag>
                </div>
                <div class="detail-item" v-if="item.operator">
                  <span class="detail-label">操作人</span>
                  <span class="detail-value">{{ item.operator }}</span>
                </div>
              </div>

              <div v-if="item.remark" class="timeline-remark">
                <el-icon class="remark-icon"><ChatDotRound /></el-icon>
                <span class="remark-text">{{ item.remark }}</span>
              </div>

              <div class="timeline-actions">
                <el-button type="primary" link size="small" @click="handleEditTimelineItem(item, group)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteTimelineItem(item)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '登记更换' : '编辑更换记录'"
      width="680px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="更换信息" name="basic">
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
        </el-tab-pane>

        <el-tab-pane label="准备清单" name="checklist">
          <div class="checklist-tab-content">
            <div v-if="!form.accessoryId" class="empty-state">
              <el-empty description="请先选择配件以生成准备清单" />
            </div>
            <div v-else-if="!linkedChecklist && !generatingChecklist" class="checklist-actions-bar">
              <el-alert
                title="该配件更换前需要完成准备清单"
                :type="templateAvailable ? 'info' : 'warning'"
                :closable="false"
                show-icon
                style="margin-bottom: 12px"
              >
                <template #default>
                  <span v-if="templateAvailable">
                    检测到该配件类型「{{ currentAccessory?.typeName }}」有对应的准备清单模板，建议先生成并完成清单后再提交更换记录。
                  </span>
                  <span v-else>
                    该配件类型「{{ currentAccessory?.typeName }}」暂无准备清单模板，可跳过此步骤。
                  </span>
                </template>
                <template #operation>
                  <el-button
                    size="small"
                    type="primary"
                    v-if="templateAvailable"
                    :loading="generatingChecklist"
                    @click="generateChecklistForReplacement"
                  >
                    生成准备清单
                  </el-button>
                </template>
              </el-alert>
            </div>
            <div v-else-if="linkedChecklist" class="checklist-preview">
              <div class="checklist-preview-header">
                <div>
                  <div class="checklist-name">{{ linkedChecklist.templateName }}</div>
                  <div class="checklist-meta">
                    <el-tag size="small" :type="getStatusTagType(linkedChecklist.status)">
                      {{ linkedChecklist.statusName }}
                    </el-tag>
                    <span class="progress-info">
                      进度：{{ linkedChecklist.completedCount }}/{{ linkedChecklist.totalCount }}
                      （必做 {{ linkedChecklist.requiredCompletedCount }}/{{ linkedChecklist.requiredTotalCount }}）
                    </span>
                  </div>
                </div>
                <div class="checklist-preview-actions">
                  <el-button
                    size="small"
                    type="success"
                    v-if="linkedChecklist.status === 'pending'"
                    @click="startLinkedChecklist"
                  >
                    开始清单
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    @click="openChecklistDetail"
                  >
                    打开清单
                  </el-button>
                </div>
              </div>
              <el-progress
                :percentage="getChecklistProgressPercent(linkedChecklist)"
                :color="getChecklistProgressColor(linkedChecklist)"
                :stroke-width="10"
                style="margin-top: 12px"
              />
              <div
                v-if="linkedChecklist.requiredCompletedCount < linkedChecklist.requiredTotalCount && dialogMode === 'add'"
                class="checklist-warning"
              >
                <el-icon color="#e6a23c"><Warning /></el-icon>
                <span>还有必做项未完成，建议完成后再提交更换记录</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="checklistDetailDialogVisible"
      :title="`准备清单 - ${linkedChecklist?.templateName || ''}`"
      width="700px"
      destroy-on-close
      @close="handleChecklistDetailClose"
    >
      <div v-if="linkedChecklist" class="checklist-detail">
        <div class="checklist-header">
          <div class="checklist-info">
            <div class="info-row">
              <span class="info-label">配件：</span>
              <span class="info-value">{{ linkedChecklist.accessoryName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">类型：</span>
              <span class="info-value">{{ linkedChecklist.typeName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">总进度：</span>
              <span class="info-value">
                <el-tag size="small" :type="getStatusTagType(linkedChecklist.status)">
                  {{ linkedChecklist.statusName }}
                </el-tag>
                <span style="margin-left: 8px">{{ linkedChecklist.completedCount }}/{{ linkedChecklist.totalCount }} 项</span>
              </span>
            </div>
          </div>
          <div class="checklist-actions" v-if="linkedChecklist.status !== 'completed'">
            <el-button
              type="primary"
              size="small"
              v-if="linkedChecklist.status === 'pending'"
              @click="startLinkedChecklist"
            >
              <el-icon><VideoPlay /></el-icon>开始清单
            </el-button>
            <el-button
              type="success"
              size="small"
              v-if="linkedChecklist.status === 'in_progress'"
              :disabled="linkedChecklist.requiredCompletedCount < linkedChecklist.requiredTotalCount"
              @click="completeLinkedChecklist"
            >
              <el-icon><CircleCheck /></el-icon>完成清单
            </el-button>
          </div>
        </div>

        <el-progress
          :percentage="getChecklistProgressPercent(linkedChecklist)"
          :color="getChecklistProgressColor(linkedChecklist)"
          :stroke-width="12"
        />
        <div class="progress-stats">
          <span>必做项：<b>{{ linkedChecklist.requiredCompletedCount }}/{{ linkedChecklist.requiredTotalCount }}</b></span>
          <span>选做项：<b>{{ linkedChecklist.completedCount - linkedChecklist.requiredCompletedCount }}/{{ linkedChecklist.totalCount - linkedChecklist.requiredTotalCount }}</b></span>
        </div>

        <div class="category-list" v-loading="checklistDetailLoading">
          <div v-for="category in checklistCategoryData" :key="category.category" class="category-section">
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
                <div class="item-checkbox" @click="toggleChecklistItemComplete(item)">
                  <el-checkbox v-model="item.completed" :label="1" @change="() => handleChecklistItemComplete(item)">
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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  List, Clock, Plus, Goods, ChatDotRound, Refresh, ArrowDown, Timer,
  Warning, VideoPlay, CircleCheck, User, Tools, Brush, Setting, View, Box
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { replacementApi, accessoryApi, preparationChecklistApi, preparationTemplateApi } from '@/api'
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
const viewMode = ref('list')
const timelineData = ref([])

const activeTab = ref('basic')
const generatingChecklist = ref(false)
const templateAvailable = ref(false)
const linkedChecklist = ref(null)
const checklistDetailDialogVisible = ref(false)
const checklistDetailLoading = ref(false)
const checklistCategoryData = ref([])

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

const currentAccessory = computed(() => {
  if (!form.accessoryId) return null
  return accessoryList.value.find(a => a.id === form.accessoryId)
})

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
    accessoryList.value = []
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

const loadTimeline = async () => {
  loading.value = true
  try {
    const params = {
      keyword: filters.keyword,
      accessoryId: filters.accessoryId
    }
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }
    const res = await replacementApi.timeline(params)
    if (res && res.data) {
      timelineData.value = res.data
    } else {
      timelineData.value = []
    }
  } catch {
    timelineData.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  if (viewMode.value === 'list') {
    loadList()
  } else {
    loadTimeline()
  }
}

const handleViewModeChange = () => {
  if (viewMode.value === 'list') {
    loadList()
  } else {
    loadTimeline()
  }
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
  activeTab.value = 'basic'
  linkedChecklist.value = null
  templateAvailable.value = false
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

const handleEditTimelineItem = (item, group) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: item.id,
    accessoryId: group.accessoryId,
    replaceDate: item.replaceDate,
    operator: item.operator,
    remark: item.remark
  })
  dialogVisible.value = true
  if (form.accessoryId) {
    replacementApi.history(form.accessoryId).then(res => {
      historyList.value = res.data || res || []
    }).catch(() => {})
  }
}

const handleDeleteTimelineItem = (item) => {
  handleDelete(item)
}

const handleAccessoryChange = async (id) => {
  const acc = accessoryList.value.find(a => a.id === id)
  if (acc) {
    try {
      const res = await replacementApi.history(id)
      historyList.value = res.data || res || []
    } catch {}

    try {
      const templateRes = await preparationTemplateApi.getByTypeCode(acc.typeCode)
      templateAvailable.value = !!(templateRes && templateRes.data)
    } catch {
      templateAvailable.value = false
    }

    linkedChecklist.value = null
  } else {
    templateAvailable.value = false
    linkedChecklist.value = null
  }
}

const generateChecklistForReplacement = async () => {
  if (!form.accessoryId) {
    ElMessage.warning('请先选择配件')
    return
  }
  generatingChecklist.value = true
  try {
    const res = await preparationChecklistApi.generate({
      accessoryId: form.accessoryId,
      operator: form.operator,
      remark: '更换登记时生成'
    })
    if (res && res.data) {
      linkedChecklist.value = res.data
      ElMessage.success('准备清单生成成功')
    }
  } catch (e) {
    ElMessage.error(e?.message || '生成失败')
  } finally {
    generatingChecklist.value = false
  }
}

const startLinkedChecklist = async () => {
  if (!linkedChecklist.value) return
  try {
    await ElMessageBox.confirm(`确定开始「${linkedChecklist.value.templateName}」吗？`, '确认开始', {
      type: 'primary',
      confirmButtonText: '开始',
      cancelButtonText: '取消'
    })
    const res = await preparationChecklistApi.startChecklist(linkedChecklist.value.id, form.operator)
    if (res && res.data) {
      linkedChecklist.value = res.data
      ElMessage.success('清单已开始')
      if (checklistDetailDialogVisible.value) {
        loadChecklistDetailData(linkedChecklist.value.id)
      }
    }
  } catch (e) {
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

const completeLinkedChecklist = async () => {
  if (!linkedChecklist.value) return
  try {
    await ElMessageBox.confirm('确定完成此清单吗？完成后将无法修改。', '确认完成', {
      type: 'success',
      confirmButtonText: '完成',
      cancelButtonText: '取消'
    })
    const res = await preparationChecklistApi.completeChecklist(linkedChecklist.value.id, form.operator)
    if (res && res.data) {
      linkedChecklist.value = res.data
      ElMessage.success('清单已完成')
      if (checklistDetailDialogVisible.value) {
        loadChecklistDetailData(linkedChecklist.value.id)
      }
    }
  } catch (e) {
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

const openChecklistDetail = () => {
  if (!linkedChecklist.value) return
  checklistDetailDialogVisible.value = true
  loadChecklistDetailData(linkedChecklist.value.id)
}

const loadChecklistDetailData = async (id) => {
  checklistDetailLoading.value = true
  try {
    const res = await preparationChecklistApi.getChecklistWithCategories(id)
    if (res && res.data) {
      checklistCategoryData.value = res.data
    }
    const res2 = await preparationChecklistApi.getById(id)
    if (res2 && res2.data) {
      linkedChecklist.value = res2.data
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载详情失败')
  } finally {
    checklistDetailLoading.value = false
  }
}

const toggleChecklistItemComplete = (item) => {
  if (!linkedChecklist.value || linkedChecklist.value.status === 'completed') {
    return
  }
  if (linkedChecklist.value.status === 'pending') {
    ElMessage.warning('请先开始清单')
    return
  }
  item.completed = item.completed === 1 ? 0 : 1
}

const handleChecklistItemComplete = async (item) => {
  if (!linkedChecklist.value || linkedChecklist.value.status !== 'in_progress') {
    return
  }
  try {
    const res = await preparationChecklistApi.completeItem({
      itemId: item.id,
      completed: item.completed,
      completedBy: form.operator || '本人'
    })
    if (res && res.data) {
      linkedChecklist.value = res.data
      loadChecklistDetailData(linkedChecklist.value.id)
    }
  } catch (e) {
    item.completed = item.completed === 1 ? 0 : 1
    ElMessage.error(e?.message || '更新失败')
  }
}

const handleChecklistDetailClose = () => {
  checklistCategoryData.value = []
}

const getChecklistProgressPercent = (row) => {
  if (!row || !row.totalCount) return 0
  return Math.round((row.completedCount / row.totalCount) * 100)
}

const getChecklistProgressColor = (row) => {
  const pct = getChecklistProgressPercent(row)
  if (pct >= 100) return '#67c23a'
  if (pct >= 80) return '#e6a23c'
  return '#409eff'
}

const getCategoryIcon = (category) => {
  return categoryIcons[category] || Goods
}

const getCategoryIconColor = (category) => {
  return categoryColors[category] || '#909399'
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
      refreshCurrentView()
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

const refreshCurrentView = () => {
  if (viewMode.value === 'list') {
    loadList()
  } else {
    loadTimeline()
  }
  loadHistory()
}

const handleSubmit = async () => {
  await formRef.value.validate()

  if (dialogMode.value === 'add' && linkedChecklist.value && linkedChecklist.value.status !== 'completed') {
    if (linkedChecklist.value.requiredCompletedCount < linkedChecklist.value.requiredTotalCount) {
      try {
        await ElMessageBox.confirm(
          '还有必做项未完成，确定要提交更换记录吗？建议完成所有必做项后再提交。',
          '清单未完成',
          { type: 'warning', confirmButtonText: '继续提交', cancelButtonText: '取消' }
        )
      } catch (e) {
        if (e?.message === 'cancel') {
          activeTab.value = 'checklist'
          return
        }
      }
    }
  }

  submitting.value = true
  const originalForm = { ...form }
  try {
    let res
    if (dialogMode.value === 'add') {
      res = await replacementApi.add(form)
    } else {
      res = await replacementApi.update(form)
    }
    const result = res?.data

    if (linkedChecklist.value && result?.recordId) {
      try {
        await preparationChecklistApi.linkReplacementRecord(linkedChecklist.value.id, result.recordId)
      } catch (e) {
        console.warn('关联准备清单失败', e)
      }
    }

    showSuccessNotification(result)
    dialogVisible.value = false
    refreshCurrentView()
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

const showSuccessNotification = (result) => {
  if (!result) {
    ElMessage.success('保存成功')
    return
  }

  const lines = []

  if (result.deviationLabel) {
    lines.push(`<div style="margin-bottom: 8px;"><b>周期偏差：</b>${result.deviationLabel}</div>`)
  } else if (result.usageDays != null) {
    lines.push(`<div style="margin-bottom: 8px;"><b>使用天数：</b>${result.usageDays}天</div>`)
  }

  if (result.statusMessage) {
    const statusIcon = result.statusUpdated ? '✅' : 'ℹ️'
    lines.push(`<div><b>状态联动：</b>${statusIcon} ${result.statusMessage}</div>`)
  }

  const html = lines.join('')

  ElNotification({
    title: '更换记录保存成功',
    dangerouslyUseHTMLString: true,
    message: html || '保存成功',
    type: 'success',
    duration: 5000,
    showClose: true
  })
}

const handleDialogClose = () => {
  resetForm()
}

const handleRecalcCommand = async (command) => {
  try {
    if (command === 'all') {
      await ElMessageBox.confirm(
        '确定要重算全部配件的使用天数和标准周期吗？\n这将基于最新的采购日期、标准周期规则重新计算所有历史记录。',
        '全量重算确认',
        { type: 'warning', confirmButtonText: '确认重算', cancelButtonText: '取消' }
      )
      await replacementApi.recalculateAll()
      ElMessage.success('全量重算完成，已更新所有更换记录的使用天数和标准周期')
    } else if (command === 'usage') {
      await ElMessageBox.confirm(
        '确定要重算全部配件的使用天数吗？\n将基于最新采购日期和更换日期重新计算（不更新标准周期）。',
        '重算使用天数确认',
        { type: 'warning', confirmButtonText: '确认重算', cancelButtonText: '取消' }
      )
      await replacementApi.recalculateByCondition()
      ElMessage.success('使用天数重算完成')
    } else if (command === 'currentFilter') {
      if (!filters.accessoryId) {
        ElMessage.warning('请先选择配件')
        return
      }
      const acc = accessoryList.value.find(a => a.id === filters.accessoryId)
      await ElMessageBox.confirm(
        `确定要重算配件「${acc?.name || filters.accessoryId}」的所有更换记录吗？`,
        '单个配件重算确认',
        { type: 'warning', confirmButtonText: '确认重算', cancelButtonText: '取消' }
      )
      await replacementApi.recalculateByAccessory(filters.accessoryId, true)
      ElMessage.success('该配件更换记录重算完成')
    }
    refreshCurrentView()
    loadHistory()
  } catch (e) {
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '重算失败，请稍后重试')
    }
  }
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

const getIntervalTagType = (intervalDays, standardCycle) => {
  if (!standardCycle || !intervalDays) return 'info'
  const ratio = intervalDays / standardCycle
  if (ratio >= 1.2) return 'success'
  if (ratio >= 0.8) return 'warning'
  return 'danger'
}

const getUsageDaysTagType = (usageDays, standardCycle) => {
  if (!standardCycle || !usageDays) return 'info'
  const ratio = usageDays / standardCycle
  if (ratio >= 1) return 'danger'
  if (ratio >= 0.8) return 'warning'
  return 'success'
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

.timeline-container {
  .empty-state {
    padding: 60px 0;
  }

  .timeline-group {
    background: #fff;
    border-radius: 12px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    overflow: hidden;
  }

  .timeline-group-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
    border-bottom: 1px solid #ebeef5;

    .accessory-info {
      display: flex;
      align-items: center;
      gap: 14px;

      .accessory-avatar {
        width: 48px;
        height: 48px;
        border-radius: 10px;
        flex-shrink: 0;
      }

      .accessory-avatar-icon {
        width: 48px;
        height: 48px;
        padding: 8px;
        background: #fff;
        border-radius: 10px;
        flex-shrink: 0;
      }

      .accessory-detail {
        .accessory-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 4px;
        }
        .accessory-meta {
          font-size: 13px;
          color: #909399;
          display: flex;
          align-items: center;
          gap: 6px;

          .meta-divider {
            color: #dcdfe6;
          }
        }
      }
    }

    .group-stats {
      display: flex;
      gap: 32px;

      .stat-item {
        text-align: center;

        .stat-value {
          display: block;
          font-size: 20px;
          font-weight: 600;
          color: #409eff;
          margin-bottom: 2px;
        }
        .stat-label {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  .timeline-list {
    padding: 20px 24px 10px;
    position: relative;
  }

  .timeline-item {
    position: relative;
    padding-left: 32px;
    padding-bottom: 24px;

    &:last-child {
      padding-bottom: 10px;
    }

    .timeline-dot {
      position: absolute;
      left: 0;
      top: 4px;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: #ecf5ff;
      border: 2px solid #409eff;
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: center;

      .dot-inner {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #409eff;
      }
    }

    &.is-first .timeline-dot {
      background: #f0f9eb;
      border-color: #67c23a;

      .dot-inner {
        background: #67c23a;
      }
    }

    .timeline-line {
      position: absolute;
      left: 7px;
      top: 22px;
      bottom: 0;
      width: 2px;
      background: #ebeef5;
    }

    .timeline-content {
      background: #fafafa;
      border-radius: 8px;
      padding: 16px 18px;
      border: 1px solid #ebeef5;
      transition: all 0.2s;

      &:hover {
        background: #f5f7fa;
        border-color: #dcdfe6;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      }

      .timeline-date-row {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 10px;

        .timeline-date {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
        }

        .interval-tag {
          flex-shrink: 0;
        }
      }

      .timeline-detail-row {
        display: flex;
        gap: 24px;
        margin-bottom: 10px;

        .detail-item {
          display: flex;
          align-items: center;
          gap: 8px;

          .detail-label {
            font-size: 13px;
            color: #909399;
          }

          .detail-value {
            font-size: 13px;
            color: #606266;
            font-weight: 500;
          }
        }
      }

      .timeline-remark {
        display: flex;
        align-items: flex-start;
        gap: 6px;
        padding: 8px 12px;
        background: #fff;
        border-radius: 6px;
        border-left: 3px solid #409eff;
        margin-bottom: 10px;

        .remark-icon {
          color: #409eff;
          margin-top: 2px;
          flex-shrink: 0;
          font-size: 14px;
        }

        .remark-text {
          font-size: 13px;
          color: #606266;
          line-height: 1.5;
        }
      }

      .timeline-actions {
        display: flex;
        gap: 8px;
        justify-content: flex-end;
        padding-top: 8px;
        border-top: 1px dashed #ebeef5;
      }
    }
  }
}

@media (max-width: 768px) {
  .timeline-group-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 16px;

    .group-stats {
      width: 100%;
      justify-content: space-around;
    }
  }
}

.checklist-tab-content {
  min-height: 400px;

  .empty-state {
    padding: 60px 0;
  }
}

.checklist-preview {
  .checklist-preview-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 16px 20px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 16px;

    .checklist-name {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 8px;
    }

    .checklist-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 13px;

      .progress-info {
        color: #606266;
      }
    }

    .checklist-preview-actions {
      display: flex;
      gap: 8px;
    }
  }

  .checklist-warning {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 14px;
    background: #fdf6ec;
    border: 1px solid #faecd8;
    border-radius: 6px;
    margin-top: 12px;
    font-size: 13px;
    color: #e6a23c;
  }
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

  .progress-stats {
    display: flex;
    justify-content: space-around;
    margin-top: 8px;
    margin-bottom: 20px;
    font-size: 13px;
    color: #606266;
  }

  .category-list {
    max-height: 450px;
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
  .checklist-preview-header {
    flex-direction: column;
    gap: 12px;
  }

  .checklist-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
