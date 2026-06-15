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
  listLifecycle: (params) => request.get('/accessory/lifecycle', { params })
}

export const replacementApi = {
  page: (params) => request.get('/replacement/page', { params }),
  list: (params) => request.get('/replacement/list', { params }),
  getById: (id) => request.get(`/replacement/${id}`),
  add: (data) => request.post('/replacement', data),
  update: (data) => request.put('/replacement', data),
  remove: (ids) => request.delete('/replacement', { data: { ids } }),
  history: (accessoryId) => request.get(`/replacement/history/${accessoryId}`)
}

export const groupApi = {
  tree: () => request.get('/group/tree'),
  list: () => request.get('/group/list'),
  getById: (id) => request.get(`/group/${id}`),
  add: (data) => request.post('/group', data),
  update: (data) => request.put('/group', data),
  remove: (id) => request.delete(`/group/${id}`)
}

export const dictApi = {
  accessoryTypes: () => request.get('/dict/accessory-types'),
  instruments: () => request.get('/dict/instruments'),
  wornStatuses: () => request.get('/dict/worn-statuses'),
  replacementCycles: () => request.get('/dict/replacement-cycles')
}

export const dashboardApi = {
  stats: () => request.get('/dashboard/stats'),
  upcomingReplacements: () => request.get('/dashboard/upcoming-replacements'),
  wornDistribution: () => request.get('/dashboard/worn-distribution'),
  groupDistribution: () => request.get('/dashboard/group-distribution'),
  riskTierList: (tier) => request.get(`/dashboard/risk-tier/${tier}`),
  riskTiers: () => request.get('/dashboard/risk-tiers')
}
