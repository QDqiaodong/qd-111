<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">配件耗材建档</h2>
      <div class="table-toolbar">
        <el-button @click="goToSpecPage">
          <el-icon><Operation /></el-icon>规格对照
        </el-button>
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
              :src="getImageMeta(row).imageUrl"
              :preview-src-list="[getImageMeta(row).imageUrl]"
              fit="cover"
              style="width: 44px; height: 44px; border-radius: 6px"
            />
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
        <el-table-column label="风险等级" width="120" align="center">
          <template #default="{ row }">
            <el-tooltip :content="getRiskTooltip(row)" placement="top">
              <el-tag :color="getRiskBgColor(row)" :style="{ color: getRiskColor(row), borderColor: getRiskColor(row), borderWidth: '1px' }" effect="light" size="small">
                <span style="font-weight: 600">{{ getRiskLabel(row) }}</span>
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="周期状态" width="160" align="center">
          <template #default="{ row }">
            <div class="cycle-status-cell">
              <div class="cycle-bar-wrap">
                <div class="cycle-bar-bg">
                  <div
                    class="cycle-bar-fill"
                    :style="{
                      width: getCyclePercent(row) + '%',
                      background: getCycleBarColor(row)
                    }"
                  />
                </div>
                <span class="cycle-bar-pct" :style="{ color: getCycleBarColor(row) }">{{ getCyclePercent(row) }}%</span>
              </div>
              <span class="cycle-stage-tag" :class="'stage-' + getCycleStage(row)">{{ getCycleStageLabel(row) }}</span>
            </div>
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
              <el-form-item label="标准更换周期(天)" prop="standardCycle">
                <el-input-number v-model="form.standardCycle" :min="1" :max="3650" style="width: 100%" @change="handleStandardCycleChange" :disabled="!useManualCycle && cycleRuleMatchData?.matchedCycle" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="使用规则库">
                <el-switch v-model="useManualCycle" active-text="人工覆盖" inactive-text="规则匹配" @change="handleUseManualCycleChange" />
              </el-form-item>
            </el-col>
          </el-row>

          <div v-if="cycleRuleMatchData || cycleRuleMatchLoading" class="cycle-rule-match-panel" v-loading="cycleRuleMatchLoading">
            <div class="reference-panel-header">
              <el-icon><MagicStick /></el-icon>
              <span>周期规则匹配</span>
            </div>
            <template v-if="cycleRuleMatchData">
              <div class="match-result-row">
                <div class="match-result-main">
                  <span class="match-cycle-value">{{ cycleRuleMatchData.matchedCycle || '-' }}</span>
                  <span class="match-cycle-unit">天</span>
                  <span class="match-cycle-label">({{ cycleRuleMatchData.matchedCycleLabel || '' }})</span>
                </div>
                <el-tag v-if="cycleRuleMatchData.fromManualOverride" type="warning" size="small">人工覆盖</el-tag>
                <el-tag v-else type="success" size="small">规则匹配</el-tag>
              </div>
              <div v-if="cycleRuleMatchData.specDescription" class="match-detail">
                <span class="match-detail-label">匹配规则：</span>
                <span class="match-detail-value">{{ cycleRuleMatchData.specDescription }}</span>
              </div>
              <div v-if="cycleRuleMatchData.suggestion" class="match-suggestion">
                <el-tag :type="cycleRuleMatchData.fromManualOverride ? 'warning' : 'info'" effect="light" class="suggestion-tag">
                  <el-icon><InfoFilled /></el-icon>
                  {{ cycleRuleMatchData.suggestion }}
                </el-tag>
              </div>
              <div v-if="cycleRuleMatchData.candidateRules && cycleRuleMatchData.candidateRules.length > 1" class="candidate-rules">
                <div class="candidate-title">其他可选规则：</div>
                <div class="candidate-list">
                  <div
                    v-for="rule in cycleRuleMatchData.candidateRules.slice(1, 4)"
                    :key="rule.id"
                    class="candidate-item"
                    @click="applyCandidateRule(rule)"
                  >
                    <span class="candidate-cycle">{{ rule.standardCycle }}天</span>
                    <span class="candidate-desc">{{ rule.specDescription || rule.instrumentName }}</span>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <div v-if="compatibilityData || compatibilityLoading" 
               class="compatibility-panel" 
               :class="{
                 'has-error': compatibilityData && !compatibilityData.compatible,
                 'has-warning': compatibilityData && compatibilityData.compatible && compatibilityData.hasIssues && compatibilityData.hasIssues()
               }"
               v-loading="compatibilityLoading">
            <div class="reference-panel-header">
              <el-icon><CircleCheck /></el-icon>
              <span>适配校验</span>
              <el-tag
                v-if="compatibilityData"
                :type="compatibilityData.compatible ? (compatibilityData.hasIssues && compatibilityData.hasIssues() ? 'warning' : 'success') : 'danger'"
                size="small"
                class="compatibility-tag"
              >
                {{ compatibilityData.compatible ? (compatibilityData.hasIssues && compatibilityData.hasIssues() ? '有警告' : '适配良好') : '不匹配' }}
              </el-tag>
            </div>
            <template v-if="compatibilityData">
              <div v-if="compatibilityData.summary" class="compatibility-summary">
                {{ compatibilityData.summary }}
              </div>
              <div v-if="compatibilityData.errors && compatibilityData.errors.length > 0" class="compatibility-issues">
                <div v-for="(error, index) in compatibilityData.errors" :key="'error-' + index" class="issue-item error-item">
                  <el-icon color="#f56c6c"><CircleClose /></el-icon>
                  <span>{{ error }}</span>
                </div>
              </div>
              <div v-if="compatibilityData.warnings && compatibilityData.warnings.length > 0" class="compatibility-issues">
                <div v-for="(warning, index) in compatibilityData.warnings" :key="'warning-' + index" class="issue-item warning-item">
                  <el-icon color="#e6a23c"><Warning /></el-icon>
                  <span>{{ warning }}</span>
                </div>
              </div>
              <div v-if="compatibilityData.suggestion" class="compatibility-suggestion">
                <el-tag type="info" effect="light" class="suggestion-tag">
                  <el-icon><InfoFilled /></el-icon>
                  {{ compatibilityData.suggestion }}
                </el-tag>
              </div>
            </template>
          </div>

          <div v-if="cycleReferenceData || cycleReferenceLoading" class="cycle-reference-panel" v-loading="cycleReferenceLoading">
            <div class="reference-panel-header">
              <el-icon><InfoFilled /></el-icon>
              <span>周期参考信息</span>
            </div>
            <template v-if="cycleReferenceData">
              <div class="reference-grid">
                <div class="reference-item">
                  <div class="reference-label">标准周期</div>
                  <div class="reference-value">
                    <span class="value-main">{{ cycleReferenceData.standardCycle || '-' }} 天</span>
                    <span class="value-sub">{{ cycleReferenceData.standardCycleLabel || '' }}</span>
                  </div>
                </div>
                <div class="reference-item">
                  <div class="reference-label">上次同类更换</div>
                  <div class="reference-value">
                    <template v-if="cycleReferenceData.lastInterval">
                      <span class="value-main">{{ cycleReferenceData.lastInterval }} 天</span>
                      <span class="value-sub">{{ cycleReferenceData.lastReplaceDate || '' }}</span>
                    </template>
                    <span v-else class="value-empty">暂无记录</span>
                  </div>
                </div>
                <div class="reference-item">
                  <div class="reference-label">历史平均</div>
                  <div class="reference-value">
                    <template v-if="cycleReferenceData.averageInterval">
                      <span class="value-main">{{ cycleReferenceData.averageInterval }} 天</span>
                      <span class="value-sub">共 {{ cycleReferenceData.historyCount || 0 }} 条记录</span>
                    </template>
                    <span v-else class="value-empty">暂无数据</span>
                  </div>
                </div>
              </div>
              <div v-if="cycleReferenceData.currentInputCycle > 0" class="diff-section">
                <div class="diff-title">当前输入对比</div>
                <div class="diff-grid">
                  <div class="diff-item">
                    <span class="diff-label">与标准周期：</span>
                    <span :class="['diff-value', getDiffClass(cycleReferenceData.diffFromStandard)]">
                      {{ cycleReferenceData.diffFromStandardLabel || '-' }}
                    </span>
                  </div>
                  <div v-if="cycleReferenceData.lastInterval" class="diff-item">
                    <span class="diff-label">与上次更换：</span>
                    <span :class="['diff-value', getDiffClass(cycleReferenceData.diffFromLast)]">
                      {{ cycleReferenceData.diffFromLastLabel || '-' }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-if="cycleReferenceData.suggestion" class="suggestion-section">
                <el-tag :type="getSuggestionTagType(cycleReferenceData.suggestionLevel)" effect="light" class="suggestion-tag">
                  <el-icon><Warning /></el-icon>
                  {{ cycleReferenceData.suggestion }}
                </el-tag>
              </div>
            </template>
          </div>
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
            <div style="font-size: 12px; color: #909399; margin-top: 6px">
              支持 JPG/PNG，最大 5MB，自动压缩至 1920px 以内
              <span v-if="form.imageWidth && form.imageHeight" style="margin-left: 8px">
                尺寸：{{ form.imageWidth }} × {{ form.imageHeight }}px
              </span>
              <span v-if="form.imageSize" style="margin-left: 8px">
                大小：{{ formatFileSize(form.imageSize) }}
              </span>
            </div>
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

    <el-dialog v-model="viewVisible" title="配件详情" width="720px" destroy-on-close>
      <template v-if="currentRow">
        <div class="detail-image-section">
          <el-image
            :src="getImageMeta(currentRow).imageUrl"
            :preview-src-list="[getImageMeta(currentRow).imageUrl]"
            fit="cover"
            class="detail-image"
          />
          <div v-if="currentRow.imageWidth && currentRow.imageHeight" class="detail-image-meta">
            尺寸：{{ currentRow.imageWidth }} × {{ currentRow.imageHeight }}px
            <span v-if="currentRow.imageSize" style="margin-left: 12px">
              大小：{{ formatFileSize(currentRow.imageSize) }}
            </span>
          </div>
        </div>
        <el-descriptions :column="2" border>
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

        <div class="lifecycle-section">
          <div class="lifecycle-section-title">
            <el-icon><Timer /></el-icon>
            生命周期视图
          </div>
          <div v-loading="lifecycleLoading" class="lifecycle-timeline-wrap">
            <template v-if="lifecycleData">
              <div class="lifecycle-overview">
                <div class="lifecycle-stage-badge" :class="'stage-' + lifecycleData.stage">
                  {{ lifecycleData.stageLabel }}
                </div>
                <div class="lifecycle-progress-wrap">
                  <div class="lifecycle-progress-bar">
                    <div
                      class="lifecycle-progress-fill"
                      :style="{
                        width: lifecycleData.cyclePercent + '%',
                        background: getStageColor(lifecycleData.stage)
                      }"
                    />
                    <div class="lifecycle-progress-markers">
                      <div class="lifecycle-marker" style="left: 0%">
                        <span class="marker-label">采购</span>
                      </div>
                      <div class="lifecycle-marker" style="left: 50%">
                        <span class="marker-label">中期</span>
                      </div>
                      <div class="lifecycle-marker" style="left: 80%">
                        <span class="marker-label">预警</span>
                      </div>
                      <div class="lifecycle-marker" style="left: 100%">
                        <span class="marker-label">到期</span>
                      </div>
                    </div>
                  </div>
                  <div class="lifecycle-progress-info">
                    <template v-if="currentRow.standardCycle && currentRow.standardCycle > 0">
                      已使用 {{ lifecycleData.usedDays }} 天 / 标准周期 {{ currentRow.standardCycle }} 天
                    </template>
                    <template v-else>
                      已使用 {{ lifecycleData.usedDays }} 天 / <span style="color: #e6a23c">未设置标准周期，无法计算到期提醒</span>
                    </template>
                  </div>
                </div>
              </div>

              <div class="lifecycle-timeline">
                <div class="timeline-item">
                  <div class="timeline-dot dot-purchase" />
                  <div class="timeline-content">
                    <div class="timeline-label">采购日期</div>
                    <div class="timeline-value">{{ currentRow.purchaseDate || '-' }}</div>
                  </div>
                </div>
                <div class="timeline-item">
                  <div class="timeline-dot dot-cycle" />
                  <div class="timeline-content">
                    <div class="timeline-label">标准更换周期</div>
                    <div class="timeline-value">{{ currentRow.standardCycle ? currentRow.standardCycle + ' 天' : '-' }}</div>
                  </div>
                </div>
                <div class="timeline-item">
                  <div class="timeline-dot dot-used" />
                  <div class="timeline-content">
                    <div class="timeline-label">已使用天数</div>
                    <div class="timeline-value">
                      <span :style="{ color: getStageColor(lifecycleData.stage) }">{{ lifecycleData.usedDays }}</span> 天
                      <span class="timeline-sub" v-if="lifecycleData.daysLeft > 0">（剩余 {{ lifecycleData.daysLeft }} 天）</span>
                      <span class="timeline-sub timeline-sub-danger" v-else-if="lifecycleData.daysLeft < 0">（已超期 {{ Math.abs(lifecycleData.daysLeft) }} 天）</span>
                      <span class="timeline-sub" v-else>（已达标准周期）</span>
                    </div>
                  </div>
                </div>
                <div class="timeline-item">
                  <div class="timeline-dot dot-replace" />
                  <div class="timeline-content">
                    <div class="timeline-label">最近更换</div>
                    <div class="timeline-value">{{ lifecycleData.lastReplaceDate || '暂无更换记录' }}</div>
                  </div>
                </div>
                <div class="timeline-item">
                  <div class="timeline-dot dot-worn" />
                  <div class="timeline-content">
                    <div class="timeline-label">当前损耗</div>
                    <div class="timeline-value">
                      <el-tag :type="getWornTagType(currentRow.wornStatus)" size="small">{{ getWornLabel(currentRow.wornStatus) }}</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无生命周期数据" :image-size="60" />
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToSpecComparison">
          <el-icon><Operation /></el-icon>查看规格对照
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Operation, InfoFilled, Warning, MagicStick, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { accessoryApi, dictApi, groupApi, replacementApi } from '@/api'
import { compressImage, getImageMeta, getImageDimensions, MAX_IMAGE_SIZE, formatFileSize } from '@/utils/image'
import BatchActionBar from '@/components/BatchActionBar.vue'

