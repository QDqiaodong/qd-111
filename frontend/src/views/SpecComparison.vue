<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">耗材规格对照</h2>
        <div class="page-subtitle">按乐器类型集中展示琴弦、拨片、松香等配件的规格对照信息</div>
      </div>
      <div class="table-toolbar">
        <el-button @click="expandAll">
          <el-icon><ArrowDown /></el-icon>全部展开
        </el-button>
        <el-button @click="collapseAll">
          <el-icon><ArrowRight /></el-icon>全部收起
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索名称/规格/品牌" clearable style="width: 240px" @input="handleFilter" />
      <el-select v-model="filters.typeCode" placeholder="配件类型" clearable style="width: 140px" @change="handleFilter">
        <el-option v-for="t in accessoryTypes" :key="t.code" :label="t.label" :value="t.code" />
      </el-select>
      <el-select v-model="filters.wornStatus" placeholder="损耗状态" clearable style="width: 130px" @change="handleFilter">
        <el-option v-for="w in wornStatuses" :key="w.code" :label="w.label" :value="w.code" />
      </el-select>
      <el-tag v-if="activeInstrument" closable type="primary" effect="light" class="filter-tag" @close="clearInstrumentFilter">
        当前乐器：{{ activeInstrumentLabel }}
      </el-tag>
    </div>

    <div v-loading="loading" class="comparison-container">
      <div v-if="groupedData.length === 0" class="empty-wrap">
        <el-empty description="暂无配件数据，请先在配件建档中添加" />
      </div>

      <div
        v-for="group in groupedData"
        :key="group.instrumentCode"
        :id="'instrument-' + group.instrumentCode"
        class="instrument-group"
        :class="{ 'group-highlight': highlightInstrument === group.instrumentCode }"
      >
        <div class="group-header" @click="toggleGroup(group.instrumentCode)">
          <div class="group-title">
            <el-icon class="collapse-icon" :class="{ 'is-expanded': expandedInstruments.includes(group.instrumentCode) }">
              <ArrowRight />
            </el-icon>
            <el-icon class="instrument-icon"><Goods /></el-icon>
            <span class="instrument-name">{{ group.instrumentName }}</span>
            <el-tag size="small" type="info" effect="plain" class="count-tag">
              {{ group.accessories.length }} 个配件
            </el-tag>
          </div>
          <div class="group-summary">
            <span v-for="t in group.typeSummary" :key="t.code" class="summary-item">
              {{ t.label }} {{ t.count }}
            </span>
          </div>
        </div>

        <el-collapse-transition>
          <div v-show="expandedInstruments.includes(group.instrumentCode)" class="group-content">
            <div
              v-for="typeGroup in group.typeGroups"
              :key="typeGroup.typeCode"
              :id="typeGroup.typeCode + '-' + group.instrumentCode"
              class="type-section"
              :class="{ 'section-highlight': highlightSection === (typeGroup.typeCode + '-' + group.instrumentCode) }"
            >
              <div class="type-header">
                <div class="type-title">
                  <div class="type-badge" :style="{ background: getTypeColor(typeGroup.typeCode) }">
                    {{ typeGroup.typeLabel }}
                  </div>
                  <span class="type-count">{{ typeGroup.accessories.length }} 款</span>
                </div>
              </div>

              <div class="spec-table-wrap">
                <el-table
                  :data="typeGroup.accessories"
                  :row-key="row => row.id"
                  stripe
                  size="small"
                  style="width: 100%"
                  :row-class-name="getRowClassName"
                >
                  <el-table-column label="配图" width="64" align="center">
                    <template #default="{ row }">
                      <el-image
                        v-if="row.imageUrl"
                        :src="row.imageUrl"
                        :preview-src-list="[row.imageUrl]"
                        fit="cover"
                        style="width: 36px; height: 36px; border-radius: 4px"
                      />
                      <el-icon v-else :size="22" color="#c0c4cc"><Picture /></el-icon>
                    </template>
                  </el-table-column>
                  <el-table-column prop="name" label="配件名称" min-width="130" show-overflow-tooltip />
                  <el-table-column label="规格参数" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="spec-text">{{ row.specification || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="品牌型号" min-width="150" show-overflow-tooltip>
                    <template #default="{ row }">
                      <el-tag v-if="row.brandModel" size="small" type="success" effect="plain" class="brand-tag">
                        {{ row.brandModel }}
                      </el-tag>
                      <span v-else class="text-muted">-</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="标准周期" width="100" align="center">
                    <template #default="{ row }">
                      <span class="cycle-text">{{ row.standardCycle }} 天</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="损耗状态" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getWornTagType(row.wornStatus)" effect="light" size="small">
                        {{ getWornLabel(row.wornStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="适用说明" min-width="160" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="remark-text">{{ row.remark || getDefaultRemark(row) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="110" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" link size="small" @click="viewAccessory(row)">查看详情</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <el-dialog v-model="viewVisible" title="配件详情" width="640px" destroy-on-close>
      <template v-if="currentRow">
        <el-descriptions :column="2" border size="default">
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
      </template>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, ArrowDown, Goods, Picture } from '@element-plus/icons-vue'
import { accessoryApi, dictApi, groupApi } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const allAccessories = ref([])
const accessoryTypes = ref([])
const wornStatuses = ref([])
const instruments = ref([])
const groupList = ref([])
const expandedInstruments = ref([])
const viewVisible = ref(false)
const currentRow = ref(null)
const highlightInstrument = ref('')
const highlightSection = ref('')
const highlightAccessory = ref(null)

const filters = reactive({
  keyword: '',
  typeCode: '',
  wornStatus: '',
  instrument: ''
})

const activeInstrument = computed(() => filters.instrument)
const activeInstrumentLabel = computed(() => {
  const ins = instruments.value.find(i => i.code === filters.instrument)
  return ins ? ins.label : ''
})

const typeColors = {
  string: '#409eff',
  bow: '#67c23a',
  pick: '#e6a23c',
  rosin: '#909399',
  capo: '#8e44ad',
  strap: '#16a085',
  cleaner: '#d35400',
  other: '#c0c4cc'
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

const loadAllAccessories = async () => {
  loading.value = true
  try {
    const res = await accessoryApi.list()
    allAccessories.value = res.data || res || []
    if (allAccessories.value.length === 0) {
      loadMockData()
    }
  } catch {
    loadMockData()
  } finally {
    loading.value = false
  }
}

const loadMockData = () => {
  allAccessories.value = [
    { id: 1, name: '木吉他琴弦', typeCode: 'string', typeName: '琴弦', specification: '012-053 磷铜覆膜', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 1, groupName: '弹奏配件', brandModel: 'Elixir Nanoweb', standardCycle: 90, wornStatus: 'slight', purchaseDate: '2026-04-01', imageUrl: '', remark: '常用款，音色温暖持久', createTime: '2026-04-01 10:00:00' },
    { id: 2, name: '小提琴松香', typeCode: 'rosin', typeName: '松香', specification: '无尘轻型 4/4', instrument: 'violin', instrumentName: '小提琴', groupId: 3, groupName: '养护耗材', brandModel: 'Pirastro', standardCycle: 180, wornStatus: 'good', purchaseDate: '2026-05-01', imageUrl: '', remark: '适用于小提琴演奏', createTime: '2026-05-01 14:00:00' },
    { id: 3, name: '电吉他拨片', typeCode: 'pick', typeName: '拨片', specification: '0.88mm 尼龙防滑', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, groupName: '弹奏配件', brandModel: 'Dunlop Tortex', standardCycle: 60, wornStatus: 'good', purchaseDate: '2026-05-10', imageUrl: '', remark: '5片装，适合速弹', createTime: '2026-05-10 09:30:00' },
    { id: 4, name: '小提琴琴弓', typeCode: 'bow', typeName: '琴弓', specification: '4/4 巴西木 八角弓', instrument: 'violin', instrumentName: '小提琴', groupId: 1, groupName: '弹奏配件', brandModel: '', standardCycle: 365, wornStatus: 'slight', purchaseDate: '2026-01-15', imageUrl: '', remark: '适合进阶学习者', createTime: '2026-01-15 16:00:00' },
    { id: 5, name: '吉他变调夹', typeCode: 'capo', typeName: '变调夹', specification: '弹簧式 金属款', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 2, groupName: '辅助工具', brandModel: 'Shubb C1', standardCycle: 730, wornStatus: 'good', purchaseDate: '2025-11-20', imageUrl: '', remark: '耐用型，夹持力好', createTime: '2025-11-20 11:00:00' },
    { id: 6, name: '指板清洁剂', typeCode: 'cleaner', typeName: '清洁用品', specification: '柠檬油 100ml', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 3, groupName: '养护耗材', brandModel: 'MusicNomad', standardCycle: 180, wornStatus: 'severe', purchaseDate: '2025-08-01', imageUrl: '', remark: '深度清洁保养用', createTime: '2025-08-01 08:00:00' },
    { id: 7, name: '电吉他琴弦', typeCode: 'string', typeName: '琴弦', specification: '009-042 镀镍钢', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, groupName: '弹奏配件', brandModel: 'Ernie Ball', standardCycle: 60, wornStatus: 'good', purchaseDate: '2026-05-20', imageUrl: '', remark: '经典摇滚音色', createTime: '2026-05-20 11:00:00' },
    { id: 8, name: '木吉他拨片', typeCode: 'pick', typeName: '拨片', specification: '0.71mm 赛璐珞', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 1, groupName: '弹奏配件', brandModel: 'Fender', standardCycle: 90, wornStatus: 'good', purchaseDate: '2026-03-15', imageUrl: '', remark: '适合指弹和扫弦', createTime: '2026-03-15 14:20:00' },
    { id: 9, name: '小提琴琴弦', typeCode: 'string', typeName: '琴弦', specification: '4/4 钢弦 套装', instrument: 'violin', instrumentName: '小提琴', groupId: 1, groupName: '弹奏配件', brandModel: 'Dominant', standardCycle: 120, wornStatus: 'slight', purchaseDate: '2026-02-10', imageUrl: '', remark: '专业演奏级', createTime: '2026-02-10 09:00:00' },
    { id: 10, name: '贝斯琴弦', typeCode: 'string', typeName: '琴弦', specification: '045-105 镍钢', instrument: 'guitar-bass', instrumentName: '贝斯', groupId: 1, groupName: '弹奏配件', brandModel: 'DR Strings', standardCycle: 150, wornStatus: 'good', purchaseDate: '2026-04-20', imageUrl: '', remark: '低音浑厚有力', createTime: '2026-04-20 16:30:00' },
    { id: 11, name: '尤克里里琴弦', typeCode: 'string', typeName: '琴弦', specification: '23寸 高分子氟碳', instrument: 'ukulele', instrumentName: '尤克里里', groupId: 1, groupName: '弹奏配件', brandModel: 'Aquila', standardCycle: 180, wornStatus: 'good', purchaseDate: '2026-05-01', imageUrl: '', remark: '音色明亮', createTime: '2026-05-01 10:15:00' },
    { id: 12, name: '二胡松香', typeCode: 'rosin', typeName: '松香', specification: '微尘型 专业级', instrument: 'erhu', instrumentName: '二胡', groupId: 3, groupName: '养护耗材', brandModel: '李古堂', standardCycle: 200, wornStatus: 'good', purchaseDate: '2026-03-01', imageUrl: '', remark: '适用于民族弓弦乐器', createTime: '2026-03-01 13:45:00' }
  ]
}

const filteredAccessories = computed(() => {
  let result = [...allAccessories.value]

  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    result = result.filter(a =>
      a.name.toLowerCase().includes(kw) ||
      (a.specification && a.specification.toLowerCase().includes(kw)) ||
      (a.brandModel && a.brandModel.toLowerCase().includes(kw)) ||
      (a.remark && a.remark.toLowerCase().includes(kw))
    )
  }

  if (filters.typeCode) {
    result = result.filter(a => a.typeCode === filters.typeCode)
  }

  if (filters.wornStatus) {
    result = result.filter(a => a.wornStatus === filters.wornStatus)
  }

  if (filters.instrument) {
    result = result.filter(a => a.instrument === filters.instrument)
  }

  return result
})

const groupedData = computed(() => {
  const instrumentMap = {}

  filteredAccessories.value.forEach(acc => {
    if (!instrumentMap[acc.instrument]) {
      instrumentMap[acc.instrument] = {
        instrumentCode: acc.instrument,
        instrumentName: acc.instrumentName,
        accessories: [],
        typeGroups: [],
        typeSummary: []
      }
    }
    instrumentMap[acc.instrument].accessories.push(acc)
  })

  Object.values(instrumentMap).forEach(group => {
    const typeMap = {}
    group.accessories.forEach(acc => {
      if (!typeMap[acc.typeCode]) {
        const typeInfo = accessoryTypes.value.find(t => t.code === acc.typeCode)
        typeMap[acc.typeCode] = {
          typeCode: acc.typeCode,
          typeLabel: typeInfo ? typeInfo.label : acc.typeCode,
          accessories: []
        }
      }
      typeMap[acc.typeCode].accessories.push(acc)
    })

    group.typeGroups = Object.values(typeMap).sort((a, b) => {
      const order = ['string', 'bow', 'pick', 'rosin', 'capo', 'strap', 'cleaner', 'other']
      return order.indexOf(a.typeCode) - order.indexOf(b.typeCode)
    })

    group.typeSummary = group.typeGroups.map(tg => ({
      code: tg.typeCode,
      label: tg.typeLabel,
      count: tg.accessories.length
    }))
  })

  return Object.values(instrumentMap).sort((a, b) => {
    const order = ['guitar-acoustic', 'guitar-electric', 'guitar-bass', 'violin', 'piano', 'ukulele', 'erhu', 'other']
    return order.indexOf(a.instrumentCode) - order.indexOf(b.instrumentCode)
  })
})

const toggleGroup = (code) => {
  const idx = expandedInstruments.value.indexOf(code)
  if (idx > -1) {
    expandedInstruments.value.splice(idx, 1)
  } else {
    expandedInstruments.value.push(code)
  }
}

const expandAll = () => {
  expandedInstruments.value = groupedData.value.map(g => g.instrumentCode)
}

const collapseAll = () => {
  expandedInstruments.value = []
}

const handleFilter = () => {
}

const clearInstrumentFilter = () => {
  filters.instrument = ''
}

const getTypeColor = (code) => {
  return typeColors[code] || '#c0c4cc'
}

const getWornLabel = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.label : code
}

const getWornTagType = (code) => {
  const map = { good: 'success', slight: 'warning', severe: 'danger', broken: 'info' }
  return map[code] || 'info'
}

const getDefaultRemark = (row) => {
  const remarks = {
    string: `标准更换周期 ${row.standardCycle} 天，建议定期更换保持音色`,
    bow: `注意定期上松香，保持弓毛状态`,
    pick: `磨损严重时及时更换，避免影响手感`,
    rosin: `适量使用，避免过多影响音色`,
    capo: `使用后及时取下，避免长时间压迫琴颈`,
    strap: `定期检查扣件，确保安全可靠`,
    cleaner: `按说明使用，避免接触漆面`,
    other: `按使用说明正确维护`
  }
  return remarks[row.typeCode] || '请按使用说明正确维护'
}

const viewAccessory = (row) => {
  currentRow.value = row
  viewVisible.value = true
}

const getRowClassName = ({ row }) => {
  return highlightAccessory.value === row.id ? `row-highlight accessory-row-${row.id}` : `accessory-row-${row.id}`
}

const scrollToElement = (selector, offset = 100) => {
  const el = document.querySelector(selector)
  if (el) {
    const top = el.getBoundingClientRect().top + window.pageYOffset - offset
    window.scrollTo({ top, behavior: 'smooth' })
    return true
  }
  return false
}

const handleRouteParams = async () => {
  await nextTick()

  const instrumentParam = route.query.instrument
  const accessoryId = route.query.accessoryId

  let targetInstrumentCode = ''

  if (instrumentParam) {
    const instrument = instruments.value.find(i => i.label === instrumentParam) || instruments.value.find(i => i.code === instrumentParam)
    if (instrument) {
      targetInstrumentCode = instrument.code
      filters.instrument = instrument.code
    }
  }

  if (accessoryId) {
    const acc = allAccessories.value.find(a => a.id === Number(accessoryId))
    if (acc) {
      targetInstrumentCode = acc.instrument

      if (!expandedInstruments.value.includes(acc.instrument)) {
        expandedInstruments.value.push(acc.instrument)
      }

      await nextTick()
      await nextTick()

      highlightInstrument.value = acc.instrument
      highlightSection.value = acc.typeCode + '-' + acc.instrument
      highlightAccessory.value = acc.id

      setTimeout(() => {
        scrollToElement('.accessory-row-' + acc.id, 140) || scrollToElement('#instrument-' + acc.instrument, 80)
      }, 100)

      setTimeout(() => {
        highlightInstrument.value = ''
        highlightSection.value = ''
        highlightAccessory.value = null
      }, 3000)

      return
    }
  }

  if (targetInstrumentCode && !expandedInstruments.value.includes(targetInstrumentCode)) {
    expandedInstruments.value.push(targetInstrumentCode)
    await nextTick()
    setTimeout(() => {
      scrollToElement('#instrument-' + targetInstrumentCode, 80)
    }, 100)
  }

  if (expandedInstruments.value.length === 0 && groupedData.value.length > 0) {
    expandedInstruments.value = [groupedData.value[0].instrumentCode]
  }
}

onMounted(async () => {
  await loadDict()
  await loadAllAccessories()
  await handleRouteParams()
})
</script>

<style lang="scss" scoped>
.page-subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.filter-tag {
  margin-left: 8px;
}

.comparison-container {
  margin-top: 16px;
}

.empty-wrap {
  padding: 40px 0;
}

.instrument-group {
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.3s ease;

  &.group-highlight {
    box-shadow: 0 0 0 2px #409eff, 0 4px 16px rgba(64, 158, 255, 0.2);
    animation: pulse-highlight 1.5s ease-in-out;
  }
}

@keyframes pulse-highlight {
  0%, 100% {
    box-shadow: 0 0 0 2px #409eff, 0 4px 16px rgba(64, 158, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 0 2px #66b1ff, 0 4px 20px rgba(64, 158, 255, 0.35);
  }
}

.group-header {
  padding: 14px 20px;
  background: linear-gradient(135deg, #fafbfc 0%, #f4f8fb 100%);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.2s ease;

  &:hover {
    background: linear-gradient(135deg, #f4f8fb 0%, #eef3f8 100%);
  }
}

.group-title {
  display: flex;
  align-items: center;
  gap: 10px;

  .collapse-icon {
    font-size: 14px;
    color: #909399;
    transition: transform 0.3s ease;

    &.is-expanded {
      transform: rotate(90deg);
    }
  }

  .instrument-icon {
    font-size: 18px;
    color: #409eff;
  }

  .instrument-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .count-tag {
    margin-left: 4px;
  }
}

.group-summary {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;

  .summary-item {
    padding: 2px 8px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }
}

.group-content {
  padding: 16px 20px 20px;
}

.type-section {
  margin-bottom: 20px;
  border: 1px solid #f2f6fc;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.3s ease;

  &:last-child {
    margin-bottom: 0;
  }

  &.section-highlight {
    box-shadow: 0 0 0 2px #67c23a, 0 2px 12px rgba(103, 194, 58, 0.2);
    animation: pulse-section 1.5s ease-in-out;
  }
}

@keyframes pulse-section {
  0%, 100% {
    box-shadow: 0 0 0 2px #67c23a, 0 2px 12px rgba(103, 194, 58, 0.2);
  }
  50% {
    box-shadow: 0 0 0 2px #85ce61, 0 2px 16px rgba(103, 194, 58, 0.35);
  }
}

.type-header {
  padding: 10px 16px;
  background: #fafbfc;
  border-bottom: 1px solid #f2f6fc;
}

.type-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-badge {
  padding: 4px 12px;
  border-radius: 4px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
}

.type-count {
  font-size: 12px;
  color: #909399;
}

.spec-table-wrap {
  padding: 0;

  :deep(.row-highlight) {
    td {
      background-color: #ecf5ff !important;
      animation: row-pulse 1.5s ease-in-out;
    }
  }
}

@keyframes row-pulse {
  0%, 100% {
    background-color: #ecf5ff;
  }
  50% {
    background-color: #d9ecff;
  }
}

.spec-text {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
}

.brand-tag {
  font-weight: 500;
}

.cycle-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.remark-text {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.text-muted {
  color: #c0c4cc;
}
</style>
