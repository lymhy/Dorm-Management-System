import request from './request'

export default {
  page: (current, size) => request.get('/announcement/page', { params: { current, size } }),
  latest: () => request.get('/announcement/latest'),
  get: (id) => request.get('/announcement/' + id),
  add: (data) => request.post('/announcement', data),
  update: (data) => request.put('/announcement', data),
  remove: (id) => request.delete('/announcement/' + id),
}
