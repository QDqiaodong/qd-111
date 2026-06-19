<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">损耗状态字典</h2>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增状态
        </el-button>
      </div>
    </div>

    <el-card class="card-shadow" shadow="never" body-style="padding: 0">
      <el-table
        ref="tableRef"
        :data="tableData"
        stripe
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="状态标识" width="140">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'primary' : 'info'" effect="plain">{{ row.statusCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态名称" min-width="140">
          <template #default="{ row }">
            <div class="status-name-cell">
              <span class="status-color-dot" :style="{ background: row.color }"></span>
              <span>{{ row.statusLabel }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="颜色" width="120" align="center">
          <template #default="{ row }">
            <div class="color-preview">
              <span class="color-block" :style="{ background: row.color }"></span>
              <span class="color-text">{{ row.color }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="80" align="center" prop="sortOrder" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              size="small"
              @update:model-value="(val) => handleToggleStatus(row, val)"
              :loading="row._toggleLoading"
            />
          </template>
        </el-table-column>
        <el-table-column label="使用中的配件" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row._usageCount != null" size="small" :type="row._usageCount > 0 ? 'warning' : 'success'" effect="plain">
              {{ row._usageCount }} 件
            </el-tag>
            <el-tag v-else size="small" type="info" effect="plain">
              <el-icon><Loading /></el-icon>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="row._usageCount != null && row._usageCount > 0"
            >
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑状态' : '新增状态'"
      width="520px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="状态编码" prop="statusCode">
          <el-input v-model="form.statusCode" placeholder="请输入状态编码，如 good" :disabled="isEdit" maxlength="20" />
        </el-form-item>
        <el-form-item label="状态名称" prop="statusLabel">
          <el-input v-model="form.statusLabel" placeholder="请输入状态名称，如 完好" maxlength="50" />
        </el-form-item>
        <el-form-item label="状态颜色">
          <div class="color-picker-row">
            <el-color-picker v-model="form.color" show-alpha />
            <span class="color-value">{{ form.color || '#909399' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="form-tip">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注说明" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="deleteDialogVisible"
      title="删除确认"
      width="480px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div v-if="currentDeleteRow" class="delete-confirm-content">
        <div class="confirm-icon-wrap">
          <el-icon :size="40" color="#e6a23c"><WarningFilled /></el-icon>
        </div>
        <div class="confirm-text">
          <div class="confirm-title">确定要删除状态「{{ currentDeleteRow.statusLabel }}」吗？</div>
          <div class="confirm-desc">删除后该状态将无法在配件管理中使用</div>
        </div>

        <div v-if="deleteUsageInfo && deleteUsageInfo.usedCount > 0" class="usage-warning-block">
          <div class="warning-header">
            <el-icon color="#f56c6c"><InfoFilled /></el-icon>
            <span class="warning-title">使用中状态，不可删除</span>
          </div>
          <div class="warning-body">
            <div class="usage-count">
              <span class="count-num">{{ deleteUsageInfo.usedCount }}</span>
              <span class="count-label">个配件正在使用此状态</span>
            </div>
            <div class="typical-accessories">
              <div class="typical-label">典型配件：</div>
              <div class="typical-list">
                <el-tag
                  v-for="(name, idx) in deleteUsageInfo.typicalAccessoryNames"
                  :key="idx"
                  size="small"
                  type="info"
                  effect="plain"
                  class="typical-tag"
                >
                  {{ name }}
                </el-tag>
                <span v-if="deleteUsageInfo.usedCount > 5" class="more-hint">
                  等 {{ deleteUsageInfo.usedCount }} 件
                </span>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="deleteUsageInfo && deleteUsageInfo.usedCount === 0" class="usage-safe-block">
          <el-icon color="#67c23a"><CircleCheckFilled /></el-icon>
          <span>当前状态未被任何配件使用，可以安全删除</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button
          type="danger"
          @click="executeDelete"
          :loading="deleteSubmitting"
          :disabled="deleteUsageInfo && deleteUsageInfo.usedCount > 0"
        >
          确认删除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, WarningFilled, InfoFilled, CircleCheckFilled, Loading
} from '@element-plus/icons-vue'
import { wornStatusDictApi } from '@/api'

const loading = ref(false)
const tableData = ref([])
const tableRef = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  statusCode: '',
  statusLabel: '',
  color: '#67c23a',
  sortOrder: 0,
  enabled: 1,
  remark: ''
})

const rules = {
  statusCode: [
    { required: true, message: '请输入状态编码', trigger: 'blur' },
    { max: 20, message: '状态编码不能超过20个字符', trigger: 'blur' }
  ],
  statusLabel: [
    { required: true, message: '请输入状态名称', trigger: 'blur' },
    { max: 50, message: '状态名称不能超过50个字符', trigger: 'blur' }
  ]
}

const deleteDialogVisible = ref(false)
const currentDeleteRow = ref(null)
const deleteUsageInfo = ref(null)
const deleteSubmitting = ref(false)

const loadList = async () => {
  loading.value = true
  try {
    const res = await wornStatusDictApi.list()
    tableData.value = res.data || res || []
    loadAllUsageInfo()
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadAllUsageInfo = async () => {
  for (const row of tableData.value) {
    try {
      const res = await wornStatusDictApi.getUsage(row.id)
      row._usageCount = res.data?.usedCount ?? 0
    } catch {
      row._usageCount = 0
    }
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    statusCode: '',
    statusLabel: '',
    color: '#67c23a',
    sortOrder: 0,
    enabled: 1,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    statusCode: row.statusCode,
    statusLabel: row.statusLabel,
    color: row.color || '#909399',
    sortOrder: row.sortOrder || 0,
    enabled: row.enabled,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await wornStatusDictApi.update(form)
        ElMessage.success('更新成功')
      } else {
        await wornStatusDictApi.add(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadList()
    } catch (e) {
      ElMessage.error(e?.message || (isEdit.value ? '更新失败' : '新增失败'))
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row) => {
  currentDeleteRow.value = row
  deleteUsageInfo.value = null
  deleteDialogVisible.value = true
  try {
    const res = await wornStatusDictApi.getUsage(row.id)
    deleteUsageInfo.value = res.data
  } catch (e) {
    deleteUsageInfo.value = { usedCount: 0, typicalAccessoryNames: [] }
  }
}

const executeDelete = async () => {
  if (!currentDeleteRow.value) return
  deleteSubmitting.value = true
  try {
    await wornStatusDictApi.remove([currentDeleteRow.value.id])
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  } finally {
    deleteSubmitting.value = false
  }
}

const handleToggleStatus = async (row, val) => {
  row._toggleLoading = true
  const originalEnabled = row.enabled
  try {
    await wornStatusDictApi.toggleStatus(row.id, val ? 1 : 0)
    row.enabled = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (e) {
    row.enabled = originalEnabled
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '状态切换失败')
    }
  } finally {
    row._toggleLoading = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.status-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .status-color-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    display: inline-block;
    flex-shrink: 0;
  }
}

.color-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .color-block {
    width: 24px;
    height: 24px;
    border-radius: 4px;
    border: 1px solid #ebeef5;
  }

  .color-text {
    font-size: 12px;
    color: #909399;
    font-family: monospace;
  }
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .color-value {
    font-size: 13px;
    color: #606266;
    font-family: monospace;
  }
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.delete-confirm-content {
  .confirm-icon-wrap {
    text-align: center;
    margin-bottom: 16px;
  }

  .confirm-text {
    text-align: center;
    margin-bottom: 20px;

    .confirm-title {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 6px;
    }

    .confirm-desc {
      font-size: 13px;
      color: #909399;
    }
  }
}

.usage-warning-block {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  padding: 16px;

  .warning-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 12px;

    .warning-title {
      font-size: 14px;
      font-weight: 600;
      color: #f56c6c;
    }
  }

  .warning-body {
    .usage-count {
      display: flex;
      align-items: baseline;
      gap: 6px;
      margin-bottom: 12px;

      .count-num {
        font-size: 28px;
        font-weight: 700;
        color: #f56c6c;
      }

      .count-label {
        font-size: 13px;
        color: #606266;
      }
    }

    .typical-accessories {
      .typical-label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 6px;
      }

      .typical-list {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 6px;

        .typical-tag {
          margin: 0;
        }

        .more-hint {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

.usage-safe-block {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #67c23a;
  font-size: 14px;
  justify-content: center;
}
</style>
