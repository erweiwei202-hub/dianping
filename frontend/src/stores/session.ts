import { defineStore } from 'pinia'
import { ApiError, authApi, type UserDto } from '../services/api'

export const useSessionStore = defineStore('session', {
  state: () => ({ user: null as UserDto | null, loading: false, authDialogOpen: false }),
  actions: {
    openAuthDialog() { this.authDialogOpen = true },
    closeAuthDialog() { this.authDialogOpen = false },
    async loadCurrentUser() {
      this.loading = true
      try {
        this.user = await authApi.me()
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) this.user = null
        else this.user = null
      } finally {
        this.loading = false
      }
    },
    async login(username: string, password: string) {
      this.user = await authApi.login(username, password)
      this.closeAuthDialog()
    },
    async register(username: string, password: string, displayName: string) {
      await authApi.register(username, password, displayName)
      this.user = await authApi.login(username, password)
      this.closeAuthDialog()
    },
    async logout() {
      await authApi.logout()
      this.user = null
    },
  },
})
