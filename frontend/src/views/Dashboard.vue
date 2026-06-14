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

    <el-row :gutter="16">
      <el-col :lg="16" :md="24">
        <el-card class="card-shadow" shadow="never">
          <template #header>
            <div class="card-header">
              <span>即将到期更换</span>
              <el-tag size="small" type="warning">建议优先处理</el-tag>
            </div>
          </template>
          <el-table :data="upcomingList" stripe style="width: 100%" size="default">
            <el-table-column prop="name" label="配件名称" min-width="140" />
            <el-table-column prop="specification" label="规格" min-width="120" show-overflow-tooltip />
            <el-table-column prop="instrument" label="适配乐器" width="100" />
            <el-table-column prop="lastReplaceDate" label="上次更换" width="120" />
            <el-table-column label="使用时长" width="100">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.usageDays }}天</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.daysLeft <= 7 ? 'danger' : row.daysLeft <= 30 ? 'warning' : 'success'" size="small">
                  {{ row.daysLeft <= 0 ? '已超期' : `剩${row.daysLeft}天` }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="upcomingList.length === 0" description="暂无即将到期的配件" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :lg="8" :md="24">
        <el-card class="card-shadow" shadow="never" style="margin-bottom: 16px">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { dashboardApi } from '@/api'

const stats = reactive({
  totalAccessories: 0,
  wornCount: 0,
  monthReplacements: 0,
  groupCount: 0
})

const upcomingList = ref([])
const wornDist = ref([])
const groupDist = ref([])

const loadData = async () => {
  try {
    const [s, u, w, g] = await Promise.all([
      dashboardApi.stats(),
      dashboardApi.upcomingReplacements(),
      dashboardApi.wornDistribution(),
      dashboardApi.groupDistribution()
    ])
    Object.assign(stats, s.data || s || {})
    upcomingList.value = u.data || u || []
    wornDist.value = w.data || w || []
    groupDist.value = g.data || g || []
  } catch {
    loadMockData()
  }
}

const loadMockData = () => {
  Object.assign(stats, { totalAccessories: 28, wornCount: 5, monthReplacements: 3, groupCount: 3 })
  upcomingList.value = [
    { name: '吉他琴弦', specification: '012-053 磷铜', instrument: '木吉他', lastReplaceDate: '2026-04-15', usageDays: 60, daysLeft: -10 },
    { name: '小提琴松香', specification: '无尘轻型', instrument: '小提琴', lastReplaceDate: '2026-05-01', usageDays: 44, daysLeft: 16 },
    { name: '拨片', specification: '0.88mm 尼龙', instrument: '电吉他', lastReplaceDate: '2026-05-20', usageDays: 25, daysLeft: 35 }
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
</style>
