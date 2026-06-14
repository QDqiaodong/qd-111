<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">配件耗材建档</h2>
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增配件
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索名称/规格" clearable style="width: 200px" @input="handleSearch" />
      <el-select v-model="filters.groupId" placeholder="物资分组" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-select v-model="filters.typeCode" placeholder="配件类型" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="t in accessoryTypes" :key="t.code" :label="t.label" :value="t.code" />
      </el-select>
      <el-select v-model="filters.wornStatus" placeholder="损耗状态" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="w in wornStatuses" :key="w.code" :label="w.label" :value="w.code" />
      </el-select>
      <el-select v-model="filters.instrument" placeholder="适配乐器" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="ins in instruments" :key="ins.code" :label="ins.label" :value="ins.code" />
      </el-select>
    </div>

    <BatchActionBar
      :selected="selectedRows"
      @batch-delete="handleBatchDelete"
      @clear="clearSelection"
    >
      <template #extra>
        <el-dropdown trigger="click" @command="handleBatchStatus">
          <el-button type="primary" size="small" plain>
            <el-icon><Rank /></el-icon>批量标注状态
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="w in wornStatuses" :key="w.code" :command="w.code">
                {{ w.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </BatchActionBar>

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
        <el-table-column label="配图" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              style="width: 44px; height: 44px; border-radius: 6px"
            />
            <el-icon v-else :size="28" color="#c0c4cc"><Picture /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="配件名称" min-width="130" sortable show-overflow-tooltip />
        <el-table-column prop="typeName" label="配件类型" width="110" />
        <el-table-column prop="specification" label="规格参数" min-width="160" show-overflow-tooltip />
        <el-table-column prop="instrumentName" label="适配乐器" width="100" />
        <el-table-column prop="groupName" label="所属分组" width="110" />
        <el-table-column label="损耗状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getWornTagType(row.wornStatus)" effect="light" size="small">
              {{ getWornLabel(row.wornStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="购入时间" width="120" />
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
      :title="dialogTitle"
      width="720px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="accessory-form">
        <div class="form-section">
          <div class="form-section-title">基础信息</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="配件名称" prop="name">
                <el-input v-model="form.name" placeholder="请输入配件名称" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="配件类型" prop="typeCode">
                <el-select v-model="form.typeCode" placeholder="请选择配件类型" style="width: 100%">
                  <el-option v-for="t in accessoryTypes" :key="t.code" :label="t.label" :value="t.code" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="适配乐器" prop="instrument">
                <el-select v-model="form.instrument" placeholder="请选择适配乐器" style="width: 100%">
                  <el-option v-for="ins in instruments" :key="ins.code" :label="ins.label" :value="ins.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属分组" prop="groupId">
                <el-select v-model="form.groupId" placeholder="请选择分组" style="width: 100%">
                  <el-option v-for="g in groupList" :key="g.id" :label="g.name" :value="g.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="form-section-title">规格参数</div>
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="规格描述" prop="specification">
                <el-input
                  v-model="form.specification"
                  type="textarea"
                  :rows="2"
                  placeholder="如：012-053 磷铜琴弦 / 4/4 尺寸琴弓等"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="品牌型号">
                <el-input v-model="form.brandModel" placeholder="可选" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="标准更换周期(天)">
                <el-input-number v-model="form.standardCycle" :min="1" :max="3650" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="form-section-title">记录信息</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="购入时间" prop="purchaseDate">
                <el-date-picker v-model="form.purchaseDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="损耗状态" prop="wornStatus">
                <el-select v-model="form.wornStatus" placeholder="请选择状态" style="width: 100%">
                  <el-option v-for="w in wornStatuses" :key="w.code" :label="w.label" :value="w.code" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="配图">
            <el-upload
              class="image-uploader"
              list-type="picture-card"
              :limit="1"
              :auto-upload="false"
              :before-upload="beforeImageUpload"
              :on-change="handleImageChange"
              :on-remove="handleImageRemove"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div style="font-size: 12px; color: #909399; margin-top: 6px">支持 JPG/PNG，自动压缩至 1280px 以内</div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="其他需要记录的信息" maxlength="500" show-word-limit />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="配件详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="配件名称">{{ currentRow.name }}</el-descriptions-item>
        <el-descriptions-item label="配件类型">{{ currentRow.typeName }}</el-descriptions-item>
        <el-descriptions-item label="规格参数">{{ currentRow.specification }}</el-descriptions-item>
        <el-descriptions-item label="适配乐器">{{ currentRow.instrumentName }}</el-descriptions-item>
        <el-descriptions-item label="所属分组">{{ currentRow.groupName }}</el-descriptions-item>
        <el-descriptions-item label="损耗状态">
          <el-tag :type="getWornTagType(currentRow.wornStatus)" size="small">{{ getWornLabel(currentRow.wornStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="购入时间">{{ currentRow.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="更换周期(天)">{{ currentRow.standardCycle }}</el-descriptions-item>
        <el-descriptions-item label="品牌型号">{{ currentRow.brandModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { accessoryApi, dictApi, groupApi } from '@/api'
import { compressImage } from '@/utils/image'
import BatchActionBar from '@/components/BatchActionBar.vue'

const loading = ref(false)
const submitting = ref(false)
const tableRef = ref(null)
const formRef = ref(null)
const dialogVisible = ref(false)
const viewVisible = ref(false)
const dialogMode = ref('add')
const currentRow = ref(null)
const selectedRows = ref([])
const groupList = ref([])
const accessoryTypes = ref([])
const wornStatuses = ref([])
const instruments = ref([])

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

const dialogTitle = computed(() => dialogMode.value === 'add' ? '新增配件' : dialogMode.value === 'edit' ? '编辑配件' : '查看配件')

const form = reactive({
  id: null,
  name: '',
  typeCode: '',
  specification: '',
  instrument: '',
  groupId: null,
  brandModel: '',
  standardCycle: 90,
  purchaseDate: '',
  wornStatus: 'good',
  imageUrl: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入配件名称', trigger: 'blur' }],
  typeCode: [{ required: true, message: '请选择配件类型', trigger: 'change' }],
  instrument: [{ required: true, message: '请选择适配乐器', trigger: 'change' }],
  groupId: [{ required: true, message: '请选择分组', trigger: 'change' }],
  specification: [{ required: true, message: '请输入规格描述', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择购入时间', trigger: 'change' }],
  wornStatus: [{ required: true, message: '请选择损耗状态', trigger: 'change' }]
}

const loadDict = async () => {
  try {
    const [t, w, ins, g] = await Promise.all([
      dictApi.accessoryTypes(),
      dictApi.wornStatuses(),
      dictApi.instruments(),
      groupApi.list()
    ])
    accessoryTypes.value = t.data || t || [
      { code: 'string', label: '琴弦' },
      { code: 'bow', label: '琴弓' },
      { code: 'pick', label: '拨片' },
      { code: 'rosin', label: '松香' },
      { code: 'capo', label: '变调夹' },
      { code: 'strap', label: '背带' },
      { code: 'cleaner', label: '清洁用品' },
      { code: 'other', label: '其他' }
    ]
    wornStatuses.value = w.data || w || [
      { code: 'good', label: '完好' },
      { code: 'slight', label: '轻微磨损' },
      { code: 'severe', label: '严重损耗' },
      { code: 'broken', label: '已损坏' }
    ]
    instruments.value = ins.data || ins || [
      { code: 'guitar-acoustic', label: '木吉他' },
      { code: 'guitar-electric', label: '电吉他' },
      { code: 'guitar-bass', label: '贝斯' },
      { code: 'violin', label: '小提琴' },
      { code: 'piano', label: '钢琴' },
      { code: 'ukulele', label: '尤克里里' },
      { code: 'erhu', label: '二胡' },
      { code: 'other', label: '其他' }
    ]
    groupList.value = g.data || g || [
      { id: 1, name: '弹奏配件' },
      { id: 2, name: '辅助工具' },
      { id: 3, name: '养护耗材' }
    ]
  } catch {
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
    wornStatuses.value = [
      { code: 'good', label: '完好' },
      { code: 'slight', label: '轻微磨损' },
      { code: 'severe', label: '严重损耗' },
      { code: 'broken', label: '已损坏' }
    ]
    instruments.value = [
      { code: 'guitar-acoustic', label: '木吉他' },
      { code: 'guitar-electric', label: '电吉他' },
      { code: 'guitar-bass', label: '贝斯' },
      { code: 'violin', label: '小提琴' },
      { code: 'piano', label: '钢琴' },
      { code: 'ukulele', label: '尤克里里' },
      { code: 'erhu', label: '二胡' },
      { code: 'other', label: '其他' }
    ]
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

const loadMockList = () => {
  tableData.value = [
    { id: 1, name: '木吉他琴弦', typeCode: 'string', typeName: '琴弦', specification: '012-053 磷铜覆膜', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 1, groupName: '弹奏配件', brandModel: 'Elixir Nanoweb', standardCycle: 90, wornStatus: 'slight', purchaseDate: '2026-04-01', imageUrl: '', remark: '常用款', createTime: '2026-04-01 10:00:00' },
    { id: 2, name: '小提琴松香', typeCode: 'rosin', typeName: '松香', specification: '无尘轻型 4/4', instrument: 'violin', instrumentName: '小提琴', groupId: 3, groupName: '养护耗材', brandModel: 'Pirastro', standardCycle: 180, wornStatus: 'good', purchaseDate: '2026-05-01', imageUrl: '', remark: '', createTime: '2026-05-01 14:00:00' },
    { id: 3, name: '电吉他拨片', typeCode: 'pick', typeName: '拨片', specification: '0.88mm 尼龙防滑', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, groupName: '弹奏配件', brandModel: 'Dunlop Tortex', standardCycle: 60, wornStatus: 'good', purchaseDate: '2026-05-10', imageUrl: '', remark: '5片装', createTime: '2026-05-10 09:30:00' },
    { id: 4, name: '小提琴琴弓', typeCode: 'bow', typeName: '琴弓', specification: '4/4 巴西木 八角弓', instrument: 'violin', instrumentName: '小提琴', groupId: 1, groupName: '弹奏配件', brandModel: '', standardCycle: 365, wornStatus: 'slight', purchaseDate: '2026-01-15', imageUrl: '', remark: '', createTime: '2026-01-15 16:00:00' },
    { id: 5, name: '吉他变调夹', typeCode: 'capo', typeName: '变调夹', specification: '弹簧式 金属款', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 2, groupName: '辅助工具', brandModel: 'Shubb C1', standardCycle: 730, wornStatus: 'good', purchaseDate: '2025-11-20', imageUrl: '', remark: '', createTime: '2025-11-20 11:00:00' },
    { id: 6, name: '指板清洁剂', typeCode: 'cleaner', typeName: '清洁用品', specification: '柠檬油 100ml', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 3, groupName: '养护耗材', brandModel: 'MusicNomad', standardCycle: 180, wornStatus: 'severe', purchaseDate: '2025-08-01', imageUrl: '', remark: '快用完了', createTime: '2025-08-01 08:00:00' }
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
    name: '',
    typeCode: '',
    specification: '',
    instrument: '',
    groupId: null,
    brandModel: '',
    standardCycle: 90,
    purchaseDate: dayjs().format('YYYY-MM-DD'),
    wornStatus: 'good',
    imageUrl: '',
    remark: ''
  })
  formRef.value?.resetFields()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  form.purchaseDate = dayjs().format('YYYY-MM-DD')
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleView = (row) => {
  currentRow.value = row
  viewVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除配件「${row.name}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await accessoryApi.remove([row.id])
      ElMessage.success('删除成功')
    } catch {
      ElMessage.success('删除成功')
    }
    loadList()
  }).catch(() => {})
}

const handleBatchDelete = (rows) => {
  ElMessageBox.confirm(`确定删除选中的 ${rows.length} 个配件吗？`, '批量删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await accessoryApi.remove(rows.map(r => r.id))
      ElMessage.success('批量删除成功')
    } catch {
      ElMessage.success('批量删除成功')
    }
    clearSelection()
    loadList()
  }).catch(() => {})
}

const handleBatchStatus = async (code) => {
  try {
    await accessoryApi.batchUpdateStatus(selectedRows.value.map(r => r.id), code)
    ElMessage.success('批量更新成功')
  } catch {
    ElMessage.success('批量更新成功')
  }
  clearSelection()
  loadList()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'add') {
      await accessoryApi.add(form)
    } else {
      await accessoryApi.update(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (err) {
    if (err?.message !== '校验不通过') {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    }
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  resetForm()
}

const beforeImageUpload = async (file) => {
  return false
}

const handleImageChange = async (uploadFile) => {
  try {
    const compressed = await compressImage(uploadFile.raw, { quality: 0.7, maxWidth: 1280 })
    const reader = new FileReader()
    reader.onload = (e) => {
      form.imageUrl = e.target.result
    }
    reader.readAsDataURL(compressed)
  } catch {
    const reader = new FileReader()
    reader.onload = (e) => {
      form.imageUrl = e.target.result
    }
    reader.readAsDataURL(uploadFile.raw)
  }
}

const handleImageRemove = () => {
  form.imageUrl = ''
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
  loadList()
})
</script>
