import request from './request'

export default {
  page: (current, size) => request.get('/repair/page', { params: { current, size } }),
  list: (params) => request.get('/repair/list'),
  add: (data) => request.post('/repair', data),
  update: (data) => request.put('/repair', data),
  remove: (id) => request.delete('/repair/' + id),
}
