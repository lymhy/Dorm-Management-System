import request from './request'

export default {
  stats: (params) => request.get('/dashboard/stats'),
}
