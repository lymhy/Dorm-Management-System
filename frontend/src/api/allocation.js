import request from './request'

export default {
  page: (current, size) => request.get('/allocation/page', { params: { current, size } }),
  list: (params) => request.get('/allocation/list'),
  add: (data) => request.post('/allocation', data),
  update: (data) => request.put('/allocation', data),
  remove: (id) => request.delete('/allocation/' + id),
}
