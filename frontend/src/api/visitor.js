import request from './request'

export default {
  page: (current, size) => request.get('/visitor/page', { params: { current, size } }),
  list: (params) => request.get('/visitor/list'),
  add: (data) => request.post('/visitor', data),
  update: (data) => request.put('/visitor', data),
  remove: (id) => request.delete('/visitor/' + id),
}