const router = useRouter()

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
const lifecycleLoading = ref(false)
const lifecycleData = ref(null)
const replacementHistory = ref([])
const cycleReferenceLoading = ref(false)
const cycleReferenceData = ref(null)
const compatibilityLoading = ref(false)
const compatibilityData = ref(null)
const cycleRuleMatchLoading = ref(false)
const cycleRuleMatchData = ref(null)
const useManualCycle = ref(false)

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

const DEFAULT_STANDARD_CYCLE = 90

const TYPE_CYCLE_DEFAULTS = {
  string: 90,
  bow: 365,
  pick: 60,
  rosin: 180,
  capo: 730,
  strap: 730,
  cleaner: 180,
  other: 90
}

const form = reactive({
  id: null,
  name: '',
  typeCode: '',
  specification: '',
  instrument: '',
  groupId: null,
  brandModel: '',
  standardCycle: DEFAULT_STANDARD_CYCLE,
  purchaseDate: '',
  wornStatus: 'good',
  imageUrl: '',
  imageWidth: null,
  imageHeight: null,
  imageSize: null,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入配件名称', trigger: 'blur' }],
  typeCode: [{ required: true, message: '请选择配件类型', trigger: 'change' }],
  instrument: [{ required: true, message: '请选择适配乐器', trigger: 'change' }],
  groupId: [{ required: true, message: '请选择分组', trigger: 'change' }],
  specification: [{ required: true, message: '请输入规格描述', trigger: 'blur' }],
  standardCycle: [{ required: true, message: '请输入标准更换周期', trigger: 'change' }],
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
    standardCycle: DEFAULT_STANDARD_CYCLE,
    purchaseDate: dayjs().format('YYYY-MM-DD'),
    wornStatus: 'good',
    imageUrl: '',
    imageWidth: null,
    imageHeight: null,
    imageSize: null,
    remark: ''
  })
  cycleReferenceData.value = null
  compatibilityData.value = null
  cycleRuleMatchData.value = null
  useManualCycle.value = false
  formRef.value?.resetFields()
}

