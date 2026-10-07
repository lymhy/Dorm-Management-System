import request from './request'

export default {
  page: (current, size, params) => request.get('/fee/page', { params: { current, size, ...(params || {}) } }),
  summary: (month) => request.get('/fee/summary', { params: { month } }),
  report: (month) => request.get('/fee/report', { params: { month } }),
  list: () => request.get('/fee/list'),
  add: (data) => request.post('/fee', data),
  update: (data) => request.put('/fee', data),
  remove: (id) => request.delete('/fee/' + id),
  markPaid: (id, paid) => request.put('/fee/' + id + '/paid', { paid }),
  batchPaid: (ids, paid) => request.put('/fee/batch-paid', { ids, paid }),
  batchRemove: (ids) => request.delete('/fee/batch', { data: { ids } }),
  urge: (ids, month) => request.post('/fee/urge', { ids: ids || [], month })
}
