import request from './request'

export default {
  me: () => request.get('/employee/me'),
  myAllocation: () => request.get('/employee/my-allocation'),
  feePage: (current, size) => request.get('/fee/my', { params: { current, size } }),
  repairPage: (current, size) => request.get('/repair/my', { params: { current, size } }),
  visitorPage: (current, size) => request.get('/visitor/my', { params: { current, size } }),
  changePassword: (oldPassword, newPassword) => request.put('/auth/password', { oldPassword, newPassword }),
}
