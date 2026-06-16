<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">分组陈列看板</h2>
      <div class="page-subtitle">快速盘点个人配件箱，按分组一览全局</div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.groupCount }}</div>
            <div class="stat-label">物资分组</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.totalAccessories }}</div>
            <div class="stat-label">配件总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
            <el-icon><VideoCamera /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.instrumentCount }}</div>
            <div class="stat-label">涉及乐器</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
            <el-icon><RefreshRight /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ summary.recentReplacements }}</div>
            <div class="stat-label">近期更换</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索分组名称" clearable style="width: 200px" @input="handleSearch" />
      <el-select v-model="filters.wornStatus" placeholder="损耗状态筛选" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="w in wornStatuses" :key="w.code" :label="w.label" :value="w.code" />
      </el-select>
      <el-button @click="clearFilters">
        <el-icon><RefreshLeft /></el-icon>重置
      </el-button>
    </div>

    <div class="group-grid" v-loading="loading">
      <el-card
        v-for="group in filteredGroups"
        :key="group.id"
        class="group-card card-shadow"
        shadow="never"
        body-style="padding: 0"
      >
        <div class="group-card-header" :style="{ background: getGroupGradient(group.id) }">
          <div class="group-header-content">
            <div class="group-title-row">
              <el-icon :size="20"><FolderOpened /></el-icon>
              <span class="group-title">{{ group.name }}</span>
            </div>
            <div class="group-count-badge">
              <span class="count-num">{{ group.accessoryCount }}</span>
              <span class="count-label">件配件</span>
            </div>
          </div>
          <div v-if="group.description" class="group-description">{{ group.description }}</div>
        </div>

        <div class="group-card-body">
          <div class="info-section">
            <div class="section-label">
              <el-icon color="#409eff"><VideoCamera /></el-icon>
              <span>主要乐器</span>
            </div>
            <div class="instrument-tags">
              <el-tag
                v-for="inst in group.mainInstruments"
                :key="inst.name"
                size="small"
                type="primary"
                effect="light"
              >
                {{ inst.name }}
                <span class="tag-count">{{ inst.count }}</span>
              </el-tag>
              <span v-if="group.mainInstruments.length === 0" class="empty-text">暂无</span>
            </div>
          </div>

          <div class="info-section">
            <div class="section-label">
              <el-icon :color="getMostWornColor(group.mostWornStatus)"><Warning /></el-icon>
              <span>最常见损耗</span>
            </div>
            <div class="worn-status-row">
              <span
                class="worn-badge"
                :style="{ background: getMostWornColor(group.mostWornStatus) + '18', color: getMostWornColor(group.mostWornStatus) }"
              >
                <span class="worn-dot" :style="{ background: getMostWornColor(group.mostWornStatus) }"></span>
                {{ getMostWornLabel(group.mostWornStatus) }}
              </span>
              <span class="worn-count">{{ group.mostWornCount }} 件</span>
            </div>
            <div class="worn-distribution">
              <div v-for="w in group.wornDistribution" :key="w.code" class="worn-dist-item">
                <span class="worn-dist-label">{{ w.label }}</span>
                <div class="worn-dist-bar">
                  <div
                    class="worn-dist-fill"
                    :style="{ width: w.percent + '%', background: w.color }"
                  ></div>
                </div>
                <span class="worn-dist-count">{{ w.count }}</span>
              </div>
            </div>
          </div>

          <div class="info-section">
            <div class="section-label">
              <el-icon color="#67c23a"><Clock /></el-icon>
              <span>最近更换</span>
            </div>
            <div v-if="group.lastReplacement" class="replacement-summary">
              <div class="replacement-item">
                <span class="replacement-name">{{ group.lastReplacement.accessoryName }}</span>
                <span class="replacement-date">{{ group.lastReplacement.replaceDate }}</span>
              </div>
              <div class="replacement-meta">
                <el-tag size="small" type="success" effect="plain">
                  近30天 {{ group.recentReplacementCount }} 次
                </el-tag>
              </div>
            </div>
            <div v-else class="no-replacement">
              <el-icon color="#c0c4cc"><CircleClose /></el-icon>
              <span>暂无更换记录</span>
            </div>
          </div>
        </div>

        <div class="group-card-footer">
          <el-button type="primary" plain size="small" @click="goToGroup(group.id)">
            查看详情
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>

      <el-empty v-if="filteredGroups.length === 0 && !loading" description="暂无匹配的分组" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { groupApi, accessoryApi, replacementApi, dictApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const groupList = ref([])
const allAccessories = ref([])
const allReplacements = ref([])
const wornStatuses = ref([
  { code: 'good', label: '完好', color: '#67c23a' },
  { code: 'slight', label: '轻微磨损', color: '#e6a23c' },
  { code: 'severe', label: '严重损耗', color: '#f56c6c' },
  { code: 'broken', label: '已损坏', color: '#909399' }
])

const filters = reactive({
  keyword: '',
  wornStatus: ''
})

const gradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
]

