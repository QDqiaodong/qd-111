<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">总览</h2>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalAccessories }}</div>
            <div class="stat-label">配件总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.wornCount }}</div>
            <div class="stat-label">需关注</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
            <el-icon><RefreshRight /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.monthReplacements }}</div>
            <div class="stat-label">本月更换</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
            <el-icon><Folder /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.groupCount }}</div>
            <div class="stat-label">物资分组</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="risk-tiers-section">
      <div class="section-title">
        <el-icon color="#f56c6c"><Rank /></el-icon>
        <span>周期风险分层</span>
        <span class="section-subtitle">按处理优先级从高到低排列</span>
      </div>

      <el-row :gutter="16">
        <el-col :lg="12" :md="24">
          <el-card class="risk-card risk-card-expired card-shadow" shadow="never">
            <template #header>
              <div class="risk-card-header">
                <div class="risk-card-title">
                  <span class="priority-badge priority-p0">P0</span>
                  <el-icon color="#f56c6c"><Clock /></el-icon>
                  <span class="risk-title-text">已超期</span>
                </div>
                <el-tag type="danger" effect="dark" size="small">
                  {{ expiredList.length }} 项需立即处理
                </el-tag>
              </div>
            </template>
            <div v-if="expiredGrouped.length > 0" class="risk-content">
              <div v-for="group in expiredGrouped" :key="group.instrument" class="instrument-group">
                <div class="instrument-header">
                  <el-icon color="#f56c6c"><VideoCamera /></el-icon>
                  <span class="instrument-name">{{ group.instrument }}</span>
                  <el-tag size="small" type="danger" effect="plain">{{ group.count }}件</el-tag>
                </div>
                <div class="accessory-list">
                  <div v-for="item in group.items" :key="item.id" class="accessory-item">
                    <div class="accessory-main">
                      <span class="accessory-name">{{ item.name }}</span>
                      <el-tag size="small" type="info" effect="plain">{{ item.typeName }}</el-tag>
                    </div>
                    <div class="accessory-meta">
                      <span class="spec-text">{{ item.specification }}</span>
                      <el-tag size="small" type="danger" effect="dark">超期{{ Math.abs(item.daysLeft) }}天</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无已超期配件" :image-size="60" />
          </el-card>
        </el-col>

        <el-col :lg="12" :md="24">
          <el-card class="risk-card risk-card-broken card-shadow" shadow="never">
            <template #header>
              <div class="risk-card-header">
                <div class="risk-card-title">
                  <span class="priority-badge priority-p0">P0</span>
                  <el-icon color="#909399"><CircleClose /></el-icon>
                  <span class="risk-title-text">断裂配件</span>
                </div>
                <el-tag type="info" effect="dark" size="small">
                  {{ brokenList.length }} 项需立即处理
                </el-tag>
              </div>
            </template>
            <div v-if="brokenGrouped.length > 0" class="risk-content">
              <div v-for="group in brokenGrouped" :key="group.instrument" class="instrument-group">
                <div class="instrument-header">
                  <el-icon color="#909399"><VideoCamera /></el-icon>
                  <span class="instrument-name">{{ group.instrument }}</span>
                  <el-tag size="small" type="info" effect="plain">{{ group.count }}件</el-tag>
                </div>
                <div class="accessory-list">
                  <div v-for="item in group.items" :key="item.id" class="accessory-item">
                    <div class="accessory-main">
                      <span class="accessory-name">{{ item.name }}</span>
                      <el-tag size="small" type="info" effect="plain">{{ item.typeName }}</el-tag>
                    </div>
                    <div class="accessory-meta">
                      <span class="spec-text">{{ item.specification }}</span>
                      <el-tag size="small" type="info" effect="dark">已损坏</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无断裂配件" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :lg="12" :md="24">
          <el-card class="risk-card risk-card-severe card-shadow" shadow="never">
            <template #header>
              <div class="risk-card-header">
                <div class="risk-card-title">
                  <span class="priority-badge priority-p1">P1</span>
                  <el-icon color="#e6a23c"><Warning /></el-icon>
                  <span class="risk-title-text">严重损耗</span>
                </div>
                <el-tag type="warning" effect="dark" size="small">
                  {{ severeList.length }} 项建议尽快更换
                </el-tag>
              </div>
            </template>
            <div v-if="severeGrouped.length > 0" class="risk-content">
              <div v-for="group in severeGrouped" :key="group.instrument" class="instrument-group">
                <div class="instrument-header">
                  <el-icon color="#e6a23c"><VideoCamera /></el-icon>
                  <span class="instrument-name">{{ group.instrument }}</span>
                  <el-tag size="small" type="warning" effect="plain">{{ group.count }}件</el-tag>
                </div>
                <div class="accessory-list">
                  <div v-for="item in group.items" :key="item.id" class="accessory-item">
                    <div class="accessory-main">
                      <span class="accessory-name">{{ item.name }}</span>
                      <el-tag size="small" type="info" effect="plain">{{ item.typeName }}</el-tag>
                    </div>
                    <div class="accessory-meta">
                      <span class="spec-text">{{ item.specification }}</span>
                      <el-tag size="small" type="warning" effect="dark">使用{{ item.usageDays }}天</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无严重损耗配件" :image-size="60" />
          </el-card>
        </el-col>

        <el-col :lg="12" :md="24">
          <el-card class="risk-card risk-card-upcoming card-shadow" shadow="never">
            <template #header>
              <div class="risk-card-header">
                <div class="risk-card-title">
                  <span class="priority-badge priority-p2">P2</span>
                  <el-icon color="#409eff"><Timer /></el-icon>
                  <span class="risk-title-text">即将到期</span>
                </div>
                <el-tag type="primary" effect="dark" size="small">
                  {{ upcomingRiskList.length }} 项30天内到期
                </el-tag>
              </div>
            </template>
            <div v-if="upcomingGrouped.length > 0" class="risk-content">
              <div v-for="group in upcomingGrouped" :key="group.instrument" class="instrument-group">
                <div class="instrument-header">
                  <el-icon color="#409eff"><VideoCamera /></el-icon>
                  <span class="instrument-name">{{ group.instrument }}</span>
                  <el-tag size="small" type="primary" effect="plain">{{ group.count }}件</el-tag>
                </div>
                <div class="accessory-list">
                  <div v-for="item in group.items" :key="item.id" class="accessory-item">
                    <div class="accessory-main">
                      <span class="accessory-name">{{ item.name }}</span>
                      <el-tag size="small" type="info" effect="plain">{{ item.typeName }}</el-tag>
                    </div>
                    <div class="accessory-meta">
                      <span class="spec-text">{{ item.specification }}</span>
                      <el-tag :type="item.daysLeft <= 7 ? 'danger' : 'primary'" size="small" effect="dark">剩{{ item.daysLeft }}天</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无即将到期配件" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :lg="12" :md="24">
        <el-card class="card-shadow" shadow="never">
          <template #header>
            <span>损耗状态分布</span>
          </template>
          <div class="distribution-list">
            <div v-for="item in wornDist" :key="item.status" class="dist-item">
              <div class="dist-label">
                <span class="dist-dot" :style="{ background: item.color }"></span>
                <span>{{ item.label }}</span>
              </div>
              <div class="dist-bar-wrap">
                <div class="dist-bar" :style="{ width: item.percent + '%', background: item.color }"></div>
              </div>
              <div class="dist-count">{{ item.count }} ({{ item.percent }}%)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :lg="12" :md="24">
        <el-card class="card-shadow" shadow="never">
          <template #header>
            <span>物资分组统计</span>
          </template>
          <div class="distribution-list">
            <div v-for="item in groupDist" :key="item.id" class="dist-item">
              <div class="dist-label">
                <el-icon color="#409eff"><FolderOpened /></el-icon>
                <span style="margin-left: 6px">{{ item.name }}</span>
              </div>
              <div class="dist-bar-wrap">
                <div class="dist-bar" style="background: #409eff" :style="{ width: item.percent + '%' }"></div>
              </div>
              <div class="dist-count">{{ item.count }} ({{ item.percent }}%)</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 16px">
      <el-col :span="24">
        <el-card class="card-shadow" shadow="never">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#409eff"><Calendar /></el-icon>
              <span>年度配件更换统计</span>
              <span class="section-subtitle">按年度汇总各类配件更换次数、平均使用天数及高损耗乐器</span>
            </div>
          </template>
          <div v-loading="annualLoading" class="annual-body">
            <div v-if="annualStats.length > 0" class="annual-cards">
              <div v-for="year in annualStats" :key="year.year" class="annual-card">
                <div class="annual-card-head">
                  <span class="annual-year">{{ year.year }} 年</span>
                  <el-tag type="primary" effect="dark" size="small">{{ year.totalReplacements }} 次更换</el-tag>
                </div>
                <div class="annual-avg">
                  <span class="annual-avg-value">{{ year.avgUsageDays }}</span>
                  <span class="annual-avg-unit">天</span>
                  <span class="annual-avg-label">平均使用周期</span>
                </div>
                <div class="annual-sub-title">各类配件更换</div>
                <div class="annual-type-list">
                  <div v-for="t in year.typeStats" :key="t.typeCode" class="annual-type-item">
                    <span class="annual-type-name">{{ t.typeName }}</span>
                    <div class="annual-type-bar-wrap">
                      <div class="annual-type-bar" :style="{ width: typePercent(year, t) + '%' }"></div>
                    </div>
                    <span class="annual-type-count">{{ t.replacementCount }}次</span>
                    <span class="annual-type-avg">均{{ t.avgUsageDays }}天</span>
                  </div>
                  <el-empty v-if="!year.typeStats.length" description="暂无类型数据" :image-size="40" />
                </div>
                <div class="annual-sub-title">高损耗乐器</div>
                <div class="annual-instrument-list">
                  <el-tag
                    v-for="inst in year.topInstruments"
                    :key="inst.instrument"
                    type="warning"
                    effect="plain"
                    size="small"
                    class="annual-instrument-tag"
                  >
                    {{ inst.instrumentName }} · {{ inst.replacementCount }}次
                  </el-tag>
                  <span v-if="!year.topInstruments.length" class="annual-empty-text">暂无数据</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无年度更换数据" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 16px">
      <el-col :span="24">
        <el-card class="card-shadow" shadow="never">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#f56c6c"><DataAnalysis /></el-icon>
              <span>损耗热区分布</span>
              <span class="section-subtitle">按乐器×配件类型交叉呈现损耗状态</span>
            </div>
          </template>
          <WornHeatmap :data="wornHeatmapData" :loading="heatmapLoading" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { dashboardApi } from '@/api'