const handleAdd = () => {
  dialogMode.value = 'add'
  resetForm()
  form.purchaseDate = dayjs().format('YYYY-MM-DD')
  dialogVisible.value = true
}

const handleStandardCycleChange = (val) => {
  if (val === undefined || val === null) {
    const defaultCycle = TYPE_CYCLE_DEFAULTS[form.typeCode] || DEFAULT_STANDARD_CYCLE
    form.standardCycle = defaultCycle
    ElMessage.info(`标准周期已恢复为默认值 ${defaultCycle} 天`)
  }
}

watch(() => form.typeCode, (newType, oldType) => {
  if (newType && newType !== oldType && dialogMode.value === 'add' && !useManualCycle.value) {
    loadCycleRuleMatch()
  }
  loadCycleReference()
  loadCompatibilityCheck()
})

watch(() => form.instrument, () => {
  if (!useManualCycle.value) {
    loadCycleRuleMatch()
  }
  loadCycleReference()
  loadCompatibilityCheck()
})

watch(() => form.specification, () => {
  if (!useManualCycle.value) {
    loadCycleRuleMatch()
  }
  loadCompatibilityCheck()
})

watch(() => form.standardCycle, () => {
  loadCycleReference()
  if (useManualCycle.value) {
    loadCycleRuleMatch()
  }
})