const summary = computed(() => {
  const groups = groupDisplayData.value
  const instruments = new Set()
  let totalAcc = 0
  let recentRepl = 0
  groups.forEach(g => {
    totalAcc += g.accessoryCount
    g.mainInstruments.forEach(i => instruments.add(i.name))
    recentRepl += g.recentReplacementCount
  })
  return {
    groupCount: groups.length,
    totalAccessories: totalAcc,
    instrumentCount: instruments.size,
    recentReplacements: recentRepl
  }
})

const getGroupGradient = (id) => {
  const index = (id - 1) % gradients.length
  return gradients[index]
}

const getMostWornColor = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.color : '#909399'
}

const getMostWornLabel = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.label : code
}

const groupDisplayData = computed(() => {
  const groups = [...groupList.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  return groups.map(group => {
    const accessories = allAccessories.value.filter(a => a.groupId === group.id)
    const accCount = accessories.length

    const instrumentMap = new Map()
    accessories.forEach(a => {
      const name = a.instrumentName || a.instrument || '未知'
      instrumentMap.set(name, (instrumentMap.get(name) || 0) + 1)
    })
    const mainInstruments = Array.from(instrumentMap.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 3)

    const wornCounts = { good: 0, slight: 0, severe: 0, broken: 0 }
    accessories.forEach(a => {
      if (wornCounts[a.wornStatus] !== undefined) {
        wornCounts[a.wornStatus]++
      }
    })

    let mostWornStatus = 'good'
    let mostWornCount = 0
    Object.entries(wornCounts).forEach(([code, count]) => {
      if (count > mostWornCount) {
        mostWornCount = count
        mostWornStatus = code
      }
    })

    const wornDistribution = wornStatuses.value.map(w => ({
      code: w.code,
      label: w.label,
      color: w.color,
      count: wornCounts[w.code] || 0,
      percent: accCount > 0 ? Math.round((wornCounts[w.code] || 0) / accCount * 100) : 0
    }))

    const groupReplacements = allReplacements.value.filter(r => {
      const acc = allAccessories.value.find(a => a.id === r.accessoryId)
      return acc && acc.groupId === group.id
    })

    const lastReplacement = groupReplacements.length > 0
      ? groupReplacements.sort((a, b) => new Date(b.replaceDate) - new Date(a.replaceDate))[0]
      : null

    const thirtyDaysAgo = new Date()
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
    const recentReplacementCount = groupReplacements.filter(r => {
      return new Date(r.replaceDate) >= thirtyDaysAgo
    }).length

    return {
      ...group,
      accessoryCount: accCount,
      mainInstruments,
      mostWornStatus: accCount > 0 ? mostWornStatus : null,
      mostWornCount,
      wornDistribution,
      lastReplacement,
      recentReplacementCount
    }
  })
})

const filteredGroups = computed(() => {
  let groups = groupDisplayData.value
  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    groups = groups.filter(g =>
      g.name.toLowerCase().includes(kw) ||
      (g.description || '').toLowerCase().includes(kw)
    )
  }
  if (filters.wornStatus) {
    groups = groups.filter(g => g.mostWornStatus === filters.wornStatus)
  }
  return groups
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
      { id: 1, name: '弹奏配件', sortOrder: 1, description: '直接参与演奏发声的配件，如琴弦、琴弓、拨片等' },
      { id: 2, name: '辅助工具', sortOrder: 2, description: '演奏过程中使用的辅助工具，如变调夹、背带等' },
      { id: 3, name: '养护耗材', sortOrder: 3, description: '乐器清洁、保养使用的消耗品，如松香、清洁剂等' }
    ]
  }
}

