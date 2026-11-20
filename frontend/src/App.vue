<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
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
        <button class="avatar-button" type="button" aria-label="登录或个人中心">林</button>
      </div>
    </header>
    <RouterView />
    <footer v-if="!isAdmin" class="site-footer">
      <div class="footer-brand"><span class="brand-mark small">享</span><strong>智享生活</strong></div>
      <span>发现附近好店，把每一次消费都变成好体验。</span>
      <span class="footer-note">界面预览 · 数据尚未连接服务端</span>
    </footer>
  </div>
</template>