const loadCycleReference = async () => {
  if (!form.typeCode || !form.instrument) {
    cycleReferenceData.value = null
    return
  }
  cycleReferenceLoading.value = true
  try {
    const res = await accessoryApi.getCycleReference({
      typeCode: form.typeCode,
      instrument: form.instrument,
      currentCycle: form.standardCycle
    })
    if (res && res.data) {
      cycleReferenceData.value = res.data
    } else {
      cycleReferenceData.value = null
    }
  } catch (e) {
    cycleReferenceData.value = null
  } finally {
    cycleReferenceLoading.value = false
  }
}

const loadCycleRuleMatch = async () => {
  if (!form.typeCode) {
    cycleRuleMatchData.value = null
    return
  }
  cycleRuleMatchLoading.value = true
  try {
    const params = {
      typeCode: form.typeCode,
      instrument: form.instrument,
      specification: form.specification
    }
    if (useManualCycle.value && form.standardCycle > 0) {
      params.manualCycle = form.standardCycle
    }
    const res = await accessoryApi.getCycleRuleMatch(params)
    if (res && res.data) {
      cycleRuleMatchData.value = res.data
      if (!useManualCycle.value && res.data.matchedCycle && dialogMode.value === 'add') {
        form.standardCycle = res.data.matchedCycle
      }
    } else {
      cycleRuleMatchData.value = null
    }
  } catch (e) {
    cycleRuleMatchData.value = null
  } finally {
    cycleRuleMatchLoading.value = false
  }
}

const loadCompatibilityCheck = async () => {
  if (!form.typeCode) {
    compatibilityData.value = null
    return
  }
  compatibilityLoading.value = true
  try {
    const res = await accessoryApi.checkCompatibility({
      typeCode: form.typeCode,
      instrument: form.instrument,
      specification: form.specification
    })
    if (res && res.data) {
      compatibilityData.value = res.data
    } else {
      compatibilityData.value = null
    }
  } catch (e) {
    compatibilityData.value = null
  } finally {
    compatibilityLoading.value = false
  }
}

const handleUseManualCycleChange = (val) => {
  if (!val) {
    useManualCycle.value = false
    loadCycleRuleMatch()
  } else {
    useManualCycle.value = true
    if (cycleRuleMatchData.value?.matchedCycle) {
      loadCycleRuleMatch()
    }
  }
}

const applyCandidateRule = (rule) => {
  form.standardCycle = rule.standardCycle
  useManualCycle.value = true
  ElMessage.success(`已应用周期：${rule.standardCycle}天`)
}

const getSuggestionTagType = (level) => {
  const map = { success: 'success', warning: 'warning', danger: 'danger', info: 'info' }
  return map[level] || 'info'
}

const getDiffClass = (diff) => {
  if (diff === null || diff === undefined) return 'diff-neutral'
  if (diff === 0) return 'diff-success'
  if (Math.abs(diff) <= 10) return 'diff-warning'
  return 'diff-danger'
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  originalRowForEdit.value = { ...row }
  Object.assign(form, row)
  useManualCycle.value = true
  dialogVisible.value = true
  nextTick(() => {
    loadCycleReference()
    loadCycleRuleMatch()
    loadCompatibilityCheck()
  })
}

const handleView = async (row) => {
  currentRow.value = row
  viewVisible.value = true
  lifecycleLoading.value = true
  lifecycleData.value = null
  try {
    const res = await accessoryApi.getLifecycle(row.id)
    if (res && res.data) {
      lifecycleData.value = res.data
    } else {
      lifecycleData.value = computeLifecycleLocal(row)
    }
  } catch {
    lifecycleData.value = computeLifecycleLocal(row)
  } finally {
    lifecycleLoading.value = false
  }
  try {
    const hRes = await replacementApi.history(row.id)
    replacementHistory.value = hRes.data || hRes || []
  } catch {
    replacementHistory.value = []
  }
}

