<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">套装耗材档案</h2>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增套装
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索套装名称/说明" clearable style="width: 220px" @input="handleSearch" />
      <el-select v-model="filters.instrument" placeholder="适配乐器" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="ins in instruments" :key="ins.code" :label="ins.label" :value="ins.code" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px" @change="handleSearch">
        <el-option v-for="s in setStatuses" :key="s.code" :label="s.label" :value="s.code" />
      </el-select>
    </div>

    <el-card class="card-shadow" shadow="never" body-style="padding: 0">
      <el-table :data="tableData" stripe style="width: 100%" v-loading="loading" empty-text="暂无套装，请新增">
        <el-table-column prop="name" label="套装名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="instrumentName" label="适配乐器" width="110" />
        <el-table-column label="包含配件" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain" size="small">{{ row.itemCount || 0 }} 项</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="套装说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
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
      :title="dialogMode === 'add' ? '新增套装' : '编辑套装'"
      width="900px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-section">
          <div class="form-section-title">基础信息</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="套装名称" prop="name">
                <el-input v-model="form.name" placeholder="如：木吉他日常保养套装" maxlength="100" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="适配乐器" prop="instrument">
                <el-select v-model="form.instrument" placeholder="请选择适配乐器" style="width: 100%">
                  <el-option v-for="ins in instruments" :key="ins.code" :label="ins.label" :value="ins.code" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="状态">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option v-for="s in setStatuses" :key="s.code" :label="s.label" :value="s.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="套装说明">
                <el-input v-model="form.description" placeholder="可选" maxlength="200" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            <span>套装结构（{{ form.items.length }} 项）</span>
            <el-button type="primary" size="small" plain style="margin-left: 12px" @click="openPicker">
              <el-icon><Plus /></el-icon>添加配件
            </el-button>
          </div>
          <el-table :data="form.items" stripe size="small" empty-text="请添加配件组成套装" style="width: 100%">
            <el-table-column type="index" label="序" width="50" align="center" />
            <el-table-column prop="accessoryName" label="配件名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="typeName" label="类型" width="90" />
            <el-table-column prop="specification" label="规格" min-width="140" show-overflow-tooltip />
            <el-table-column prop="groupName" label="分组" width="100" />
            <el-table-column label="数量" width="110" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="1" :max="999" size="small" controls-position="right" style="width: 90px" />
              </template>
            </el-table-column>
            <el-table-column label="排序" width="100" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.sortOrder" :min="0" :max="999" size="small" controls-position="right" style="width: 80px" />
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="可选" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeItem($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pickerVisible" title="选择配件" width="720px" destroy-on-close>
      <div class="filter-bar" style="margin-bottom: 12px; padding: 0">
        <el-input v-model="pickerKeyword" placeholder="搜索配件名称/规格" clearable style="width: 220px" />
        <el-tag v-if="form.instrument" type="info" effect="plain">
          仅展示适配「{{ getInstrumentLabel(form.instrument) }}」的配件
        </el-tag>
        <el-tag v-else type="warning" effect="plain">未选择乐器，展示全部配件</el-tag>
      </div>
      <el-table
        :data="availableAccessories"
        stripe
        size="small"
        max-height="380"
        @selection-change="handlePickerSelection"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="name" label="配件名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="typeName" label="类型" width="90" />
        <el-table-column prop="specification" label="规格" min-width="140" show-overflow-tooltip />
        <el-table-column prop="instrumentName" label="适配乐器" width="100" />
        <el-table-column prop="groupName" label="分组" width="100" />
      </el-table>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPicker" :disabled="pickerSelected.length === 0">
          添加（{{ pickerSelected.length }}）
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="套装详情" width="820px" destroy-on-close>
      <template v-if="viewData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="套装名称">{{ viewData.name }}</el-descriptions-item>
          <el-descriptions-item label="适配乐器">{{ viewData.instrumentName }}</el-descriptions-item>
          <el-descriptions-item label="包含配件">{{ viewData.itemCount }} 项</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="viewData.status === 'enabled' ? 'success' : 'info'" size="small">
              {{ getStatusLabel(viewData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="套装说明" :span="2">{{ viewData.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="view-items-title">
          <el-icon color="#409eff"><Box /></el-icon>
          <span>套装明细</span>
        </div>
        <el-table :data="viewData.items" stripe size="small" border>
          <el-table-column type="index" label="序" width="55" align="center" />
          <el-table-column prop="accessoryName" label="配件名称" min-width="130" show-overflow-tooltip />
          <el-table-column prop="typeName" label="类型" width="90" />
          <el-table-column prop="specification" label="规格" min-width="150" show-overflow-tooltip />
          <el-table-column prop="groupName" label="分组" width="100" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Box } from '@element-plus/icons-vue'
import { setApi, accessoryApi, dictApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const dialogVisible = ref(false)
const pickerVisible = ref(false)
const viewVisible = ref(false)
const dialogMode = ref('add')

const instruments = ref([])
const accessoryList = ref([])
const setStatuses = ref([
  { code: 'enabled', label: '启用' },
  { code: 'disabled', label: '停用' }
])

const filters = reactive({ keyword: '', instrument: '', status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref([])

const pickerKeyword = ref('')
const pickerSelected = ref([])

const defaultForm = () => ({
  id: null,
  name: '',
  instrument: '',
  description: '',
  coverUrl: '',
  status: 'enabled',
  items: []
})
const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入套装名称', trigger: 'blur' }],
  instrument: [{ required: true, message: '请选择适配乐器', trigger: 'change' }]
}

const availableAccessories = computed(() => {
  const existingIds = form.items.map(i => i.accessoryId)
  let list = accessoryList.value
  if (form.instrument) {
    list = list.filter(a => a.instrument === form.instrument)
  }
  if (pickerKeyword.value) {
    const kw = pickerKeyword.value.toLowerCase()
    list = list.filter(a => (a.name || '').toLowerCase().includes(kw)
      || (a.specification || '').toLowerCase().includes(kw))
  }
  return list.filter(a => !existingIds.includes(a.id))
})

const loadDict = async () => {
  try {
    const res = await dictApi.instruments()
    instruments.value = res.data || res || []
  } catch {
    instruments.value = []
  }
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
    const res = await setApi.page({
      ...filters,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
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

const resetForm = () => {
  Object.assign(form, defaultForm())
  formRef.value?.resetFields()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogMode.value = 'edit'
  resetForm()
  try {
    const res = await setApi.getById(row.id)
    const data = res.data || res
    if (data) {
      Object.assign(form, {
        id: data.id,
        name: data.name,
        instrument: data.instrument,
        description: data.description || '',
        coverUrl: data.coverUrl || '',
        status: data.status || 'enabled',
        items: (data.items || []).map(it => ({
          accessoryId: it.accessoryId,
          accessoryName: it.accessoryName,
          typeCode: it.typeCode,
          typeName: it.typeName,
          specification: it.specification,
          instrumentName: it.instrumentName,
          groupName: it.groupName,
          quantity: it.quantity || 1,
          sortOrder: it.sortOrder,
          remark: it.remark || ''
        }))
      })
    }
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error(e?.message || '加载套装详情失败')
  }
}

const handleView = async (row) => {
  try {
    const res = await setApi.getById(row.id)
    viewData.value = res.data || res
    viewVisible.value = true
  } catch (e) {
    ElMessage.error(e?.message || '加载套装详情失败')
  }
}
const viewData = ref(null)

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除套装「${row.name}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await setApi.remove([row.id])
      ElMessage.success('删除成功')
      loadList()
    } catch (e) {
      if (e?.message !== 'cancel') {
        ElMessage.error(e?.message || '删除失败，请稍后重试')
      }
    }
  }).catch(() => {})
}

const openPicker = () => {
  pickerKeyword.value = ''
  pickerSelected.value = []
  pickerVisible.value = true
}

const handlePickerSelection = (rows) => {
  pickerSelected.value = rows
}

const confirmPicker = () => {
  const next = form.items.length
  pickerSelected.value.forEach((acc, idx) => {
    form.items.push({
      accessoryId: acc.id,
      accessoryName: acc.name,
      typeCode: acc.typeCode,
      typeName: acc.typeName,
      specification: acc.specification,
      instrumentName: acc.instrumentName,
      groupName: acc.groupName,
      quantity: 1,
      sortOrder: next + idx + 1,
      remark: ''
    })
  })
  pickerVisible.value = false
  ElMessage.success(`已添加 ${pickerSelected.value.length} 个配件`)
}

const removeItem = (index) => {
  form.items.splice(index, 1)
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.items.length === 0) {
    ElMessage.warning('套装至少包含一个配件')
    return
  }
  const payload = {
    id: form.id,
    name: form.name,
    instrument: form.instrument,
    description: form.description,
    coverUrl: form.coverUrl,
    status: form.status,
    items: form.items.map((it, idx) => ({
      accessoryId: it.accessoryId,
      quantity: it.quantity || 1,
      sortOrder: it.sortOrder != null ? it.sortOrder : idx + 1,
      remark: it.remark || ''
    }))
  }
  submitting.value = true
  try {
    if (dialogMode.value === 'add') {
      await setApi.add(payload)
      ElMessage.success('保存成功')
    } else {
      await setApi.update(payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (err) {
    if (err?.message === '校验不通过') return
    ElMessage.error(err?.message || '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const getStatusLabel = (code) => {
  const item = setStatuses.value.find(s => s.code === code)
  return item ? item.label : code
}

const getInstrumentLabel = (code) => {
  const item = instruments.value.find(i => i.code === code)
  return item ? item.label : code
}

onMounted(() => {
  loadDict()
  loadAccessories()
  loadList()
})
</script>

<style lang="scss" scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
}

.form-section-title {
  display: flex;
  align-items: center;
}

.view-items-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