import WornHeatmap from '@/components/WornHeatmap.vue'

const stats = reactive({
  totalAccessories: 0,
  wornCount: 0,
  monthReplacements: 0,
  groupCount: 0
})

const upcomingList = ref([])
const wornDist = ref([])
const groupDist = ref([])
const wornHeatmapData = ref({})
const heatmapLoading = ref(false)
const annualStats = ref([])
const annualLoading = ref(false)

const expiredList = ref([])
const brokenList = ref([])
const severeList = ref([])
const upcomingRiskList = ref([])

const groupByInstrument = (list) => {
  const map = new Map()
  list.forEach(item => {
    const key = item.instrumentName || item.instrument || '未知乐器'
    if (!map.has(key)) {
      map.set(key, { instrument: key, count: 0, items: [] })
    }
    const group = map.get(key)
    group.count++
    group.items.push(item)
  })
  return Array.from(map.values()).sort((a, b) => b.count - a.count)
}

const expiredGrouped = computed(() => groupByInstrument(expiredList.value))
const brokenGrouped = computed(() => groupByInstrument(brokenList.value))
const severeGrouped = computed(() => groupByInstrument(severeList.value))
const upcomingGrouped = computed(() => groupByInstrument(upcomingRiskList.value))

const typePercent = (year, type) => {
  const max = Math.max(...(year.typeStats || []).map(t => t.replacementCount || 0), 1)
  return Math.round(((type.replacementCount || 0) * 100) / max)
}