const computeLifecycleLocal = (row) => {
  const purchaseDate = row.purchaseDate ? dayjs(row.purchaseDate) : null
  const standardCycle = row.standardCycle || 0
  const usedDays = purchaseDate ? dayjs().diff(purchaseDate, 'day') : 0
  const cyclePercent = standardCycle > 0 ? Math.min(Math.round((usedDays / standardCycle) * 100), 100) : 0
  const daysLeft = standardCycle > 0 ? standardCycle - usedDays : 0
  let lastReplaceDate = null
  if (replacementHistory.value.length > 0) {
    const sorted = [...replacementHistory.value].sort((a, b) => dayjs(b.replaceDate).valueOf() - dayjs(a.replaceDate).valueOf())
    lastReplaceDate = sorted[0].replaceDate
  }
  let stage
  if (standardCycle <= 0) {
    stage = determineStage(0, row.wornStatus)
  } else {
    stage = determineStage(cyclePercent, row.wornStatus)
  }
  return {
    accessoryId: row.id,
    name: row.name,
    purchaseDate: row.purchaseDate,
    standardCycle,
    usedDays: Math.max(usedDays, 0),
    daysLeft,
    cyclePercent,
    lastReplaceDate,
    wornStatus: row.wornStatus,
    stage,
    stageLabel: standardCycle > 0 ? getStageLabelByCode(stage) : '未设置周期'
  }
}

const determineStage = (cyclePercent, wornStatus) => {
  if (wornStatus === 'broken') return 'broken'
  if (cyclePercent >= 100 || wornStatus === 'severe') return 'expired'
  if (cyclePercent >= 80) return 'warning'
  if (cyclePercent >= 50) return 'aging'
  return 'fresh'
}

const getStageLabelByCode = (stage) => {
  const map = { fresh: '初期', aging: '中期', warning: '临近更换', expired: '已超期', broken: '已损坏' }
  return map[stage] || '未知'
}

const getStageColor = (stage) => {
  const map = { fresh: '#67c23a', aging: '#409eff', warning: '#e6a23c', expired: '#f56c6c', broken: '#909399' }
  return map[stage] || '#c0c4cc'
}

const getCyclePercent = (row) => {
  if (!row.standardCycle || row.standardCycle <= 0) return 0
  const purchaseDate = row.purchaseDate ? dayjs(row.purchaseDate) : null
  if (!purchaseDate) return 0
  const usedDays = dayjs().diff(purchaseDate, 'day')
  return Math.min(Math.round((usedDays / row.standardCycle) * 100), 100)
}

const getCycleBarColor = (row) => {
  const pct = getCyclePercent(row)
  if (pct >= 100) return '#f56c6c'
  if (pct >= 80) return '#e6a23c'
  if (pct >= 50) return '#409eff'
  return '#67c23a'
}

const getCycleStage = (row) => {
  const pct = getCyclePercent(row)
  return determineStage(pct, row.wornStatus)
}

const getCycleStageLabel = (row) => {
  return getStageLabelByCode(getCycleStage(row))
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
      loadList()
    } catch (e) {
      if (e?.message !== 'cancel') {
        ElMessage.error(e?.message || '删除失败，请稍后重试')
      }
    }
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
      clearSelection()
      loadList()
    } catch (e) {
      if (e?.message !== 'cancel') {
        ElMessage.error(e?.message || '批量删除失败，请稍后重试')
      }
    }
  }).catch(() => {})
}

const handleBatchStatus = async (code) => {
  try {
    await accessoryApi.batchUpdateStatus(selectedRows.value.map(r => r.id), code)
    ElMessage.success('批量更新成功')
    clearSelection()
    loadList()
  } catch (e) {
    if (e?.message !== 'cancel') {
      ElMessage.error(e?.message || '批量更新失败，请稍后重试')
    }
  }
}

const originalRowForEdit = ref(null)

