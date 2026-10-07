import request from './request'

export default {
  // 当前员工通知分页（isRead 可选：0未读 1已读）
  page: (current, size, isRead) =>
    request.get('/notification/my', { params: { current, size, isRead } }),
  // 未读数量（角标）
  unreadCount: () => request.get('/notification/unread-count'),
  // 单条已读 / 未读
  markRead: (id) => request.put('/notification/' + id + '/read'),
  markUnread: (id) => request.put('/notification/' + id + '/unread'),
  // 全部已读
  readAll: () => request.put('/notification/read-all'),
  // 删除
  remove: (id) => request.delete('/notification/' + id)
}