const loadData = async () => {
  try {
    heatmapLoading.value = true
    annualLoading.value = true
    const [s, u, w, g, r, h, a] = await Promise.all([
      dashboardApi.stats(),
      dashboardApi.upcomingReplacements(),
      dashboardApi.wornDistribution(),
      dashboardApi.groupDistribution(),
      dashboardApi.riskTiers().catch(() => null),
      dashboardApi.wornHeatmap().catch(() => null),
      dashboardApi.annualStats().catch(() => null)
    ])
    Object.assign(stats, s.data || s || {})
    upcomingList.value = u.data || u || []
    wornDist.value = w.data || w || []
    groupDist.value = g.data || g || []
    annualStats.value = a?.data || a || []

    if (h && h.data) {
      wornHeatmapData.value = h.data
    } else {
      wornHeatmapData.value = {}
    }

    if (r && (r.data || r)) {
      const riskData = r.data || r
      expiredList.value = riskData.expired || []
      brokenList.value = riskData.broken || []
      severeList.value = riskData.severe || []
      upcomingRiskList.value = riskData.upcoming || []
    } else {
      categorizeFromUpcoming()
    }
  } catch {
    Object.assign(stats, { totalAccessories: 0, wornCount: 0, monthReplacements: 0, groupCount: 0 })
    upcomingList.value = []
    wornDist.value = []
    groupDist.value = []
    wornHeatmapData.value = {}
    annualStats.value = []
    expiredList.value = []
    brokenList.value = []
    severeList.value = []
    upcomingRiskList.value = []
  } finally {
    heatmapLoading.value = false
    annualLoading.value = false
  }
}