const handleSubmit = async () => {
  await formRef.value.validate()
  if (!form.standardCycle || form.standardCycle <= 0) {
    form.standardCycle = TYPE_CYCLE_DEFAULTS[form.typeCode] || DEFAULT_STANDARD_CYCLE
  }
  submitting.value = true
  const originalForm = { ...form }

  let changedFields = []
  if (dialogMode.value === 'edit' && originalRowForEdit.value) {
    const orig = originalRowForEdit.value
    if (orig.purchaseDate !== form.purchaseDate) changedFields.push('采购日期')
    if (orig.standardCycle !== form.standardCycle) changedFields.push('标准周期')
    if (orig.typeCode !== form.typeCode) changedFields.push('配件类型')
    if (orig.instrument !== form.instrument) changedFields.push('适配乐器')
    if (orig.specification !== form.specification) changedFields.push('规格参数')
  }

  try {
    if (dialogMode.value === 'add') {
      await accessoryApi.add(form)
      ElMessage.success('保存成功')
    } else {
      await accessoryApi.update(form)
      if (changedFields.length > 0) {
        ElMessage.success({
          message: `保存成功，已自动重算历史更换记录（变更：${changedFields.join('、')}）`,
          duration: 3000,
          showClose: true
        })
      } else {
        ElMessage.success('保存成功')
      }
    }
    dialogVisible.value = false
    loadList()
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

const handleDialogClose = () => {
  resetForm()
}

const beforeImageUpload = async (file) => {
  return false
}

const handleImageChange = async (uploadFile) => {
  try {
    if (uploadFile.raw && uploadFile.raw.size > MAX_IMAGE_SIZE) {
      ElMessage.warning(`图片大小不能超过 ${formatFileSize(MAX_IMAGE_SIZE)}，将自动压缩`)
    }
    const compressed = await compressImage(uploadFile.raw, { quality: 0.7, maxWidth: 1280 })
    const dimensions = await getImageDimensions(compressed)
    const reader = new FileReader()
    reader.onload = (e) => {
      form.imageUrl = e.target.result
      form.imageWidth = dimensions.width
      form.imageHeight = dimensions.height
      form.imageSize = compressed.size
    }
    reader.readAsDataURL(compressed)
  } catch (err) {
    try {
      const dimensions = await getImageDimensions(uploadFile.raw)
      const reader = new FileReader()
      reader.onload = (e) => {
        form.imageUrl = e.target.result
        form.imageWidth = dimensions.width
        form.imageHeight = dimensions.height
        form.imageSize = uploadFile.raw.size
      }
      reader.readAsDataURL(uploadFile.raw)
    } catch {
      const reader = new FileReader()
      reader.onload = (e) => {
        form.imageUrl = e.target.result
        form.imageWidth = null
        form.imageHeight = null
        form.imageSize = uploadFile.raw?.size || null
      }
      reader.readAsDataURL(uploadFile.raw)
    }
  }
}

const handleImageRemove = () => {
  form.imageUrl = ''
  form.imageWidth = null
  form.imageHeight = null
  form.imageSize = null
}

const getWornLabel = (code) => {
  const item = wornStatuses.value.find(w => w.code === code)
  return item ? item.label : code
}

const getWornTagType = (code) => {
  const map = { good: 'success', slight: 'warning', severe: 'danger', broken: 'info' }
  return map[code] || 'info'
}

const RISK_LEVELS = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  CRITICAL: 'critical',
  EXTREME: 'extreme'
}

const RISK_LABELS = {
  low: '低风险',
  medium: '中风险',
  high: '高风险',
  critical: '严重风险',
  extreme: '极端风险'
}

const RISK_COLORS = {
  low: '#67c23a',
  medium: '#409eff',
  high: '#e6a23c',
  critical: '#f56c6c',
  extreme: '#9c27b0'
}

const RISK_BG_COLORS = {
  low: '#f0f9eb',
  medium: '#ecf5ff',
  high: '#fdf6ec',
  critical: '#fef0f0',
  extreme: '#f3e5f5'
}

const WORN_STATUS_SCORES = {
  good: 0,
  slight: 20,
  severe: 70,
  broken: 100
}

const calculateRisk = (row) => {
  const standardCycle = row.standardCycle || 0
  const purchaseDate = row.purchaseDate ? dayjs(row.purchaseDate) : null
  const usedDays = purchaseDate ? Math.max(dayjs().diff(purchaseDate, 'day'), 0) : 0
  const cyclePercent = standardCycle > 0 ? Math.min(Math.round((usedDays / standardCycle) * 100), 100) : 0
  const daysLeft = standardCycle > 0 ? standardCycle - usedDays : null

  let cycleScore = 0
  if (standardCycle > 0) {
    if (cyclePercent >= 150) cycleScore = 95
    else if (cyclePercent >= 120) cycleScore = 85
    else if (cyclePercent >= 100) cycleScore = 75
    else if (cyclePercent >= 90) cycleScore = 60
    else if (cyclePercent >= 80) cycleScore = 45
    else if (cyclePercent >= 50) cycleScore = 25
    else if (cyclePercent >= 30) cycleScore = 10
  }

  const wornScore = WORN_STATUS_SCORES[row.wornStatus] || 0

  let expiredPenalty = 0
  if (daysLeft !== null && daysLeft < 0 && standardCycle > 0) {
    const overdueDays = Math.abs(daysLeft)
    const overdueRatio = overdueDays / standardCycle
    if (overdueRatio >= 1.0) expiredPenalty = 25
    else if (overdueRatio >= 0.5) expiredPenalty = 18
    else if (overdueRatio >= 0.2) expiredPenalty = 12
    else expiredPenalty = 6
  }

  let cycleWeight = 0.45
  let wornWeight = 0.40
  const penaltyWeight = 0.15

  if (row.wornStatus === 'broken') {
    wornWeight = 0.60
    cycleWeight = 0.25
  }

  let combinedScore = Math.round(cycleScore * cycleWeight + wornScore * wornWeight + expiredPenalty * penaltyWeight)

  if (row.wornStatus === 'broken') {
    combinedScore = Math.max(combinedScore, 90)
  } else if (row.wornStatus === 'severe') {
    combinedScore = Math.max(combinedScore, 65)
  }

  combinedScore = Math.min(Math.max(combinedScore, 0), 100)

  let level = RISK_LEVELS.LOW
  if (row.wornStatus === 'broken') {
    level = RISK_LEVELS.EXTREME
  } else if (row.wornStatus === 'severe' && combinedScore >= 80) {
    level = RISK_LEVELS.CRITICAL
  } else if (daysLeft !== null && daysLeft < 0) {
    const overdue = Math.abs(daysLeft)
    if (overdue >= 180) {
      level = RISK_LEVELS.CRITICAL
    } else if (overdue >= 60) {
      level = combinedScore >= 80 ? RISK_LEVELS.CRITICAL : RISK_LEVELS.HIGH
    }
  }

  if (level === RISK_LEVELS.LOW) {
    if (combinedScore >= 90) level = RISK_LEVELS.EXTREME
    else if (combinedScore >= 75) level = RISK_LEVELS.CRITICAL
    else if (combinedScore >= 55) level = RISK_LEVELS.HIGH
    else if (combinedScore >= 30) level = RISK_LEVELS.MEDIUM
  }

  return {
    level,
    label: RISK_LABELS[level],
    color: RISK_COLORS[level],
    bgColor: RISK_BG_COLORS[level],
    score: combinedScore,
    cyclePercent,
    daysLeft,
    usedDays
  }
}

const getRiskLabel = (row) => {
  return calculateRisk(row).label
}

const getRiskColor = (row) => {
  return calculateRisk(row).color
}

const getRiskBgColor = (row) => {
  return calculateRisk(row).bgColor
}

const getRiskTooltip = (row) => {
  const risk = calculateRisk(row)
  const parts = [`风险得分: ${risk.score}`]
  if (row.standardCycle > 0) {
    parts.push(`周期使用: ${risk.cyclePercent}%`)
    if (risk.daysLeft !== null) {
      if (risk.daysLeft >= 0) {
        parts.push(`剩余: ${risk.daysLeft}天`)
      } else {
        parts.push(`已超期: ${Math.abs(risk.daysLeft)}天`)
      }
    }
  }
  return parts.join(' | ')
}

const goToSpecPage = () => {
  router.push({ path: '/spec-comparison' })
}

const goToSpecComparison = () => {
  if (!currentRow.value) return
  viewVisible.value = false
  router.push({
    path: '/spec-comparison',
    query: {
      instrument: currentRow.value.instrument,
      instrumentName: currentRow.value.instrumentName,
      accessoryId: currentRow.value.id
    }
  })
}

onMounted(() => {
  loadDict()
  loadList()
})
</script>

<style lang="scss" scoped>
.cycle-status-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .cycle-bar-wrap {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
  }

  .cycle-bar-bg {
    flex: 1;
    height: 6px;
    background: #ebeef5;
    border-radius: 3px;
    overflow: hidden;
  }

  .cycle-bar-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.4s ease;
  }

  .cycle-bar-pct {
    font-size: 11px;
    font-weight: 600;
    min-width: 32px;
    text-align: right;
  }

  .cycle-stage-tag {
    font-size: 11px;
    padding: 1px 8px;
    border-radius: 10px;
    line-height: 1.6;

    &.stage-fresh {
      background: #f0f9eb;
      color: #67c23a;
    }
    &.stage-aging {
      background: #ecf5ff;
      color: #409eff;
    }
    &.stage-warning {
      background: #fdf6ec;
      color: #e6a23c;
    }
    &.stage-expired {
      background: #fef0f0;
      color: #f56c6c;
    }
    &.stage-broken {
      background: #f4f4f5;
      color: #909399;
    }
  }
}

