import { defineStore } from 'pinia'

// Simple user store holding token and basic info.
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('uname') || '',
    realName: localStorage.getItem('rname') || '',
    role: localStorage.getItem('role') || ''
  }),
  actions: {
    setToken(token) { this.token = token; localStorage.setItem('token', token) },
    setInfo(info) {
      this.username = info.username
      this.realName = info.realName
      this.role = info.role
      localStorage.setItem('uname', info.username)
      localStorage.setItem('rname', info.realName)
      localStorage.setItem('role', info.role)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      localStorage.removeItem('token')
      localStorage.removeItem('uname')
      localStorage.removeItem('rname')
      localStorage.removeItem('role')
    }
  }
})
