<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import AuthDialog from './components/AuthDialog.vue'
import { useSessionStore } from './stores/session'

const route = useRoute()
const session = useSessionStore()
const isAdmin = computed(() => route.path.startsWith('/admin'))

onMounted(() => { void session.loadCurrentUser() })

async function logout() {
  try {
    await session.logout()
  } catch {
    session.user = null
  }
}
</script>

<template>
  <div class="app-shell" :class="{ 'admin-shell': isAdmin }">
    <header v-if="!isAdmin" class="site-header">
      <RouterLink class="brand" to="/" aria-label="智享生活首页">
        <span class="brand-mark">享</span>
        <span class="brand-copy"><strong>智享生活</strong><small>DISCOVER YOUR CITY</small></span>
      </RouterLink>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink to="/" class="nav-item" active-class="active">发现</RouterLink>
        <a class="nav-item" href="#deals">限时优惠</a>
        <a class="nav-item" href="#reviews">真实评价</a>
      </nav>
      <div class="header-actions">
        <RouterLink class="admin-link" to="/admin">商户管理</RouterLink>
        <button v-if="session.user" class="account-button" type="button" @click="logout">{{ session.user.displayName }} · 退出</button>
        <button v-else class="avatar-button" type="button" aria-label="登录或注册" @click="session.openAuthDialog()">登录</button>
      </div>
    </header>
    <RouterView />
    <footer v-if="!isAdmin" class="site-footer">
      <div class="footer-brand"><span class="brand-mark small">享</span><strong>智享生活</strong></div>
      <span>发现附近好店，把每一次消费都变成好体验。</span>
      <span class="footer-note">商户与评价读取服务端 · 优惠活动为界面预览</span>
    </footer>
    <AuthDialog />
  </div>
</template>

<style scoped>
.account-button{border:0;background:transparent;color:#59666e;font-size:12px;font-weight:600}
.account-button:hover{color:#e85d4a}
</style>
