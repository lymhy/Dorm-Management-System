import request from './request'

export default {
  login: (data) => request.post('/auth/login', data),
  page: (current, size, keyword, role, status) =>
    request.get('/user/page', { params: { current, size, keyword, role, status } }),
  add: (data) => request.post('/user', data),
  update: (data) => request.put('/user', data),
  remove: (id) => request.delete('/user/' + id),
  resetPassword: (id, password) => request.put('/user/' + id + '/reset-password', { password }),
  setStatus: (id, status) => request.put('/user/' + id + '/status', { status }),
  importUsers: (rows) => request.post('/user/import', rows)
}
