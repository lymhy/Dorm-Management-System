import request from './request'

export default {
  page: (current, size, status) => request.get('/move-out/page', { params: { current, size, status } }),
  add: (data) => request.post('/move-out', data),
  approve: (id) => request.put('/move-out/' + id + '/approve'),
  reject: (id, reply) => request.put('/move-out/' + id + '/reject', { reply }),
}
