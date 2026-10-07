import request from './request'

// 在线支付
export default {
  createOrder: (feeId, channel) => request.post('/pay/order', { feeId, channel: channel || 'mock' }),
  confirm: (orderNo, outTradeNo) => request.post('/pay/confirm', { orderNo, outTradeNo }),
  close: (orderNo, reason) => request.post('/pay/close', { orderNo, reason }),
  orderDetail: (orderNo) => request.get('/pay/order/' + orderNo),
  my: () => request.get('/pay/my'),
  page: (current, size, params) => request.get('/pay/page', { params: { current, size, ...(params || {}) } }),
}