const categorizeFromUpcoming = () => {
  const list = upcomingList.value
  expiredList.value = list.filter(i => i.daysLeft <= 0)
  brokenList.value = list.filter(i => i.wornStatus === 'broken')
  severeList.value = list.filter(i => i.wornStatus === 'severe' && i.daysLeft > 0)
  upcomingRiskList.value = list.filter(i => i.daysLeft > 0 && i.daysLeft <= 30 && i.wornStatus !== 'severe' && i.wornStatus !== 'broken')
}

const loadMockData = () => {
  Object.assign(stats, { totalAccessories: 28, wornCount: 5, monthReplacements: 3, groupCount: 3 })
  upcomingList.value = [
    { id: 1, name: '吉他琴弦', typeName: '琴弦', specification: '012-053 磷铜', instrumentName: '木吉他', lastReplaceDate: '2026-04-15', usageDays: 60, daysLeft: -10, wornStatus: 'severe' },
    { id: 2, name: '小提琴松香', typeName: '松香', specification: '无尘轻型', instrumentName: '小提琴', lastReplaceDate: '2026-05-01', usageDays: 44, daysLeft: 16, wornStatus: 'good' },
    { id: 3, name: '拨片', typeName: '拨片', specification: '0.88mm 尼龙', instrumentName: '电吉他', lastReplaceDate: '2026-05-20', usageDays: 25, daysLeft: 35, wornStatus: 'good' }
  ]
  wornDist.value = [
    { status: 'good', label: '完好', count: 18, percent: 64, color: '#67c23a' },
    { status: 'slight', label: '轻微磨损', count: 7, percent: 25, color: '#e6a23c' },
    { status: 'severe', label: '严重损耗', count: 3, percent: 11, color: '#f56c6c' }
  ]
  groupDist.value = [
    { id: 1, name: '弹奏配件', count: 12, percent: 43 },
    { id: 2, name: '辅助工具', count: 8, percent: 29 },
    { id: 3, name: '养护耗材', count: 8, percent: 28 }
  ]

  expiredList.value = [
    { id: 101, name: '木吉他琴弦', typeName: '琴弦', specification: '012-053 磷铜覆膜', instrumentName: '木吉他', usageDays: 105, daysLeft: -15, wornStatus: 'severe' },
    { id: 102, name: '电吉他琴弦', typeName: '琴弦', specification: '009-042 镍钢', instrumentName: '电吉他', usageDays: 120, daysLeft: -30, wornStatus: 'severe' },
    { id: 103, name: '小提琴琴弓', typeName: '琴弓', specification: '4/4 巴西木 八角弓', instrumentName: '小提琴', usageDays: 400, daysLeft: -35, wornStatus: 'slight' }
  ]

  brokenList.value = [
    { id: 201, name: '吉他背带', typeName: '背带', specification: '皮革款 加厚', instrumentName: '木吉他', usageDays: 200, daysLeft: 100, wornStatus: 'broken' },
    { id: 202, name: '变调夹', typeName: '变调夹', specification: '弹簧式 金属款', instrumentName: '电吉他', usageDays: 500, daysLeft: 230, wornStatus: 'broken' }
  ]

  severeList.value = [
    { id: 301, name: '小提琴松香', typeName: '松香', specification: '无尘轻型 4/4', instrumentName: '小提琴', usageDays: 150, daysLeft: 30, wornStatus: 'severe' },
    { id: 302, name: '指板清洁剂', typeName: '清洁用品', specification: '柠檬油 100ml', instrumentName: '木吉他', usageDays: 200, daysLeft: -20, wornStatus: 'severe' },
    { id: 303, name: '贝斯琴弦', typeName: '琴弦', specification: '045-105 镍钢', instrumentName: '贝斯', usageDays: 180, daysLeft: 10, wornStatus: 'severe' }
  ]

  upcomingRiskList.value = [
    { id: 401, name: '拨片', typeName: '拨片', specification: '0.88mm 尼龙防滑', instrumentName: '电吉他', usageDays: 45, daysLeft: 15, wornStatus: 'slight' },
    { id: 402, name: '尤克里里琴弦', typeName: '琴弦', specification: '碳素弦 低张力', instrumentName: '尤克里里', usageDays: 80, daysLeft: 10, wornStatus: 'good' },
    { id: 403, name: '二胡松香', typeName: '松香', specification: '专业微尘型', instrumentName: '二胡', usageDays: 160, daysLeft: 20, wornStatus: 'slight' }
  ]

  wornHeatmapData.value = {
    instruments: [
      { code: 'guitar-acoustic', label: '木吉他' },
      { code: 'guitar-electric', label: '电吉他' },
      { code: 'guitar-bass', label: '贝斯' },
      { code: 'violin', label: '小提琴' },
      { code: 'ukulele', label: '尤克里里' },
      { code: 'erhu', label: '二胡' }
    ],
    accessoryTypes: [
      { code: 'string', label: '琴弦' },
      { code: 'bow', label: '琴弓' },
      { code: 'pick', label: '拨片' },
      { code: 'rosin', label: '松香' },
      { code: 'capo', label: '变调夹' },
      { code: 'strap', label: '背带' },
      { code: 'cleaner', label: '清洁用品' }
    ],
    legends: [
      { code: 'good', label: '完好', color: '#67c23a' },
      { code: 'slight', label: '轻微磨损', color: '#e6a23c' },
      { code: 'severe', label: '严重损耗', color: '#f56c6c' },
      { code: 'broken', label: '已损坏', color: '#909399' }
    ],
    cells: [
      { instrumentCode: 'guitar-acoustic', instrumentName: '木吉他', typeCode: 'string', typeName: '琴弦', total: 5, goodCount: 2, slightCount: 2, severeCount: 1, brokenCount: 0 },
      { instrumentCode: 'guitar-acoustic', instrumentName: '木吉他', typeCode: 'pick', typeName: '拨片', total: 3, goodCount: 2, slightCount: 1, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'guitar-acoustic', instrumentName: '木吉他', typeCode: 'capo', typeName: '变调夹', total: 2, goodCount: 1, slightCount: 0, severeCount: 0, brokenCount: 1 },
      { instrumentCode: 'guitar-acoustic', instrumentName: '木吉他', typeCode: 'strap', typeName: '背带', total: 2, goodCount: 1, slightCount: 0, severeCount: 0, brokenCount: 1 },
      { instrumentCode: 'guitar-acoustic', instrumentName: '木吉他', typeCode: 'cleaner', typeName: '清洁用品', total: 2, goodCount: 0, slightCount: 1, severeCount: 1, brokenCount: 0 },

      { instrumentCode: 'guitar-electric', instrumentName: '电吉他', typeCode: 'string', typeName: '琴弦', total: 4, goodCount: 2, slightCount: 1, severeCount: 1, brokenCount: 0 },
      { instrumentCode: 'guitar-electric', instrumentName: '电吉他', typeCode: 'pick', typeName: '拨片', total: 6, goodCount: 4, slightCount: 2, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'guitar-electric', instrumentName: '电吉他', typeCode: 'capo', typeName: '变调夹', total: 1, goodCount: 0, slightCount: 0, severeCount: 0, brokenCount: 1 },
      { instrumentCode: 'guitar-electric', instrumentName: '电吉他', typeCode: 'strap', typeName: '背带', total: 2, goodCount: 2, slightCount: 0, severeCount: 0, brokenCount: 0 },

      { instrumentCode: 'guitar-bass', instrumentName: '贝斯', typeCode: 'string', typeName: '琴弦', total: 3, goodCount: 1, slightCount: 1, severeCount: 0, brokenCount: 1 },
      { instrumentCode: 'guitar-bass', instrumentName: '贝斯', typeCode: 'pick', typeName: '拨片', total: 2, goodCount: 1, slightCount: 1, severeCount: 0, brokenCount: 0 },

      { instrumentCode: 'violin', instrumentName: '小提琴', typeCode: 'string', typeName: '琴弦', total: 2, goodCount: 1, slightCount: 1, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'violin', instrumentName: '小提琴', typeCode: 'bow', typeName: '琴弓', total: 2, goodCount: 1, slightCount: 1, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'violin', instrumentName: '小提琴', typeCode: 'rosin', typeName: '松香', total: 3, goodCount: 1, slightCount: 1, severeCount: 1, brokenCount: 0 },
      { instrumentCode: 'violin', instrumentName: '小提琴', typeCode: 'cleaner', typeName: '清洁用品', total: 1, goodCount: 1, slightCount: 0, severeCount: 0, brokenCount: 0 },

      { instrumentCode: 'ukulele', instrumentName: '尤克里里', typeCode: 'string', typeName: '琴弦', total: 2, goodCount: 2, slightCount: 0, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'ukulele', instrumentName: '尤克里里', typeCode: 'pick', typeName: '拨片', total: 1, goodCount: 1, slightCount: 0, severeCount: 0, brokenCount: 0 },

      { instrumentCode: 'erhu', instrumentName: '二胡', typeCode: 'string', typeName: '琴弦', total: 1, goodCount: 1, slightCount: 0, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'erhu', instrumentName: '二胡', typeCode: 'bow', typeName: '琴弓', total: 1, goodCount: 0, slightCount: 1, severeCount: 0, brokenCount: 0 },
      { instrumentCode: 'erhu', instrumentName: '二胡', typeCode: 'rosin', typeName: '松香', total: 2, goodCount: 1, slightCount: 1, severeCount: 0, brokenCount: 0 }
    ]
  }

  annualStats.value = [
    {
      year: 2026,
      totalReplacements: 5,
      avgUsageDays: 61,
      typeStats: [
        { typeCode: 'string', typeName: '琴弦', replacementCount: 2, avgUsageDays: 73 },
        { typeCode: 'rosin', typeName: '松香', replacementCount: 1, avgUsageDays: 35 },
        { typeCode: 'pick', typeName: '拨片', replacementCount: 1, avgUsageDays: 20 },
        { typeCode: 'cleaner', typeName: '清洁用品', replacementCount: 1, avgUsageDays: 105 }
      ],
      topInstruments: [
        { instrument: 'guitar-acoustic', instrumentName: '木吉他', replacementCount: 3 },
        { instrument: 'violin', instrumentName: '小提琴', replacementCount: 1 },
        { instrument: 'guitar-electric', instrumentName: '电吉他', replacementCount: 1 }
      ]
    },
    {
      year: 2025,
      totalReplacements: 2,
      avgUsageDays: 90,
      typeStats: [
        { typeCode: 'string', typeName: '琴弦', replacementCount: 1, avgUsageDays: 90 },
        { typeCode: 'cleaner', typeName: '清洁用品', replacementCount: 1, avgUsageDays: 90 }
      ],
      topInstruments: [
        { instrument: 'guitar-acoustic', instrumentName: '木吉他', replacementCount: 2 }
      ]
    }
  ]
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  transition: all 0.3s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    flex-shrink: 0;
  }

  .stat-info {
    .stat-value {
      font-size: 26px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
    }
    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;

  .section-subtitle {
    font-size: 12px;
    font-weight: 400;
    color: #909399;
    margin-left: 8px;
  }
}

