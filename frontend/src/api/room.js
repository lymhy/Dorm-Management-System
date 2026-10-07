import request from './request'

export default {
  page: (current, size, buildingId, floor, status) =>
    request.get('/room/page', { params: { current, size, buildingId, floor, status } }),
  list: () => request.get('/room/list'),
  add: (data) => request.post('/room', data),
  update: (data) => request.put('/room', data),
  remove: (id) => request.delete('/room/' + id),
}
