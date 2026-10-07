import request from './request'

export default {
  page: (current, size, status) => request.get('/change-room/page', { params: { current, size, status } }),
  add: (data) => request.post('/change-room', data),
  approve: (id) => request.put('/change-room/' + id + '/approve'),
  reject: (id, reply) => request.put('/change-room/' + id + '/reject', { reply }),
}
