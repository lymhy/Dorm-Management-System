import request from './request'

export default {
  page: (current, size, keyword) =>
    request.get('/building/page', { params: { current, size, keyword } }),
  list: () => request.get('/building/list'),
  add: (data) => request.post('/building', data),
  update: (data) => request.put('/building', data),
  remove: (id) => request.delete('/building/' + id),
}