const loadAccessories = async () => {
  try {
    const res = await accessoryApi.list()
    allAccessories.value = res.data || res || []
  } catch {
    allAccessories.value = [
      { id: 1, name: '木吉他琴弦', specification: '012-053 磷铜覆膜', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 1, wornStatus: 'slight', purchaseDate: '2026-04-01', imageUrl: '', standardCycle: 90 },
      { id: 2, name: '小提琴松香', specification: '无尘轻型 4/4', typeCode: 'rosin', typeName: '松香', instrument: 'violin', instrumentName: '小提琴', groupId: 3, wornStatus: 'good', purchaseDate: '2026-05-01', imageUrl: '', standardCycle: 180 },
      { id: 3, name: '电吉他拨片', specification: '0.88mm 尼龙防滑', typeCode: 'pick', typeName: '拨片', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, wornStatus: 'good', purchaseDate: '2026-05-10', imageUrl: '', standardCycle: 60 },
      { id: 4, name: '小提琴琴弓', specification: '4/4 巴西木 八角弓', typeCode: 'bow', typeName: '琴弓', instrument: 'violin', instrumentName: '小提琴', groupId: 1, wornStatus: 'slight', purchaseDate: '2026-01-15', imageUrl: '', standardCycle: 365 },
      { id: 5, name: '吉他变调夹', specification: '弹簧式 金属款', typeCode: 'capo', typeName: '变调夹', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 2, wornStatus: 'good', purchaseDate: '2025-11-20', imageUrl: '', standardCycle: 730 },
      { id: 6, name: '指板清洁剂', specification: '柠檬油 100ml', typeCode: 'cleaner', typeName: '清洁用品', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 3, wornStatus: 'severe', purchaseDate: '2025-08-01', imageUrl: '', standardCycle: 180 },
      { id: 7, name: '贝斯琴弦', specification: '045-105 镍钢', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-bass', instrumentName: '贝斯', groupId: 1, wornStatus: 'broken', purchaseDate: '2025-06-01', imageUrl: '', standardCycle: 90 },
      { id: 8, name: '尤克里里琴弦', specification: '碳素 高音C', typeCode: 'string', typeName: '琴弦', instrument: 'ukulele', instrumentName: '尤克里里', groupId: 1, wornStatus: 'good', purchaseDate: '2026-03-15', imageUrl: '', standardCycle: 90 },
      { id: 9, name: '电吉他琴弦', specification: '009-042 镍钢', typeCode: 'string', typeName: '琴弦', instrument: 'guitar-electric', instrumentName: '电吉他', groupId: 1, wornStatus: 'good', purchaseDate: '2026-04-20', imageUrl: '', standardCycle: 90 },
      { id: 10, name: '吉他背带', specification: '皮革款 加厚', typeCode: 'strap', typeName: '背带', instrument: 'guitar-acoustic', instrumentName: '木吉他', groupId: 2, wornStatus: 'slight', purchaseDate: '2025-12-01', imageUrl: '', standardCycle: 365 },
      { id: 11, name: '二胡松香', specification: '专业微尘型', typeCode: 'rosin', typeName: '松香', instrument: 'erhu', instrumentName: '二胡', groupId: 3, wornStatus: 'slight', purchaseDate: '2026-02-10', imageUrl: '', standardCycle: 180 },
      { id: 12, name: '二胡琴弓', specification: '白马尾 紫竹杆', typeCode: 'bow', typeName: '琴弓', instrument: 'erhu', instrumentName: '二胡', groupId: 1, wornStatus: 'good', purchaseDate: '2026-03-01', imageUrl: '', standardCycle: 365 }
    ]
  }
}

const loadReplacements = async () => {
  try {
    const res = await replacementApi.list()
    allReplacements.value = res.data || res || []
  } catch {
    allReplacements.value = [
      { id: 1, accessoryId: 1, accessoryName: '木吉他琴弦', specification: '012-053 磷铜覆膜', instrumentName: '木吉他', replaceDate: '2026-04-01', standardCycle: 90, usageDays: 74, operator: '本人', remark: '正常更换' },
      { id: 2, accessoryId: 3, accessoryName: '电吉他拨片', specification: '0.88mm 尼龙防滑', instrumentName: '电吉他', replaceDate: '2026-05-10', standardCycle: 60, usageDays: 35, operator: '本人', remark: '磨损更换' },
      { id: 3, accessoryId: 6, accessoryName: '指板清洁剂', specification: '柠檬油 100ml', instrumentName: '木吉他', replaceDate: '2025-08-01', standardCycle: 180, usageDays: 317, operator: '本人', remark: '首次购入' },
      { id: 4, accessoryId: 9, accessoryName: '电吉他琴弦', specification: '009-042 镍钢', instrumentName: '电吉他', replaceDate: '2026-04-20', standardCycle: 90, usageDays: 55, operator: '本人', remark: '正常更换' },
      { id: 5, accessoryId: 2, accessoryName: '小提琴松香', specification: '无尘轻型 4/4', instrumentName: '小提琴', replaceDate: '2026-05-01', standardCycle: 180, usageDays: 44, operator: '本人', remark: '正常更换' }
    ]
  }
}

const handleSearch = () => {}

const clearFilters = () => {
  filters.keyword = ''
  filters.wornStatus = ''
}

const goToGroup = (groupId) => {
  router.push({ path: '/groups', query: { groupId } })
}

const loadAllData = async () => {
  loading.value = true
  try {
    await Promise.all([loadDict(), loadGroups(), loadAccessories(), loadReplacements()])
  } finally {
    loading.value = false
  }
}

onMounted(loadAllData)
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    margin: 0 0 4px 0;
  }

  .page-subtitle {
    font-size: 13px;
    color: #909399;
  }
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #fff;
    flex-shrink: 0;
  }

  .stat-info {
    .stat-value {
      font-size: 22px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
    }
    .stat-label {
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
    }
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
}

