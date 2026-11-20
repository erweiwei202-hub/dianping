<script setup lang="ts">
import { ref, watch } from 'vue'
import { ApiError } from '../services/api'
import { useSessionStore } from '../stores/session'

const session = useSessionStore()
const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const displayName = ref('')
const errorMessage = ref('')
const submitting = ref(false)

watch(() => session.authDialogOpen, (open) => {
  if (open) errorMessage.value = ''
})

async function submit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    if (mode.value === 'register') await session.register(username.value.trim(), password.value, displayName.value.trim())
    else await session.login(username.value.trim(), password.value)
    username.value = ''
    password.value = ''
    displayName.value = ''
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '暂时无法连接服务，请稍后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="session.authDialogOpen" class="auth-backdrop" @click.self="session.closeAuthDialog()">
    <section class="auth-card" role="dialog" aria-modal="true" aria-labelledby="auth-title">
      <button class="auth-close" type="button" aria-label="关闭登录窗口" @click="session.closeAuthDialog()">×</button>
      <div class="auth-logo">享</div>
      <p class="auth-kicker">WELCOME TO YOUR CITY</p>
      <h2 id="auth-title">{{ mode === 'login' ? '欢迎回来' : '加入智享生活' }}</h2>
      <p class="auth-intro">{{ mode === 'login' ? '登录后评价喜欢的商户，给有帮助的分享点个赞。' : '创建账号，分享体验，也发现身边好店。' }}</p>
      <form class="auth-form" @submit.prevent="submit">
        <label for="auth-username">用户名</label>
        <input id="auth-username" v-model="username" name="username" autocomplete="username" minlength="3" maxlength="40" required placeholder="请输入用户名" />
        <label v-if="mode === 'register'" for="auth-display-name">显示名称</label>
        <input v-if="mode === 'register'" id="auth-display-name" v-model="displayName" name="displayName" autocomplete="nickname" minlength="1" maxlength="40" required placeholder="其他用户会看到这个名称" />
        <label for="auth-password">密码</label>
        <input id="auth-password" v-model="password" name="password" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" minlength="8" maxlength="72" required type="password" placeholder="请输入密码" />
        <p v-if="errorMessage" class="auth-error" role="alert">{{ errorMessage }}</p>
        <button class="auth-submit" type="submit" :disabled="submitting">
          {{ submitting ? '请稍候…' : mode === 'login' ? '登录' : '创建账号' }}
        </button>
      </form>
      <p class="auth-switch">
        {{ mode === 'login' ? '还没有账号？' : '已经有账号？' }}
        <button type="button" @click="mode = mode === 'login' ? 'register' : 'login'; errorMessage = ''">{{ mode === 'login' ? '注册' : '返回登录' }}</button>
      </p>
      <p class="auth-security">账户信息通过安全连接提交。密码不会保存在浏览器本地。</p>
    </section>
  </div>
</template>

<style scoped>
.auth-backdrop{position:fixed;inset:0;z-index:60;display:grid;place-items:center;padding:20px;background:rgba(24,35,41,.48);backdrop-filter:blur(5px)}
.auth-card{position:relative;width:min(410px,100%);padding:32px;border:1px solid #edf0f1;border-radius:20px;background:#fff;box-shadow:0 26px 80px rgba(25,39,47,.2)}
.auth-close{position:absolute;right:17px;top:13px;border:0;color:#87939a;background:transparent;font-size:25px}
.auth-logo{width:42px;height:42px;display:grid;place-items:center;border-radius:14px;color:#fff;background:#e85d4a;font-size:20px;font-weight:800}
.auth-kicker{margin:18px 0 6px;color:#d65a48;font-size:9px;font-weight:800;letter-spacing:1.7px}
.auth-card h2{margin:0;color:#25333b;font-size:23px;letter-spacing:-.4px}
.auth-intro{margin:8px 0 21px;color:#849098;font-size:11px;line-height:1.7}
.auth-form{display:grid;gap:8px}.auth-form label{margin-top:5px;color:#58666e;font-size:10px;font-weight:700}
.auth-form input{height:42px;padding:0 12px;border:1px solid #e2e7e9;border-radius:9px;outline:none;color:#26343c;font-size:12px}
.auth-form input:focus{border-color:#e58f80;box-shadow:0 0 0 3px rgba(232,93,74,.1)}
.auth-submit{height:43px;margin-top:8px;border:0;border-radius:9px;color:white;background:#e85d4a;font-size:12px;font-weight:700}
.auth-submit:hover{background:#cf4938}.auth-submit:disabled{opacity:.6;cursor:wait}
.auth-error{margin:3px 0 0;padding:9px 10px;border-radius:7px;color:#ac453a;background:#fff0ed;font-size:10px;line-height:1.5}
.auth-switch{margin:17px 0 0;text-align:center;color:#7d8990;font-size:10px}.auth-switch button{border:0;color:#d55343;background:transparent;font-weight:700}
.auth-security{margin:17px 0 0;padding-top:13px;border-top:1px solid #eff1f2;text-align:center;color:#a0a9ae;font-size:9px}
@media(max-width:480px){.auth-card{padding:25px 21px;border-radius:16px}}
</style>
