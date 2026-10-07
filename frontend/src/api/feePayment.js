import request from './request'

export default {
  page: (current, size, params) => request.get('/fee-payment/page', { params: { current, size, ...(params || {}) } }),
  summary: (month) => request.get('/fee-payment/summary', { params: { month } }),
  byFee: (feeId) => request.get('/fee-payment/by-fee/' + feeId),
  list: () => request.get('/fee-payment/list'),
  add: (data) => request.post('/fee-payment', data),
  remove: (id) => request.delete('/fee-payment/' + id)
}