.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.group-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  }
}

.group-card-header {
  padding: 18px 20px;
  color: #fff;
  position: relative;

  .group-header-content {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .group-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .group-title {
    font-size: 17px;
    font-weight: 600;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }

  .group-count-badge {
    background: rgba(255, 255, 255, 0.25);
    backdrop-filter: blur(8px);
    border-radius: 20px;
    padding: 6px 14px;
    display: flex;
    flex-direction: column;
    align-items: center;

    .count-num {
      font-size: 18px;
      font-weight: 700;
      line-height: 1.1;
    }
    .count-label {
      font-size: 11px;
      opacity: 0.9;
    }
  }

  .group-description {
    margin-top: 10px;
    font-size: 12px;
    opacity: 0.9;
    line-height: 1.5;
  }
}

.group-card-body {
  flex: 1;
  padding: 16px 20px;
}

.info-section {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.instrument-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  :deep(.el-tag) {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .tag-count {
    font-size: 11px;
    opacity: 0.8;
  }
}

.empty-text {
  font-size: 12px;
  color: #c0c4cc;
}

.worn-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.worn-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;

  .worn-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
}

.worn-count {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.worn-distribution {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.worn-dist-item {
  display: flex;
  align-items: center;
  gap: 8px;

  .worn-dist-label {
    width: 60px;
    font-size: 11px;
    color: #909399;
    flex-shrink: 0;
  }

  .worn-dist-bar {
    flex: 1;
    height: 6px;
    background: #f0f2f5;
    border-radius: 3px;
    overflow: hidden;

    .worn-dist-fill {
      height: 100%;
      border-radius: 3px;
      transition: width 0.5s ease;
    }
  }

  .worn-dist-count {
    width: 20px;
    text-align: right;
    font-size: 11px;
    color: #606266;
    flex-shrink: 0;
  }
}

.replacement-summary {
  .replacement-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .replacement-name {
      font-size: 13px;
      color: #303133;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
    }

    .replacement-date {
      font-size: 12px;
      color: #67c23a;
      font-weight: 500;
      flex-shrink: 0;
      margin-left: 8px;
    }
  }

  .replacement-meta {
    display: flex;
    justify-content: flex-end;
  }
}

.no-replacement {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #c0c4cc;
  font-size: 12px;
}

.group-card-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: flex-end;
}
</style>
