import request from '@/utils/request'

export const accessoryApi = {
  page: (params) => request.get('/accessory/page', { params }),
  list: (params) => request.get('/accessory/list', { params }),
  getById: (id) => request.get(`/accessory/${id}`),
  add: (data) => request.post('/accessory', data),
  update: (data) => request.put('/accessory', data),
  remove: (ids) => request.delete('/accessory', { data: { ids } }),
  updateStatus: (id, status) => request.patch(`/accessory/${id}/status`, { status }),
  batchUpdateStatus: (ids, status) => request.patch('/accessory/batch-status', { ids, status }),
  getLifecycle: (id) => request.get(`/accessory/${id}/lifecycle`),
  listLifecycle: (params) => request.get('/accessory/lifecycle', { params }),
  getCycleReference: (params) => request.get('/accessory/cycle-reference', { params }),
  getCalendarMonth: (params) => request.get('/accessory/calendar/month', { params }),
  getCalendarDay: (params) => request.get('/accessory/calendar/day', { params }),
  getCycleRuleMatch: (params) => request.get('/accessory/cycle-rule-match', { params }),
  checkCompatibility: (params) => request.get('/accessory/compatibility-check', { params })
}

export const cycleRuleApi = {
  page: (params) => request.get('/cycle-rule/page', { params }),
  list: (params) => request.get('/cycle-rule/list', { params }),
  getById: (id) => request.get(`/cycle-rule/${id}`),
  add: (data) => request.post('/cycle-rule', data),
  update: (data) => request.put('/cycle-rule', data),
  remove: (ids) => request.delete('/cycle-rule', { data: { ids } }),
  match: (params) => request.get('/cycle-rule/match', { params }),
  getCandidates: (params) => request.get('/cycle-rule/candidates', { params })
}

export const replacementApi = {
  page: (params) => request.get('/replacement/page', { params }),
  list: (params) => request.get('/replacement/list', { params }),
  timeline: (params) => request.get('/replacement/timeline', { params }),
  getById: (id) => request.get(`/replacement/${id}`),
  add: (data) => request.post('/replacement', data),
  update: (data) => request.put('/replacement', data),
  remove: (ids) => request.delete('/replacement', { data: { ids } }),
  history: (accessoryId) => request.get(`/replacement/history/${accessoryId}`),
  recalculateByAccessory: (accessoryId, withStandardCycle = true) =>
    request.post(`/replacement/recalculate/accessory/${accessoryId}`, null, { params: { withStandardCycle } }),
  recalculateByAccessoryIds: (accessoryIds, withStandardCycle = true) =>
    request.post('/replacement/recalculate/accessories', { accessoryIds, withStandardCycle }),
  recalculateByCondition: (typeCode, instrument) =>
    request.post('/replacement/recalculate/condition', { typeCode, instrument }),
  recalculateAll: () => request.post('/replacement/recalculate/all')
}

export const groupApi = {
  tree: () => request.get('/group/tree'),
  list: () => request.get('/group/list'),
  getById: (id) => request.get(`/group/${id}`),
  add: (data) => request.post('/group', data),
  update: (data) => request.put('/group', data),
  remove: (id) => request.delete(`/group/${id}`),
  healthScore: (id) => request.get(`/group/${id}/health-score`),
  healthScores: () => request.get('/group/health-scores')
}

export const setApi = {
  page: (params) => request.get('/accessory-set/page', { params }),
  list: (params) => request.get('/accessory-set/list', { params }),
  getById: (id) => request.get(`/accessory-set/${id}`),
  add: (data) => request.post('/accessory-set', data),
  update: (data) => request.put('/accessory-set', data),
  remove: (ids) => request.delete('/accessory-set', { data: { ids } }),
  updateStatus: (id, status) => request.patch(`/accessory-set/${id}/status`, { status })
}

export const dictApi = {
  accessoryTypes: () => request.get('/dict/accessory-types'),
  instruments: () => request.get('/dict/instruments'),
  wornStatuses: () => request.get('/dict/worn-statuses'),
  replacementCycles: () => request.get('/dict/replacement-cycles')
}

export const wornStatusDictApi = {
  page: (params) => request.get('/worn-status-dict/page', { params }),
  list: () => request.get('/worn-status-dict/list'),
  listEnabled: () => request.get('/worn-status-dict/list-enabled'),
  getById: (id) => request.get(`/worn-status-dict/${id}`),
  getByCode: (code) => request.get(`/worn-status-dict/code/${code}`),
  add: (data) => request.post('/worn-status-dict', data),
  update: (data) => request.put('/worn-status-dict', data),
  remove: (ids) => request.delete('/worn-status-dict', { data: { ids } }),
  getUsage: (id) => request.get(`/worn-status-dict/usage/${id}`),
  toggleStatus: (id, enabled) => request.patch(`/worn-status-dict/${id}/status`, { enabled })
}

export const dashboardApi = {
  stats: () => request.get('/dashboard/stats'),
  upcomingReplacements: () => request.get('/dashboard/upcoming-replacements'),
  wornDistribution: () => request.get('/dashboard/worn-distribution'),
  groupDistribution: () => request.get('/dashboard/group-distribution'),
  riskTierList: (tier) => request.get(`/dashboard/risk-tier/${tier}`),
  riskTiers: () => request.get('/dashboard/risk-tiers'),
  wornHeatmap: () => request.get('/dashboard/worn-heatmap'),
  riskDistribution: () => request.get('/dashboard/risk-distribution'),
  annualStats: () => request.get('/dashboard/annual-stats')
}

export const maintenancePlanApi = {
  allPlans: () => request.get('/maintenance-plan/all'),
  planByInstrument: (instrument) => request.get(`/maintenance-plan/instrument/${instrument}`),
  planItems: (instrument) => request.get(`/maintenance-plan/items/${instrument}`)
}

export const preparationTemplateApi = {
  page: (params) => request.get('/preparation-template/page', { params }),
  list: (params) => request.get('/preparation-template/list', { params }),
  getById: (id) => request.get(`/preparation-template/${id}`),
  getByTypeCode: (typeCode) => request.get(`/preparation-template/by-type/${typeCode}`),
  add: (data) => request.post('/preparation-template', data),
  update: (data) => request.put('/preparation-template', data),
  remove: (ids) => request.delete('/preparation-template', { data: { ids } }),
  updateStatus: (id, enabled) => request.patch(`/preparation-template/${id}/status`, { enabled })
}

export const preparationChecklistApi = {
  page: (params) => request.get('/preparation-checklist/page', { params }),
  list: (params) => request.get('/preparation-checklist/list', { params }),
  getById: (id) => request.get(`/preparation-checklist/${id}`),
  getChecklistWithCategories: (id) => request.get(`/preparation-checklist/${id}/categories`),
  generate: (data) => request.post('/preparation-checklist/generate', data),
  startChecklist: (id, operator) => request.post(`/preparation-checklist/${id}/start`, { operator }),
  completeItem: (data) => request.post('/preparation-checklist/complete-item', data),
  completeChecklist: (id, operator) => request.post(`/preparation-checklist/${id}/complete`, { operator }),
  remove: (ids) => request.delete('/preparation-checklist', { data: { ids } }),
  linkReplacementRecord: (checklistId, replacementRecordId) =>
    request.post(`/preparation-checklist/${checklistId}/link-replacement/${replacementRecordId}`),
  getByReplacementRecordId: (replacementRecordId) =>
    request.get(`/preparation-checklist/by-replacement/${replacementRecordId}`),
  getByAccessoryId: (accessoryId) =>
    request.get(`/preparation-checklist/by-accessory/${accessoryId}`)
}
