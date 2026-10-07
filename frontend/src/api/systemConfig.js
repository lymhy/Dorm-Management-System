import request from './request'

export default {
  list: () => request.get('/system-config/list'),
  get: (key) => request.get('/system-config', { params: { key } }),
  update: (data) => request.put('/system-config', data),
}