.lifecycle-section {
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}

.lifecycle-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;

  .el-icon {
    color: #409eff;
  }
}

.lifecycle-timeline-wrap {
  min-height: 100px;
}

.lifecycle-overview {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.lifecycle-stage-badge {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;

  &.stage-fresh {
    background: #f0f9eb;
    color: #67c23a;
  }
  &.stage-aging {
    background: #ecf5ff;
    color: #409eff;
  }
  &.stage-warning {
    background: #fdf6ec;
    color: #e6a23c;
  }
  &.stage-expired {
    background: #fef0f0;
    color: #f56c6c;
  }
  &.stage-broken {
    background: #f4f4f5;
    color: #909399;
  }
}

.lifecycle-progress-wrap {
  flex: 1;
  min-width: 0;
}

.lifecycle-progress-bar {
  position: relative;
  height: 10px;
  background: #ebeef5;
  border-radius: 5px;
  overflow: hidden;
}

.lifecycle-progress-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s ease;
  position: relative;
  z-index: 1;
}

.lifecycle-progress-markers {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2;
  pointer-events: none;
}

.lifecycle-marker {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: rgba(0, 0, 0, 0.12);

  .marker-label {
    position: absolute;
    top: 14px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 10px;
    color: #909399;
    white-space: nowrap;
  }
}

.lifecycle-progress-info {
  margin-top: 22px;
  font-size: 12px;
  color: #606266;
}

