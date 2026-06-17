<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">更换节奏日历</h2>
      <div class="table-toolbar">
        <div class="legend-bar">
          <div class="legend-item">
            <span class="legend-dot expected"></span>
            <span class="legend-text">预计到期</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot replaced"></span>
            <span class="legend-text">已经更换</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot severe"></span>
            <span class="legend-text">严重损耗</span>
          </div>
        </div>
      </div>
    </div>

    <el-card class="card-shadow calendar-card" shadow="never">
      <div class="calendar-header">
        <el-button icon="ArrowLeft" circle @click="prevMonth" />
        <h3 class="calendar-title">{{ currentYear }}年{{ currentMonth }}月</h3>
        <el-button icon="ArrowRight" circle @click="nextMonth" />
        <el-button type="primary" size="small" @click="goToToday">
          今天
        </el-button>
      </div>

      <div class="calendar-summary">
        <div class="summary-item expected">
          <span class="summary-icon"><Warning /></span>
          <div class="summary-content">
            <span class="summary-value">{{ calendarData.expectedCount || 0 }}</span>
            <span class="summary-label">预计到期</span>
          </div>
        </div>
        <div class="summary-item replaced">
          <span class="summary-icon"><CircleCheck /></span>
          <div class="summary-content">
            <span class="summary-value">{{ calendarData.replacedCount || 0 }}</span>
            <span class="summary-label">已经更换</span>
          </div>
        </div>
        <div class="summary-item severe">
          <span class="summary-icon"><CircleClose /></span>
          <div class="summary-content">
            <span class="summary-value">{{ calendarData.severeCount || 0 }}</span>
            <span class="summary-label">严重损耗</span>
          </div>
        </div>
      </div>

      <div class="calendar-grid" v-loading="loading">
        <div class="calendar-weekdays">
          <div class="weekday" v-for="day in weekDays" :key="day">{{ day }}</div>
        </div>
        <div class="calendar-days">
          <div
            v-for="(day, index) in calendarDays"
            :key="index"
            class="calendar-day"
            :class="{
              'is-today': day.isToday,
              'is-other-month': day.isOtherMonth,
              'has-expected': day.hasExpected,
              'has-replaced': day.hasReplaced,
              'has-severe': day.hasSevere,
              'is-selected': day.isSelected,
              'has-data': day.hasData
            }"
            @click="handleDayClick(day)"
          >
            <span class="day-number">{{ day.date }}</span>
            <div class="day-markers" v-if="day.hasData">
              <span class="marker expected" v-if="day.hasExpected"></span>
              <span class="marker replaced" v-if="day.hasReplaced"></span>
              <span class="marker severe" v-if="day.hasSevere"></span>
            </div>
            <div class="day-badge" v-if="day.accessoryCount > 0">
              {{ day.accessoryCount }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedDateTitle"
      width="600px"
      destroy-on-close
    >
      <div v-if="selectedDayData && selectedDayData.accessories && selectedDayData.accessories.length > 0">
        <div class="detail-list">
          <div
            v-for="(item, index) in selectedDayData.accessories"
            :key="index"
            class="detail-item"
          >
            <div class="detail-header">
              <div class="detail-name">{{ item.name }}</div>
              <el-tag :type="getStatusTagType(item.status)" size="small">
                {{ item.statusLabel }}
              </el-tag>
            </div>
            <div class="detail-info">
              <div class="info-row">
                <span class="info-label">适配乐器</span>
                <span class="info-value">{{ item.instrumentName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">规格</span>
                <span class="info-value">{{ item.specification }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">处理状态</span>
                <span class="info-value status-text" :class="item.status">
                  {{ getStatusText(item.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="当天暂无配件记录" />

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Warning, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { accessoryApi } from '@/api'

const loading = ref(false)
const currentYear = ref(dayjs().year())
const currentMonth = ref(dayjs().month() + 1)
const calendarDays = ref([])
const calendarData = ref({})
const selectedDate = ref(null)
const selectedDayData = ref(null)
const detailDialogVisible = ref(false)
const accessoryList = ref([])

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const selectedDateTitle = computed(() => {
  if (!selectedDate.value) return ''
  const date = dayjs(selectedDate.value)
  const weekDay = weekDays[date.day()]
  return `${date.format('YYYY年MM月DD日')} 星期${weekDay}`
})

const getStatusTagType = (status) => {
  switch (status) {
    case 'expected': return 'warning'
    case 'replaced': return 'success'
    case 'severe':
    case 'broken': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'expected': return '待更换'
    case 'replaced': return '已完成'
    case 'severe':
    case 'broken': return '待处理'
    default: return '未知'
  }
}

const loadAccessories = async () => {
  try {
    const res = await accessoryApi.list()
    accessoryList.value = res.data || res || []
  } catch {
    accessoryList.value = [
      { id: 1, name: '木吉他琴弦', specification: '012-053 磷铜覆膜', instrumentName: '木吉他', standardCycle: 90, imageUrl: '', purchaseDate: '2025-12-01', wornStatus: 'good' },
      { id: 2, name: '小提琴松香', specification: '无尘轻型 4/4', instrumentName: '小提琴', standardCycle: 180, imageUrl: '', purchaseDate: '2025-11-15', wornStatus: 'slight' },
      { id: 3, name: '电吉他拨片', specification: '0.88mm 尼龙防滑', instrumentName: '电吉他', standardCycle: 60, imageUrl: '', purchaseDate: '2026-01-01', wornStatus: 'good' },
      { id: 4, name: '小提琴琴弓', specification: '4/4 巴西木 八角弓', instrumentName: '小提琴', standardCycle: 365, imageUrl: '', purchaseDate: '2025-06-01', wornStatus: 'severe' },
      { id: 5, name: '吉他变调夹', specification: '弹簧式 金属款', instrumentName: '木吉他', standardCycle: 730, imageUrl: '', purchaseDate: '2024-01-15', wornStatus: 'good' },
      { id: 6, name: '指板清洁剂', specification: '柠檬油 100ml', instrumentName: '木吉他', standardCycle: 180, imageUrl: '', purchaseDate: '2025-09-20', wornStatus: 'good' },
      { id: 7, name: '贝斯琴弦', specification: '045-105 镍钢缠丝', instrumentName: '贝斯', standardCycle: 120, imageUrl: '', purchaseDate: '2026-02-01', wornStatus: 'broken' }
    ]
  }
}

const loadCalendar = async () => {
  loading.value = true
  try {
    const res = await accessoryApi.getCalendarMonth({
      year: currentYear.value,
      month: currentMonth.value
    })
    if (res && res.data) {
      calendarData.value = res.data
      buildCalendarDays(res.data)
    } else {
      loadMockCalendar()
    }
  } catch {
    loadMockCalendar()
  } finally {
    loading.value = false
  }
}

const loadMockCalendar = () => {
  const today = dayjs()
  const baseDate = dayjs(new Date(currentYear.value, currentMonth.value - 1, 15))
  const mockDays = {}
  
  const mockRecords = [
    { offset: -5, status: 'replaced', name: '木吉他琴弦', spec: '012-053 磷铜覆膜', instrument: '木吉他' },
    { offset: -3, status: 'replaced', name: '电吉他拨片', spec: '0.88mm 尼龙防滑', instrument: '电吉他' },
    { offset: 0, status: 'severe', name: '小提琴琴弓', spec: '4/4 巴西木 八角弓', instrument: '小提琴' },
    { offset: 0, status: 'broken', name: '贝斯琴弦', spec: '045-105 镍钢缠丝', instrument: '贝斯' },
    { offset: 3, status: 'expected', name: '小提琴松香', spec: '无尘轻型 4/4', instrument: '小提琴' },
    { offset: 7, status: 'expected', name: '木吉他琴弦', spec: '012-053 磷铜覆膜', instrument: '木吉他' },
    { offset: 10, status: 'expected', name: '电吉他拨片', spec: '0.88mm 尼龙防滑', instrument: '电吉他' },
    { offset: 15, status: 'expected', name: '指板清洁剂', spec: '柠檬油 100ml', instrument: '木吉他' }
  ]

  let expectedCount = 0
  let replacedCount = 0
  let severeCount = 0

  mockRecords.forEach(r => {
    const recordDate = baseDate.add(r.offset, 'day')
    const dateStr = recordDate.format('YYYY-MM-DD')
    
    if (recordDate.month() + 1 !== currentMonth.value || recordDate.year() !== currentYear.value) {
      return
    }
    
    if (!mockDays[dateStr]) {
      mockDays[dateStr] = {
        date: dateStr,
        hasExpected: false,
        hasReplaced: false,
        hasSevere: false,
        accessories: []
      }
    }
    const day = mockDays[dateStr]
    if (r.status === 'expected') {
      day.hasExpected = true
      expectedCount++
    }
    if (r.status === 'replaced') {
      day.hasReplaced = true
      replacedCount++
    }
    if (r.status === 'severe' || r.status === 'broken') {
      day.hasSevere = true
      severeCount++
    }
    day.accessories.push({
      accessoryId: Math.floor(Math.random() * 100),
      name: r.name,
      specification: r.spec,
      instrumentName: r.instrument,
      status: r.status,
      statusLabel: r.status === 'expected' ? '预计到期' : r.status === 'replaced' ? '已更换' : r.status === 'severe' ? '严重损耗' : '已损坏'
    })
  })

  calendarData.value = {
    year: currentYear.value,
    month: currentMonth.value,
    dayMap: mockDays,
    days: Object.values(mockDays),
    expectedCount: expectedCount,
    replacedCount: replacedCount,
    severeCount: severeCount
  }

  buildCalendarDays(calendarData.value)
}

const buildCalendarDays = (data) => {
  const year = currentYear.value
  const month = currentMonth.value - 1
  const firstDay = dayjs(new Date(year, month, 1))
  const lastDay = dayjs(new Date(year, month + 1, 0))
  const startDay = firstDay.day()
  const daysInMonth = lastDay.date()
  const today = dayjs()

  const days = []
  const dayMap = data.dayMap || {}

  const prevMonthLastDay = dayjs(new Date(year, month, 0)).date()
  for (let i = startDay - 1; i >= 0; i--) {
    const date = dayjs(new Date(year, month, prevMonthLastDay - i))
    days.push({
      date: prevMonthLastDay - i,
      fullDate: date.format('YYYY-MM-DD'),
      isOtherMonth: true,
      isToday: false,
      hasExpected: false,
      hasReplaced: false,
      hasSevere: false,
      hasData: false,
      accessoryCount: 0
    })
  }

  for (let i = 1; i <= daysInMonth; i++) {
    const date = dayjs(new Date(year, month, i))
    const dateStr = date.format('YYYY-MM-DD')
    const dayData = dayMap[dateStr] || null
    const isToday = date.isSame(today, 'day')

    days.push({
      date: i,
      fullDate: dateStr,
      isOtherMonth: false,
      isToday: isToday,
      hasExpected: dayData?.hasExpected || false,
      hasReplaced: dayData?.hasReplaced || false,
      hasSevere: dayData?.hasSevere || false,
      hasData: !!dayData && dayData.accessories && dayData.accessories.length > 0,
      accessoryCount: dayData?.accessories?.length || 0
    })
  }

  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    const date = dayjs(new Date(year, month + 1, i))
    days.push({
      date: i,
      fullDate: date.format('YYYY-MM-DD'),
      isOtherMonth: true,
      isToday: false,
      hasExpected: false,
      hasReplaced: false,
      hasSevere: false,
      hasData: false,
      accessoryCount: 0
    })
  }

  calendarDays.value = days
}

const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  loadCalendar()
}

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  loadCalendar()
}

const goToToday = () => {
  currentYear.value = dayjs().year()
  currentMonth.value = dayjs().month() + 1
  loadCalendar()
}

const handleDayClick = async (day) => {
  if (day.isOtherMonth) return
  if (!day.hasData) {
    selectedDate.value = day.fullDate
    selectedDayData.value = { accessories: [] }
    detailDialogVisible.value = true
    return
  }

  selectedDate.value = day.fullDate

  try {
    const res = await accessoryApi.getCalendarDay({ date: day.fullDate })
    if (res && res.data && res.data.accessories) {
      selectedDayData.value = res.data
    } else {
      const dayData = calendarData.value.dayMap?.[day.fullDate]
      selectedDayData.value = dayData || { accessories: [] }
    }
  } catch {
    const dayData = calendarData.value.dayMap?.[day.fullDate]
    selectedDayData.value = dayData || { accessories: [] }
  }

  detailDialogVisible.value = true
}

onMounted(() => {
  loadAccessories().then(() => {
    loadCalendar()
  })
})
</script>

<style lang="scss" scoped>
.legend-bar {
  display: flex;
  align-items: center;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;

  &.expected {
    background: #e6a23c;
  }
  &.replaced {
    background: #67c23a;
  }
  &.severe {
    background: #f56c6c;
  }
}

.legend-text {
  font-size: 13px;
  color: #606266;
}

.calendar-card {
  padding: 24px;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.calendar-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 20px;
  min-width: 140px;
  text-align: center;
}

.calendar-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.summary-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #ebeef5;

  &.expected {
    background: linear-gradient(135deg, #fdf6ec 0%, #faecd8 100%);
    border-color: #f5dab1;

    .summary-icon {
      background: #e6a23c;
    }
    .summary-value {
      color: #e6a23c;
    }
  }

  &.replaced {
    background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
    border-color: #c2e7b0;

    .summary-icon {
      background: #67c23a;
    }
    .summary-value {
      color: #67c23a;
    }
  }

  &.severe {
    background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
    border-color: #fbc4c4;

    .summary-icon {
      background: #f56c6c;
    }
    .summary-value {
      color: #f56c6c;
    }
  }
}

.summary-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.summary-content {
  display: flex;
  flex-direction: column;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.calendar-grid {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.weekday {
  padding: 12px 0;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-day {
  position: relative;
  aspect-ratio: 1;
  min-height: 80px;
  padding: 8px;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;

  &:nth-child(7n) {
    border-right: none;
  }

  &:nth-last-child(-n+7) {
    border-bottom: none;
  }

  &:hover {
    background: #f5f7fa;
  }

  &.is-other-month {
    background: #fafafa;
    cursor: default;
    opacity: 0.4;

    &:hover {
      background: #fafafa;
    }

    .day-number {
      color: #c0c4cc;
    }
  }

  &.is-today {
    background: #ecf5ff;

    .day-number {
      background: #409eff;
      color: #fff;
      font-weight: 600;
    }
  }

  &.is-selected {
    background: #d9ecff;
  }

  &.has-expected:not(.is-today):not(.is-other-month) {
    background: linear-gradient(135deg, #fdf6ec 0%, transparent 70%);
  }

  &.has-replaced:not(.is-today):not(.is-other-month) {
    background: linear-gradient(135deg, #f0f9eb 0%, transparent 70%);
  }

  &.has-severe:not(.is-today):not(.is-other-month) {
    background: linear-gradient(135deg, #fef0f0 0%, transparent 70%);
  }

  &.has-expected.has-replaced:not(.is-today):not(.is-other-month) {
    background: linear-gradient(135deg, #fef0f0 0%, #fdf6ec 50%, #f0f9eb 100%);
  }

  &.has-data:not(.is-other-month):hover {
    transform: scale(1.02);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    z-index: 1;
  }
}

.day-number {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
}

.day-markers {
  display: flex;
  gap: 3px;
  margin-top: 2px;
}

.marker {
  width: 6px;
  height: 6px;
  border-radius: 50%;

  &.expected {
    background: #e6a23c;
  }
  &.replaced {
    background: #67c23a;
  }
  &.severe {
    background: #f56c6c;
  }
}

.day-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  border-radius: 9px;
  text-align: center;
  font-weight: 600;
}

.detail-list {
  max-height: 400px;
  overflow-y: auto;
}

.detail-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.detail-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.info-label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}

.info-value {
  color: #606266;
}

.status-text {
  font-weight: 600;

  &.expected {
    color: #e6a23c;
  }
  &.replaced {
    color: #67c23a;
  }
  &.severe,
  &.broken {
    color: #f56c6c;
  }
}

@media (max-width: 768px) {
  .calendar-summary {
    flex-direction: column;
  }

  .calendar-day {
    min-height: 60px;
  }

  .day-number {
    width: 24px;
    height: 24px;
    line-height: 24px;
    font-size: 12px;
  }

  .legend-bar {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
