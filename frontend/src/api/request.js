import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// Axios instance with base URL and Bearer-token injection.
const service = axios.create({ baseURL: '/api', timeout: 10000 })

// 后端未就绪时页面会并发失败多个请求，这里做节流，避免同一原因弹出一串提示。
let lastOfflineWarnAt = 0

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

service.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || 'error')
      if (data.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (err) => {
    // err.response 为空说明请求没拿到任何响应：后端未启动 / 已停止 / 超时。
    // 这种情况提示"后端未就绪"，而不是含糊的 network error。
    const now = Date.now()
    if (now - lastOfflineWarnAt > 3000) {
      lastOfflineWarnAt = now
      ElMessage.error(err.response ? (err.response.data?.message || '请求失败') : '无法连接后端服务，请确认后端已启动（8080）')
    }
    return Promise.reject(err)
  }
)

export default service