.lifecycle-timeline {
  position: relative;
  padding-left: 24px;

  &::before {
    content: '';
    position: absolute;
    left: 7px;
    top: 8px;
    bottom: 8px;
    width: 2px;
    background: linear-gradient(to bottom, #409eff, #67c23a, #e6a23c, #f56c6c);
    border-radius: 1px;
  }
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
  display: flex;
  align-items: flex-start;

  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px currentColor;

  &.dot-purchase {
    color: #409eff;
    background: #409eff;
  }
  &.dot-cycle {
    color: #67c23a;
    background: #67c23a;
  }
  &.dot-used {
    color: #e6a23c;
    background: #e6a23c;
  }
  &.dot-replace {
    color: #909399;
    background: #909399;
  }
  &.dot-worn {
    color: #f56c6c;
    background: #f56c6c;
  }
}

.timeline-content {
  padding-left: 4px;
}

.timeline-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.timeline-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.timeline-sub {
  font-size: 12px;
  color: #909399;
  font-weight: 400;

  &.timeline-sub-danger {
    color: #f56c6c;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}

.cycle-reference-panel {
  margin-top: 8px;
  padding: 16px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;

  .reference-panel-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #409eff;
    margin-bottom: 12px;

    .el-icon {
      font-size: 16px;
    }
  }

  .reference-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-bottom: 12px;
  }

  .reference-item {
    background: #fff;
    padding: 10px 12px;
    border-radius: 6px;
    border: 1px solid #ebeef5;

    .reference-label {
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }

    .reference-value {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .value-main {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .value-sub {
        font-size: 11px;
        color: #909399;
      }

      .value-empty {
        font-size: 12px;
        color: #c0c4cc;
      }
    }
  }

  .diff-section {
    background: #fff;
    padding: 10px 12px;
    border-radius: 6px;
    border: 1px solid #ebeef5;
    margin-bottom: 12px;

    .diff-title {
      font-size: 12px;
      color: #606266;
      font-weight: 500;
      margin-bottom: 8px;
    }

    .diff-grid {
      display: flex;
      gap: 24px;
    }

    .diff-item {
      display: flex;
      align-items: center;
      gap: 4px;

      .diff-label {
        font-size: 12px;
        color: #606266;
      }

      .diff-value {
        font-size: 13px;
        font-weight: 500;

        &.diff-success {
          color: #67c23a;
        }

        &.diff-warning {
          color: #e6a23c;
        }

        &.diff-danger {
          color: #f56c6c;
        }

        &.diff-neutral {
          color: #909399;
        }
      }
    }
  }

  .suggestion-section {
    .suggestion-tag {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 12px;
      font-size: 13px;
      border-radius: 6px;
      line-height: 1.5;

      .el-icon {
        font-size: 16px;
        flex-shrink: 0;
      }
    }
  }
}

.cycle-rule-match-panel {
  margin-top: 8px;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #7dd3fc;
  border-radius: 8px;

  .reference-panel-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #0284c7;
    margin-bottom: 12px;

    .el-icon {
      font-size: 16px;
    }
  }

  .match-result-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding: 12px;
    background: #fff;
    border-radius: 6px;
    border: 1px solid #bae6fd;
  }

  .match-result-main {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  .match-cycle-value {
    font-size: 28px;
    font-weight: 700;
    color: #0369a1;
  }

  .match-cycle-unit {
    font-size: 14px;
    color: #0369a1;
    font-weight: 500;
  }

  .match-cycle-label {
    font-size: 12px;
    color: #64748b;
    margin-left: 8px;
  }

  .match-detail {
    font-size: 13px;
    color: #475569;
    margin-bottom: 10px;

    .match-detail-label {
      color: #64748b;
      margin-right: 4px;
    }

    .match-detail-value {
      font-weight: 500;
      color: #334155;
    }
  }

  .match-suggestion {
    margin-bottom: 12px;

    .suggestion-tag {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 12px;
      font-size: 13px;
      border-radius: 6px;
      line-height: 1.5;
      background: #fff;
      border-color: #7dd3fc;
      color: #0369a1;

      .el-icon {
        font-size: 16px;
        flex-shrink: 0;
      }
    }
  }

  .candidate-rules {
    .candidate-title {
      font-size: 12px;
      color: #64748b;
      margin-bottom: 8px;
    }

    .candidate-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .candidate-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: #fff;
      border: 1px solid #cbd5e1;
      border-radius: 16px;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: #0ea5e9;
        background: #f0f9ff;
        transform: translateY(-1px);
      }

      .candidate-cycle {
        font-weight: 600;
        color: #0369a1;
      }

      .candidate-desc {
        color: #64748b;
      }
    }
  }
}

.compatibility-panel {
  margin-top: 8px;
  padding: 16px;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border: 1px solid #86efac;
  border-radius: 8px;

  .reference-panel-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #15803d;
    margin-bottom: 12px;

    .el-icon {
      font-size: 16px;
    }

    .compatibility-tag {
      margin-left: auto;
    }
  }

  &.has-error {
    background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
    border-color: #fca5a5;

    .reference-panel-header {
      color: #dc2626;
    }
  }

  &.has-warning {
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
    border-color: #fcd34d;

    .reference-panel-header {
      color: #b45309;
    }
  }

  .compatibility-summary {
    font-size: 14px;
    font-weight: 500;
    color: #334155;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: #fff;
    border-radius: 6px;
    border-left: 3px solid #22c55e;
  }

  .compatibility-issues {
    margin-bottom: 12px;
  }

  .issue-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 12px;
    background: #fff;
    border-radius: 6px;
    margin-bottom: 6px;
    font-size: 13px;
    line-height: 1.5;

    .el-icon {
      flex-shrink: 0;
      margin-top: 1px;
    }

    &.error-item {
      border-left: 3px solid #ef4444;
      color: #991b1b;
    }

    &.warning-item {
      border-left: 3px solid #f59e0b;
      color: #92400e;
    }
  }

  .compatibility-suggestion {
    .suggestion-tag {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 12px;
      font-size: 13px;
      border-radius: 6px;
      line-height: 1.5;
      background: #fff;
      border-color: #86efac;
      color: #15803d;

      .el-icon {
        font-size: 16px;
        flex-shrink: 0;
      }
    }
  }
}

.detail-image-section {
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;

  .detail-image {
    width: 160px;
    height: 160px;
    border-radius: 8px;
    border: 1px solid #ebeef5;
  }

  .detail-image-meta {
    margin-top: 10px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
