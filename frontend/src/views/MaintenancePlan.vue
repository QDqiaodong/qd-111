<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">乐器维度保养计划</h2>
      <div class="page-header-actions">
        <el-button type="primary" :icon="Refresh" @click="loadData" :loading="loading">
          刷新计划
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
            <el-icon><VideoCamera /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ planList.length }}</div>
            <div class="stat-label">乐器总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalUrgent }}</div>
            <div class="stat-label">需立即处理</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f6d365, #fda085)">
            <el-icon><View /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalAttention }}</div>
            <div class="stat-label">需关注</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalNormal }}</div>
            <div class="stat-label">状态正常</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div v-loading="loading" class="plan-list">
      <el-empty v-if="!loading && planList.length === 0" description="暂无保养计划数据" :image-size="80" />

      <el-collapse v-model="activeInstruments" accordion v-else>
        <el-collapse-item
          v-for="plan in planList"
          :key="plan.instrument"
          :name="plan.instrument"
          class="plan-card"
        >
          <template #title>
            <div class="plan-title">
              <div class="plan-title-left">
                <el-icon :size="20" :color="plan.overallStatusColor">
                  <VideoCamera />
                </el-icon>
                <span class="instrument-name">{{ plan.instrumentName }}</span>
                <el-tag
                  :color="plan.overallStatusColor + '20'"
                  :style="{ color: plan.overallStatusColor, borderColor: plan.overallStatusColor + '60', borderWidth: '1px' }"
                  effect="light"
                  size="small"
                  class="status-tag"
                >
                  {{ plan.overallStatusLabel }}
                </el-tag>
              </div>
              <div class="plan-title-right">
                <span v-if="plan.urgentCount > 0" class="count-badge count-urgent">
                  {{ plan.urgentCount }} 项需立即处理
                </span>
                <span v-if="plan.attentionCount > 0" class="count-badge count-attention">
                  {{ plan.attentionCount }} 项需关注
                </span>
                <span v-if="plan.normalCount > 0 && plan.urgentCount === 0 && plan.attentionCount === 0" class="count-badge count-normal">
                  {{ plan.normalCount }} 项正常
                </span>
                <span class="total-count">共 {{ plan.totalCount }} 项配件</span>
              </div>
            </div>
          </template>

          <div class="plan-detail">
            <div class="detail-summary">
              <div class="summary-item">
                <span class="summary-label">最近处理窗口</span>
                <el-tag
                  v-if="plan.minDaysLeft !== null && plan.minDaysLeft < 0"
                  type="danger"
                  size="small"
                >
                  已超期 {{ Math.abs(plan.minDaysLeft) }} 天
                </el-tag>
                <el-tag
                  v-else-if="plan.minDaysLeft !== null && plan.minDaysLeft <= 30"
                  type="warning"
                  size="small"
                >
                  剩余 {{ plan.minDaysLeft }} 天
                </el-tag>
                <el-tag v-else-if="plan.minDaysLeft !== null" type="success" size="small">
                  剩余 {{ plan.minDaysLeft }} 天
                </el-tag>
                <el-tag v-else type="info" size="small">未设置周期</el-tag>
              </div>
              <div class="summary-item">
                <span class="summary-label">最高风险分</span>
                <span class="summary-value" :style="{ color: getRiskColorByScore(plan.maxRiskScore) }">
                  {{ plan.maxRiskScore }}
                </span>
              </div>
            </div>

            <el-table :data="plan.items" stripe style="width: 100%" size="default">
              <el-table-column label="配件" min-width="180">
                <template #default="{ row }">
                  <div class="accessory-cell">
                    <el-image
                      v-if="row.imageUrl"
                      :src="resolveImage(row.imageUrl, row.typeCode)"
                      fit="cover"
                      class="accessory-thumb"
                    />
                    <div class="accessory-info">
                      <div class="accessory-name">{{ row.accessoryName }}</div>
                      <div class="accessory-spec">{{ row.specification || '-' }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="typeName" label="类型" width="100" />
              <el-table-column label="损耗状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="getWornTagType(row.wornStatus)" effect="light" size="small">
                    {{ row.wornStatusLabel }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="周期进度" width="180">
                <template #default="{ row }">
                  <div class="cycle-status-cell">
                    <div class="cycle-bar-wrap">
                      <div class="cycle-bar-bg">
                        <div
                          class="cycle-bar-fill"
                          :style="{
                            width: (row.cyclePercent || 0) + '%',
                            background: getCycleBarColor(row.cyclePercent)
                          }"
                        />
                      </div>
                      <span class="cycle-bar-pct" :style="{ color: getCycleBarColor(row.cyclePercent) }">
                        {{ row.cyclePercent || 0 }}%
                      </span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="下次处理窗口" width="180" align="center">
                <template #default="{ row }">
                  <div class="date-info">
                    <div class="expected-date">
                      {{ row.expectedNextDate || '未设置' }}
                    </div>
                    <el-tag
                      v-if="row.daysLeft !== null && row.daysLeft < 0"
                      type="danger"
                      size="small"
                      effect="dark"
                    >
                      超期{{ Math.abs(row.daysLeft) }}天
                    </el-tag>
                    <el-tag
                      v-else-if="row.daysLeft !== null && row.daysLeft <= 7"
                      type="danger"
                      size="small"
                    >
                      剩{{ row.daysLeft }}天
                    </el-tag>
                    <el-tag
                      v-else-if="row.daysLeft !== null && row.daysLeft <= 30"
                      type="warning"
                      size="small"
                    >
                      剩{{ row.daysLeft }}天
                    </el-tag>
                    <el-tag v-else-if="row.daysLeft !== null" type="success" size="small">
                      剩{{ row.daysLeft }}天
                    </el-tag>
                    <el-tag v-else type="info" size="small">-</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="风险等级" width="110" align="center">
                <template #default="{ row }">
                  <el-tooltip :content="`风险得分: ${row.riskScore || 0}`" placement="top">
                    <el-tag
                      :color="row.riskColor + '20'"
                      :style="{ color: row.riskColor, borderColor: row.riskColor + '60', borderWidth: '1px' }"
                      effect="light"
                      size="small"
                    >
                      <span style="font-weight: 600">{{ row.riskLabel }}</span>
                    </el-tag>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="处理建议" min-width="200">
                <template #default="{ row }">
                  <div class="suggestion-cell">
                    <el-icon
                      :size="16"
                      :color="getActionIconColor(row.actionType)"
                      style="flex-shrink: 0"
                    >
                      <component :is="getActionIcon(row.actionType)" />
                    </el-icon>
                    <span class="suggestion-text">{{ row.actionSuggestion }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="计划状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    :color="row.planStatusColor + '20'"
                    :style="{ color: row.planStatusColor, borderColor: row.planStatusColor + '60', borderWidth: '1px' }"
                    effect="light"
                    size="small"
                  >
                    {{ row.planStatusLabel }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  VideoCamera,
  WarningFilled,
  View,
  CircleCheck,
  RefreshRight,
  Search,
  MagicStick,
  Select
} from '@element-plus/icons-vue'
import { maintenancePlanApi } from '@/api'
import { getImageMeta } from '@/utils/image'

const loading = ref(false)
const planList = ref([])
const activeInstruments = ref('')

const totalUrgent = computed(() => planList.value.reduce((sum, p) => sum + (p.urgentCount || 0), 0))
const totalAttention = computed(() => planList.value.reduce((sum, p) => sum + (p.attentionCount || 0), 0))
const totalNormal = computed(() => planList.value.reduce((sum, p) => sum + (p.normalCount || 0), 0))

const resolveImage = (url, typeCode) => {
  return getImageMeta({ imageUrl: url, typeCode }).imageUrl
}

const getWornTagType = (code) => {
  const map = { good: 'success', slight: 'warning', severe: 'danger', broken: 'info' }
  return map[code] || 'info'
}

const getCycleBarColor = (percent) => {
  if (!percent || percent <= 0) return '#67c23a'
  if (percent >= 100) return '#f56c6c'
  if (percent >= 80) return '#e6a23c'
  if (percent >= 50) return '#409eff'
  return '#67c23a'
}

const getRiskColorByScore = (score) => {
  if (!score) return '#67c23a'
  if (score >= 90) return '#9c27b0'
  if (score >= 75) return '#f56c6c'
  if (score >= 55) return '#e6a23c'
  if (score >= 30) return '#409eff'
  return '#67c23a'
}

const getActionIcon = (actionType) => {
  const map = {
    replace: RefreshRight,
    check: Search,
    clean: MagicStick,
    none: Select
  }
  return map[actionType] || Select
}

const getActionIconColor = (actionType) => {
  const map = {
    replace: '#f56c6c',
    check: '#e6a23c',
    clean: '#409eff',
    none: '#67c23a'
  }
  return map[actionType] || '#909399'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await maintenancePlanApi.allPlans()
    if (res && res.data) {
      planList.value = res.data
      if (res.data.length > 0) {
        activeInstruments.value = res.data[0].instrument
      }
    } else {
      planList.value = []
    }
  } catch (e) {
    ElMessage.error('加载保养计划失败')
    planList.value = []
  } finally {
    loading.value = false
  }
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

.page-header-actions {
  display: flex;
  gap: 12px;
}

.plan-list {
  min-height: 200px;
}

.plan-card {
  margin-bottom: 12px;
  border: none;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;

  :deep(.el-collapse-item__header) {
    padding: 16px 20px;
    border-bottom: none;
    height: auto;
    line-height: 1.5;

    &:hover {
      background: #fafbfc;
    }
  }

  :deep(.el-collapse-item__wrap) {
    border-top: 1px solid #ebeef5;
  }

  :deep(.el-collapse-item__content) {
    padding: 0;
  }
}

.plan-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}

.plan-title-left {
  display: flex;
  align-items: center;
  gap: 10px;

  .instrument-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .status-tag {
    margin-left: 4px;
  }
}

.plan-title-right {
  display: flex;
  align-items: center;
  gap: 10px;

  .count-badge {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 4px;

    &.count-urgent {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.count-attention {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.count-normal {
      background: #f0f9eb;
      color: #67c23a;
    }
  }

  .total-count {
    font-size: 12px;
    color: #909399;
  }
}

.plan-detail {
  padding: 0 20px 20px;
}

.detail-summary {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-bottom: 1px dashed #ebeef5;
  margin-bottom: 16px;

  .summary-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .summary-label {
      font-size: 13px;
      color: #909399;
    }

    .summary-value {
      font-size: 15px;
      font-weight: 600;
    }
  }
}

.accessory-cell {
  display: flex;
  align-items: center;
  gap: 10px;

  .accessory-thumb {
    width: 40px;
    height: 40px;
    border-radius: 6px;
    flex-shrink: 0;
    background: #f5f7fa;
  }

  .accessory-info {
    min-width: 0;

    .accessory-name {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
      line-height: 1.3;
    }

    .accessory-spec {
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 160px;
    }
  }
}

.cycle-status-cell {
  display: flex;
  align-items: center;
}

.cycle-bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.cycle-bar-bg {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.cycle-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s;
}

.cycle-bar-pct {
  font-size: 12px;
  font-weight: 600;
  min-width: 36px;
  text-align: right;
}

.date-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .expected-date {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
  }
}

.suggestion-cell {
  display: flex;
  align-items: flex-start;
  gap: 6px;

  .suggestion-text {
    font-size: 13px;
    color: #606266;
    line-height: 1.4;
  }
}
</style>
