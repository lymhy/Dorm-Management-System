import request from './request'

export default {
  page: (current, size, keyword, status, dept, gender) =>
    request.get('/employee/page', { params: { current, size, keyword, status, dept, gender } }),
  depts: () => request.get('/employee/depts'),
  list: () => request.get('/employee/list'),
  add: (data) => request.post('/employee', data),
  update: (data) => request.put('/employee', data),
  remove: (id) => request.delete('/employee/' + id),
}