.risk-tiers-section {
  margin-bottom: 16px;
}

.risk-card {
  height: 100%;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
  }

  &.risk-card-expired {
    border-top: 4px solid #f56c6c;
  }

  &.risk-card-broken {
    border-top: 4px solid #909399;
  }

  &.risk-card-severe {
    border-top: 4px solid #e6a23c;
  }

  &.risk-card-upcoming {
    border-top: 4px solid #409eff;
  }
}

.risk-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.risk-card-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .risk-title-text {
    font-weight: 600;
    font-size: 15px;
    color: #303133;
  }
}

.priority-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 20px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;

  &.priority-p0 {
    background: linear-gradient(135deg, #f56c6c, #c0392b);
  }

  &.priority-p1 {
    background: linear-gradient(135deg, #e6a23c, #d35400);
  }

  &.priority-p2 {
    background: linear-gradient(135deg, #409eff, #2980b9);
  }
}

.risk-content {
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
  }
}

.instrument-group {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.instrument-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fafbfc;
  border-radius: 6px;
  margin-bottom: 8px;

  .instrument-name {
    flex: 1;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}

.accessory-list {
  padding-left: 4px;
}

.accessory-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 6px;
  background: #fff;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;

  &:hover {
    background: #f5f7fa;
    border-color: #dcdfe6;
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.accessory-main {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .accessory-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

.accessory-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;

  .spec-text {
    flex: 1;
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.distribution-list {
  .dist-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    border-bottom: 1px dashed #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .dist-label {
      width: 90px;
      display: flex;
      align-items: center;
      font-size: 13px;
      color: #606266;
      flex-shrink: 0;
    }

    .dist-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 6px;
    }

    .dist-bar-wrap {
      flex: 1;
      height: 8px;
      background: #f0f2f5;
      border-radius: 4px;
      overflow: hidden;
    }

    .dist-bar {
      height: 100%;
      border-radius: 4px;
      transition: width 0.5s;
    }

    .dist-count {
      width: 70px;
      text-align: right;
      font-size: 12px;
      color: #909399;
      flex-shrink: 0;
    }
  }
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;

  .section-subtitle {
    font-size: 12px;
    font-weight: 400;
    color: #909399;
    margin-left: 8px;
  }
}

.annual-body {
  min-height: 120px;
}

.annual-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.annual-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 18px;
  transition: all 0.25s;

  &:hover {
    border-color: #c6e2ff;
    box-shadow: 0 4px 14px rgba(64, 158, 255, 0.1);
  }
}

.annual-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  .annual-year {
    font-size: 18px;
    font-weight: 700;
    color: #303133;
  }
}

.annual-avg {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #ebeef5;

  .annual-avg-value {
    font-size: 28px;
    font-weight: 700;
    color: #409eff;
    line-height: 1;
  }

  .annual-avg-unit {
    font-size: 13px;
    color: #909399;
  }

  .annual-avg-label {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }
}

.annual-sub-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.annual-type-list {
  margin-bottom: 14px;
}

.annual-type-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;

  .annual-type-name {
    width: 70px;
    font-size: 13px;
    color: #606266;
    flex-shrink: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .annual-type-bar-wrap {
    flex: 1;
    height: 7px;
    background: #f0f2f5;
    border-radius: 4px;
    overflow: hidden;
  }

  .annual-type-bar {
    height: 100%;
    background: linear-gradient(90deg, #409eff, #79bbff);
    border-radius: 4px;
    transition: width 0.5s;
  }

  .annual-type-count {
    width: 42px;
    text-align: right;
    font-size: 12px;
    color: #303133;
    font-weight: 600;
    flex-shrink: 0;
  }

  .annual-type-avg {
    width: 64px;
    text-align: right;
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
  }
}

.annual-instrument-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .annual-instrument-tag {
    border-radius: 12px;
  }

  .annual-empty-text {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
