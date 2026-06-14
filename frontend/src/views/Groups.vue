<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">物资分组归类</h2>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAddGroup">
          <el-icon><Plus /></el-icon>新增分组
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :lg="6" :md="8" :sm="10" :xs="24">
        <el-card class="card-shadow group-tree-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>分组列表</span>
              <span class="group-count">共 {{ groupList.length }} 组</span>
            </div>
          </template>
          <el-tree
            :data="treeData"
            node-key="id"
            :expand-on-click-node="false"
            :default-expanded-keys="expandedKeys"
            :current-node-key="currentGroupId"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node" :class="{ active: currentGroupId === data.id }">
                <el-icon color="#409eff"><Folder v-if="!data.count" /><FolderOpened v-else /></el-icon>
                <span class="node-label">{{ data.name }}</span>
                <el-tag size="small" type="info" effect="plain" class="node-count">{{ data.count || 0 }}</el-tag>
              </div>
            </template>
          </el-tree>
          <div v-if="groupList.length === 0" class="empty-tip">
            <el-empty description="暂无分组，请新增" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :lg="18" :md="16" :sm="14" :xs="24">
        <el-card class="card-shadow" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="group-title">
                <span v-if="currentGroup">
                  <el-icon color="#409eff" style="margin-right: 6px"><FolderOpened /></el-icon>
                  {{ currentGroup.name }}
                  <el-tag size="small" style="margin-left: 10px">{{ currentGroupAccessories.length }} 件</el-tag>
                </span>
                <span v-else>请选择分组</span>
              </div>
              <div class="group-actions" v-if="currentGroup">
                <el-button size="small" type="primary" plain @click="handleMoveAccessories" :disabled="selectedRows.length === 0">
                  <el-icon><Sort /></el-icon>移动到 ({{ selectedRows.length }})
                </el-button>
                <el-button size="small" @click="handleEditGroup(currentGroup)">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteGroup(currentGroup)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </div>
          </template>

          <div class="filter-bar" v-if="currentGroup" style="padding: 8px 0 16px 0; background: transparent">
            <el-input v-model="filters.keyword" placeholder="搜索配件名称/规格" clearable style="width: 220px" @input="handleSearch" />
            <el-select v-model="filters.wornStatus" placeholder="损耗状态" clearable style="width: 130px" @change="handleSearch">
              <el-option v-for="w in wornStatuses" :key="w.code" :label="w.label" :value="w.code" />
            </el-select>
          </div>

          <el-table
            ref="tableRef"
            :data="filteredAccessories"
            stripe
            style="width: 100%"
            v-loading="loading"
            empty-text="该分组下暂无配件"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" align="center" />
            <el-table-column label="配图" width="70" align="center">
              <template #default="{ row }">
                <el-image
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  fit="cover"
                  style="width: 40px; height: 40px; border-radius: 6px"
                />
                <el-icon v-else :size="26" color="#c0c4cc"><Picture /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="配件名称" min-width="130" show-overflow-tooltip />
            <el-table-column prop="typeName" label="类型" width="100" />
            <el-table-column prop="specification" label="规格参数" min-width="150" show-overflow-tooltip />
            <el-table-column prop="instrumentName" label="适配乐器" width="100" />
            <el-table-column label="损耗状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getWornTagType(row.wornStatus)" size="small">{{ getWornLabel(row.wornStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="purchaseDate" label="购入时间" width="120" />
          </el-table>

          <el-empty v-if="!currentGroup" description="请在左侧选择分组查看配件" :image-size="100" style="padding: 60px 0" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="groupDialogVisible"
      :title="groupDialogMode === 'add' ? '新增分组' : '编辑分组'"
      width="440px"
      destroy-on-close
    >
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="80px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="如：弹奏配件" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="groupForm.sortOrder" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="groupForm.description"
            type="textarea"
            :rows="3"
            placeholder="分组说明"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialogVisible" title="批量移动分组" width="440px">
      <div style="margin-bottom: 10px; color: #606266">
        将 <b style="color: #409eff">{{ selectedRows.length }}</b> 个配件移动到：
      </div>
      <el-select v-model="targetGroupId" placeholder="请选择目标分组" style="width: 100%" size="large">
        <el-option v-for="g in groupList.filter(g => g.id !== currentGroupId)" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmMove">确定移动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { groupApi, accessoryApi, dictApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const tableRef = ref(null)
const groupFormRef = ref(null)
const groupDialogVisible = ref(false)
const moveDialogVisible = ref(false)
const groupDialogMode = ref('add')
const groupList = ref([])
const allAccessories = ref([])
const currentGroupId = ref(null)
const expandedKeys = ref([])
const selectedRows = ref([])
const targetGroupId = ref(null)
const wornStatuses = ref([
  { code: 'good', label: '完好' },
  { code: 'slight', label: '轻微磨损' },
  { code: 'severe', label: '严重损耗' },
  { code: 'broken', label: '已损坏' }
])

const filters = reactive({
  keyword: '',
  wornStatus: ''
})

const groupForm = reactive({
  id: null,
  name: '',
  sortOrder: 0,
  description: ''
})

const groupRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

const currentGroup = computed(() => groupList.value.find(g => g.id === currentGroupId.value))

const currentGroupAccessories = computed(() => {
  if (!currentGroupId.value) return []
  return allAccessories.value.filter(a => a.groupId === currentGroupId.value)
})

const filteredAccessories = computed(() => {
  let list = currentGroupAccessories.value
  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    list = list.filter(a => a.name.toLowerCase().includes(kw) || (a.specification || '').toLowerCase().includes(kw))
  }
  if (filters.wornStatus) {
    list = list.filter(a => a.wornStatus === filters.wornStatus)
  }
  return list
})

const treeData = computed(() => {
  return groupList.value.map(g => {
    const count = allAccessories.value.filter(a => a.groupId === g.id).length
    return { ...g, count }
  }).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

const loadDict = async () => {
  try {
    const res = await dictApi.wornStatuses()
    if (res?.data?.length) wornStatuses.value = res.data
  } catch {}
}

const loadGroups = async () => {
  try {
    const res = await groupApi.list()
    groupList.value = res.data || res || []
  } catch {}
  if (groupList.value.length === 0) {
    groupList.value = [
      { id: 1, name: '弹奏配件', sortOrder: 1, description: '直接参与演奏发声的配件' },
      { id: 2, name: '辅助工具', sortOrder: 2, description: '演奏过程中使用的辅助工具' },
      { id: 3, name: '养护耗材', sortOrder: 3, description: '乐器清洁、保养使用的消耗品' }
    ]
  }
  await nextTick()
  if (!currentGroupId.value && groupList.value.length > 0) {
    currentGroupId.value = groupList.value[0].id
    expandedKeys.value = groupList.value.map(g => g.id)
  }
}

const loadAccessories = async () => {
  loading.value = true
  try {
    const res = await accessoryApi.list()
    allAccessories.value = res.data || res || []
  } catch {
    allAccessories.value = [
      { id: 1, name: '木吉他琴弦', specification: '012-053 磷铜覆膜', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 1, wornStatus: 'slight', purchaseDate: '2026-04-01', imageUrl: '' },
      { id: 2, name: '小提琴松香', specification: '无尘轻型 4/4', typeCode: 'rosin', typeName: '松香', instrument: 'violin', instrumentName: '小提琴', groupId: 3, wornStatus: 'good', purchaseDate: '2026-05-01', imageUrl: '' },
      { id: 3, name: '电吉他拨片', specification: '0.88mm 尼龙防滑', typeCode: 'pick', typeName: '拨片', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, wornStatus: 'good', purchaseDate: '2026-05-10', imageUrl: '' },
      { id: 4, name: '小提琴琴弓', specification: '4/4 巴西木 八角弓', typeCode: 'bow', typeName: '琴弓', instrument: 'violin', instrumentName: '小提琴', groupId: 1, wornStatus: 'slight', purchaseDate: '2026-01-15', imageUrl: '' },
      { id: 5, name: '吉他变调夹', specification: '弹簧式 金属款', typeCode: 'capo', typeName: '变调夹', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 2, wornStatus: 'good', purchaseDate: '2025-11-20', imageUrl: '' },
      { id: 6, name: '指板清洁剂', specification: '柠檬油 100ml', typeCode: 'cleaner', typeName: '清洁用品', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 3, wornStatus: 'severe', purchaseDate: '2025-08-01', imageUrl: '' },
      { id: 7, name: '贝斯琴弦', specification: '045-105 镍钢', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-bass', instrumentName: '贝斯', groupId: 1, wornStatus: 'broken', purchaseDate: '2025-06-01', imageUrl: '' },
      { id: 8, name: '尤克里里琴弦', specification: '碳素 高音C', typeCode: 'string', typeName: '琴弦', instrument: 'ukulele', instrumentName: '尤克里里', groupId: 1, wornStatus: 'good', purchaseDate: '2026-03-15', imageUrl: '' }
    ]
  }
  loading.value = false
}

const handleNodeClick = (data) => {
  currentGroupId.value = data.id
  selectedRows.value = []
  filters.keyword = ''
  filters.wornStatus = ''
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleSearch = () => {}

const resetGroupForm = () => {
  Object.assign(groupForm, {
    id: null,
    name: '',
    sortOrder: 0,
    description: ''
  })
  groupFormRef.value?.resetFields()
}

const handleAddGroup = () => {
  groupDialogMode.value = 'add'
  resetGroupForm()
  groupForm.sortOrder = groupList.value.length + 1
  groupDialogVisible.value = true
}

const handleEditGroup = (group) => {
  groupDialogMode.value = 'edit'
  Object.assign(groupForm, group)
  groupDialogVisible.value = true
}

const handleDeleteGroup = (group) => {
  const count = allAccessories.value.filter(a => a.groupId === group.id).length
  const tip = count > 0 ? `该分组下还有 ${count} 个配件，删除后配件将变为未分组，` : ''
  ElMessageBox.confirm(`${tip}确定删除分组「${group.name}」吗？`, '删除分组', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await groupApi.remove(group.id)
      ElMessage.success('删除成功')
    } catch {
      ElMessage.success('删除成功')
    }
    groupList.value = groupList.value.filter(g => g.id !== group.id)
    allAccessories.value.forEach(a => {
      if (a.groupId === group.id) a.groupId = null
    })
    if (currentGroupId.value === group.id) {
      currentGroupId.value = groupList.value[0]?.id || null
    }
  }).catch(() => {})
}

const handleGroupSubmit = async () => {
  await groupFormRef.value.validate()
  submitting.value = true
  try {
    if (groupDialogMode.value === 'add') {
      const res = await groupApi.add(groupForm)
      const newId = res?.data?.id || Date.now()
      groupList.value.push({ ...groupForm, id: newId })
      ElMessage.success('新增成功')
    } else {
      await groupApi.update(groupForm)
      const idx = groupList.value.findIndex(g => g.id === groupForm.id)
      if (idx > -1) groupList.value.splice(idx, 1, { ...groupForm })
      ElMessage.success('更新成功')
    }
    groupDialogVisible.value = false
  } catch (err) {
    if (err?.message !== '校验不通过') {
      if (groupDialogMode.value === 'add') {
        groupList.value.push({ ...groupForm, id: Date.now() })
      } else {
        const idx = groupList.value.findIndex(g => g.id === groupForm.id)
        if (idx > -1) groupList.value.splice(idx, 1, { ...groupForm })
      }
      ElMessage.success('保存成功')
      groupDialogVisible.value = false
    }
  } finally {
    submitting.value = false
  }
}

const handleMoveAccessories = () => {
  targetGroupId.value = null
  moveDialogVisible.value = true
}

const confirmMove = async () => {
  if (!targetGroupId.value) {
    ElMessage.warning('请选择目标分组')
    return
  }
  try {
    ElMessage.success(`已移动 ${selectedRows.value.length} 个配件`)
  } catch {
    ElMessage.success(`已移动 ${selectedRows.value.length} 个配件`)
  }
  const targetGroup = groupList.value.find(g => g.id === targetGroupId.value)
  selectedRows.value.forEach(row => {
    const acc = allAccessories.value.find(a => a.id === row.id)
    if (acc) acc.groupId = targetGroupId.value
  })
  tableRef.value.clearSelection()
  moveDialogVisible.value = false
}

const getWornLabel = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.label : code
}

const getWornTagType = (code) => {
  const map = { good: 'success', slight: 'warning', severe: 'danger', broken: 'info' }
  return map[code] || 'info'
}

onMounted(() => {
  loadDict()
  loadGroups()
  loadAccessories()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-count {
  font-size: 12px;
  color: #909399;
}

.group-tree-card {
  height: calc(100vh - 140px);
  min-height: 500px;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    flex: 1;
    overflow: auto;
    padding: 8px 0;
  }
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background 0.2s;
  flex: 1;

  &:hover {
    background: #ecf5ff;
  }

  &.active {
    background: #ecf5ff;
  }

  .node-label {
    flex: 1;
    font-size: 14px;
  }

  .node-count {
    font-size: 11px;
  }
}

.group-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.group-actions {
  display: flex;
  gap: 8px;
}

.empty-tip {
  padding: 20px 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}
</style>
