<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, couponApi, walletApi, type CouponOrderDto, type WalletBalanceDto, type WalletLedgerItemDto } from '../services/api'
import { useSessionStore } from '../stores/session'

const session = useSessionStore()
const balance = ref<WalletBalanceDto | null>(null)
const balanceLoading = ref(false)
const balanceError = ref('')
const orders = ref<CouponOrderDto[]>([])
const ordersLoading = ref(false)
const ordersError = ref('')
const ordersPage = ref(0)
const ordersHasMore = ref(false)
const ledger = ref<WalletLedgerItemDto[]>([])
const ledgerLoading = ref(false)
const ledgerError = ref('')
const ledgerPage = ref(0)
const ledgerHasMore = ref(false)
const payingOrderId = ref<number | null>(null)
const paymentErrors = ref<Record<number, string>>({})
const refreshedAt = ref('')

function money(fen: number) {
  const amount = Number(fen || 0) / 100
  return `${amount < 0 ? '−' : ''}¥${Math.abs(amount).toFixed(2)}`
}

function dateTime(value: string | null | undefined) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('zh-CN', { year: 'numeric', month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function orderStatus(status: string) {
  const labels: Record<string, string> = { PENDING_PAYMENT: '待支付', PAID: '已支付', CLOSED: '已关闭', REDEEMED: '已核销' }
  return labels[status] ?? status
}

function ledgerType(type: string) {
  const labels: Record<string, string> = {
    RECHARGE_CREDIT: '余额充值', ORDER_DEBIT: '优惠订单支付', REFUND_CREDIT: '退款入账',
    ADMIN_CREDIT: '余额调增', ADMIN_DEBIT: '余额调减',
  }
  return labels[type] ?? type
}

function handleAuthError(error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    session.openAuthDialog()
    return true
  }
  return false
}

async function loadBalance() {
  balanceLoading.value = true
  balanceError.value = ''
  try {
    balance.value = await walletApi.balance()
  } catch (error) {
    if (!handleAuthError(error)) balanceError.value = error instanceof ApiError ? error.message : '暂时无法读取余额，请稍后重试。'
  } finally {
    balanceLoading.value = false
  }
}

async function loadOrders(append = false) {
  ordersLoading.value = true
  ordersError.value = ''
  const page = append ? ordersPage.value + 1 : 0
  try {
    const result = await couponApi.myOrders(page, 20)
    orders.value = append ? [...orders.value, ...result] : result
    ordersPage.value = page
    ordersHasMore.value = result.length === 20
  } catch (error) {
    if (!handleAuthError(error)) ordersError.value = error instanceof ApiError ? error.message : '暂时无法读取订单，请稍后重试。'
    if (!append) orders.value = []
  } finally {
    ordersLoading.value = false
  }
}

async function loadLedger(append = false) {
  ledgerLoading.value = true
  ledgerError.value = ''
  const page = append ? ledgerPage.value + 1 : 0
  try {
    const result = await walletApi.ledger(page, 20)
    ledger.value = append ? [...ledger.value, ...result.items] : result.items
    ledgerPage.value = result.page
    ledgerHasMore.value = result.page + 1 < result.totalPages
  } catch (error) {
    if (!handleAuthError(error)) ledgerError.value = error instanceof ApiError ? error.message : '暂时无法读取余额流水，请稍后重试。'
    if (!append) ledger.value = []
  } finally {
    ledgerLoading.value = false
  }
}

async function refreshAccount() {
  await Promise.all([loadBalance(), loadOrders(), loadLedger()])
  refreshedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function payOrder(order: CouponOrderDto) {
  if (payingOrderId.value !== null) return
  paymentErrors.value = { ...paymentErrors.value, [order.id]: '' }
  payingOrderId.value = order.id
  try {
    const updated = await couponApi.payWithBalance(order.id)
    if (updated.status === 'PAID') paymentErrors.value = { ...paymentErrors.value, [order.id]: '' }
    await Promise.all([loadBalance(), loadOrders(), loadLedger()])
  } catch (error) {
    if (!handleAuthError(error)) {
      const message = error instanceof ApiError && error.status === 402
        ? '余额不足，当前无法完成支付；充值功能尚未开放。'
        : error instanceof ApiError ? error.message : '支付请求失败，请稍后重试。'
      paymentErrors.value = { ...paymentErrors.value, [order.id]: message }
    }
  } finally {
    payingOrderId.value = null
  }
}

function startLogin() {
  session.openAuthDialog()
}

function clearPrivateData() {
  balance.value = null
  orders.value = []
  ledger.value = []
  balanceError.value = ''
  ordersError.value = ''
  ledgerError.value = ''
}

onMounted(() => {
  if (session.user) void refreshAccount()
})

watch(() => session.user?.id, (userId) => {
  if (userId) void refreshAccount()
  else clearPrivateData()
})
</script>

<template>
  <main class="account-page">
    <div class="account-topline"><RouterLink to="/" class="back-home">← 返回发现</RouterLink><span>ACCOUNT CENTER</span></div>
    <section class="account-heading"><div><p class="section-kicker">YOUR ACTIVITY</p><h1>账户与订单</h1><p>查看抢购订单、账户余额和余额变动记录。</p></div><button v-if="session.user" class="outline-button" type="button" :disabled="balanceLoading || ordersLoading || ledgerLoading" @click="refreshAccount">{{ balanceLoading || ordersLoading || ledgerLoading ? '正在刷新…' : '刷新数据' }}</button></section>

    <section v-if="!session.user" class="account-login-card">
      <div class="account-login-icon">享</div><h2>登录后查看你的账户</h2><p>订单、余额和账户流水仅对本人开放。</p><button class="primary-button" type="button" @click="startLogin">登录或注册</button>
    </section>

    <template v-else>
      <section class="account-welcome"><div><span class="account-avatar">{{ session.user.displayName.slice(0, 1) || '享' }}</span><div><strong>{{ session.user.displayName }}</strong><small>{{ refreshedAt ? `最近更新 ${refreshedAt}` : '账户信息' }}</small></div></div><RouterLink to="/admin" class="admin-preview-link">商户管理预览</RouterLink></section>

      <section class="wallet-card">
        <div class="wallet-card-heading"><div><p class="section-kicker">MY WALLET</p><h2>账户余额</h2></div><span class="wallet-live-label">服务端数据</span></div>
        <div v-if="balanceLoading && !balance" class="account-state">正在读取余额…</div>
        <div v-else-if="balanceError" class="account-error"><p>{{ balanceError }}</p><button class="text-link" type="button" @click="loadBalance">重试</button></div>
        <template v-else-if="balance">
          <div class="wallet-balance">{{ money(balance.availableBalanceFen) }}</div>
          <div class="wallet-subbalances"><span>可用余额</span><span>冻结金额 <strong>{{ money(balance.frozenBalanceFen) }}</strong></span></div>
        </template>
        <div class="recharge-note"><span>充值</span><strong>暂未开放</strong><small>后续可接入微信等支付渠道；当前不能在线充值。</small></div>
      </section>

      <section class="account-section">
        <div class="account-section-heading"><div><p class="section-kicker">MY ORDERS</p><h2>我的优惠订单</h2><p>新生成的抢购订单会显示在这里，待支付订单可用余额结算。</p></div><span class="account-count">{{ orders.length }} 笔</span></div>
        <div v-if="ordersLoading && orders.length === 0" class="account-state">正在读取订单…</div>
        <div v-else-if="ordersError" class="account-state account-error"><p>{{ ordersError }}</p><button class="text-link" type="button" @click="loadOrders()">重试</button></div>
        <div v-else-if="orders.length === 0" class="account-state">还没有优惠订单。<RouterLink to="/#deals">去看看当前活动</RouterLink></div>
        <div v-else class="order-list">
          <article v-for="order in orders" :key="order.id" class="order-card">
            <div class="order-main"><div class="order-title-row"><strong>优惠订单 #{{ order.id }}</strong><span class="order-status" :class="order.status.toLowerCase()">{{ orderStatus(order.status) }}</span></div><p>活动编号 {{ order.campaignId }} · 创建于 {{ dateTime(order.createdAt) }}</p><p v-if="order.paymentExpiresAt && order.status === 'PENDING_PAYMENT'">支付截止 {{ dateTime(order.paymentExpiresAt) }}</p></div>
            <div class="order-payment"><strong>{{ money(order.amountMinor) }}</strong><button v-if="order.status === 'PENDING_PAYMENT'" class="primary-button" type="button" :disabled="payingOrderId !== null" @click="payOrder(order)">{{ payingOrderId === order.id ? '支付处理中…' : '余额支付' }}</button><span v-else class="order-readonly">{{ orderStatus(order.status) }}</span></div>
            <p v-if="paymentErrors[order.id]" class="order-payment-error" role="alert">{{ paymentErrors[order.id] }}</p>
          </article>
          <div v-if="ordersHasMore" class="load-more-row"><button class="outline-button" type="button" :disabled="ordersLoading" @click="loadOrders(true)">{{ ordersLoading ? '正在加载…' : '加载更多订单' }}</button></div>
        </div>
      </section>

      <section class="account-section ledger-section">
        <div class="account-section-heading"><div><p class="section-kicker">WALLET ACTIVITY</p><h2>余额流水</h2><p>支付及余额变动明细按时间倒序排列。</p></div><span class="account-count">{{ ledger.length }} 条</span></div>
        <div v-if="ledgerLoading && ledger.length === 0" class="account-state">正在读取余额流水…</div>
        <div v-else-if="ledgerError" class="account-state account-error"><p>{{ ledgerError }}</p><button class="text-link" type="button" @click="loadLedger()">重试</button></div>
        <div v-else-if="ledger.length === 0" class="account-state">暂无余额流水。订单支付后，变动记录会显示在这里。</div>
        <div v-else class="ledger-list">
          <article v-for="item in ledger" :key="item.id" class="ledger-row">
            <div class="ledger-type-icon" :class="item.amountFen < 0 ? 'debit' : 'credit'">{{ item.amountFen < 0 ? '−' : '+' }}</div>
            <div class="ledger-description"><strong>{{ ledgerType(item.type) }}</strong><small>{{ item.description || `业务编号 ${item.businessId}` }}</small><small>{{ dateTime(item.createdAt) }}</small></div>
            <div class="ledger-amount"><strong :class="item.amountFen < 0 ? 'negative' : 'positive'">{{ money(item.amountFen) }}</strong><small>余额 {{ money(item.balanceAfterFen) }}</small></div>
          </article>
          <div v-if="ledgerHasMore" class="load-more-row"><button class="outline-button" type="button" :disabled="ledgerLoading" @click="loadLedger(true)">{{ ledgerLoading ? '正在加载…' : '加载更多流水' }}</button></div>
        </div>
      </section>
      <p class="account-disclaimer">订单与余额来自服务端。充值支付渠道尚未开放，页面不会展示模拟到账。</p>
    </template>
  </main>
</template>

<style scoped>
.account-page{width:min(980px,calc(100% - 48px));min-height:70vh;margin:0 auto;padding:35px 0 76px;color:#283740}.account-topline{display:flex;justify-content:space-between;align-items:center;color:#a1aaaf;font-size:9px;font-weight:800;letter-spacing:1.5px}.back-home{color:#74818a;font-size:11px;letter-spacing:0}.account-heading{display:flex;justify-content:space-between;align-items:end;gap:20px;margin:34px 0 22px}.account-heading h1{margin:0;font-size:29px;letter-spacing:-.7px}.account-heading p:not(.section-kicker){margin:8px 0 0;color:#879198;font-size:12px}.account-heading .outline-button{padding:10px 14px;border:1px solid #dfe5e8;border-radius:8px;color:#59666e;background:#fff;font-size:11px}.account-heading .outline-button:disabled{opacity:.6}.account-login-card{padding:55px 20px;text-align:center;border:1px solid #e8edef;border-radius:18px;background:#fff}.account-login-icon{width:46px;height:46px;margin:0 auto 15px;display:grid;place-items:center;border-radius:14px;color:#fff;background:#df725f;font-size:20px;font-weight:800}.account-login-card h2{margin:0;font-size:18px}.account-login-card p{margin:8px 0 20px;color:#8c979d;font-size:12px}.account-login-card .primary-button{padding:11px 22px}.account-welcome{display:flex;align-items:center;justify-content:space-between;padding:15px 18px;border:1px solid #edf0f1;border-radius:13px;background:#fff}.account-welcome>div{display:flex;align-items:center;gap:10px}.account-avatar{width:35px;height:35px;display:grid;place-items:center;border-radius:50%;color:#9a564a;background:#ffebe4;font-size:13px;font-weight:800}.account-welcome strong,.account-welcome small{display:block}.account-welcome strong{font-size:12px}.account-welcome small{margin-top:3px;color:#a0a9ae;font-size:9px}.admin-preview-link{color:#87929a;font-size:10px}.wallet-card{margin-top:15px;padding:22px 24px;border-radius:17px;color:#fff;background:linear-gradient(125deg,#283e42,#435f5a);box-shadow:0 14px 30px rgba(37,62,65,.11)}.wallet-card-heading{display:flex;justify-content:space-between;align-items:center}.wallet-card .section-kicker{color:#c5b79b}.wallet-card h2{margin:0;font-size:15px}.wallet-live-label{padding:5px 8px;border:1px solid rgba(255,255,255,.2);border-radius:20px;color:#d9e5dd;font-size:9px}.wallet-balance{margin:18px 0 4px;font-size:34px;font-weight:800;letter-spacing:-.7px}.wallet-subbalances{display:flex;justify-content:space-between;align-items:center;color:#c4d0cc;font-size:10px}.wallet-subbalances strong{margin-left:4px;color:#fff;font-size:12px}.recharge-note{display:grid;grid-template-columns:auto auto 1fr;align-items:center;gap:10px;margin-top:20px;padding:12px 13px;border-radius:10px;background:rgba(255,255,255,.09)}.recharge-note span{font-size:11px;font-weight:700}.recharge-note strong{color:#f1cf8b;font-size:10px}.recharge-note small{color:#bdcbc7;font-size:9px;text-align:right}.account-state{padding:23px;text-align:center;color:#8c979d;font-size:11px}.account-state a{margin-left:4px;color:#c75b4a}.account-error{color:#a95044}.account-error p{margin:0 0 8px}.wallet-card .account-state,.wallet-card .account-error{padding:20px 0;color:#f4c9bb;text-align:left}.wallet-card .text-link{padding:0;color:#f5d1c8}.account-section{margin-top:35px}.account-section-heading{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:13px}.account-section-heading h2{margin:0;font-size:18px}.account-section-heading p:not(.section-kicker){margin:6px 0 0;color:#8b969d;font-size:10px}.account-count{padding:5px 8px;border-radius:20px;color:#75828a;background:#f1f4f4;font-size:9px}.order-list,.ledger-list{display:grid;gap:9px}.order-card{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px;padding:16px 18px;border:1px solid #e9eef0;border-radius:13px;background:#fff}.order-title-row{display:flex;align-items:center;gap:9px}.order-title-row strong{font-size:12px}.order-status{padding:4px 7px;border-radius:20px;color:#98733a;background:#fff4dd;font-size:9px}.order-status.paid,.order-status.redeemed{color:#4e8060;background:#e9f5ed}.order-status.closed{color:#879198;background:#f1f3f4}.order-main p{margin:7px 0 0;color:#8d989e;font-size:10px}.order-payment{display:flex;align-items:center;gap:12px}.order-payment>strong{color:#bd5848;font-size:17px}.order-payment .primary-button{padding:9px 12px;font-size:10px}.order-payment .primary-button:disabled{opacity:.55}.order-readonly{color:#8f999e;font-size:10px}.order-payment-error{grid-column:1/-1;margin:0;color:#ad4b40;font-size:10px}.load-more-row{display:flex;justify-content:center;padding:9px}.load-more-row .outline-button{padding:8px 12px;border:1px solid #e1e7e9;border-radius:8px;color:#65737b;background:white;font-size:10px}.ledger-row{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:center;gap:11px;padding:13px 16px;border:1px solid #edf0f1;border-radius:11px;background:#fff}.ledger-type-icon{width:31px;height:31px;display:grid;place-items:center;border-radius:10px;font-size:15px;font-weight:800}.ledger-type-icon.debit{color:#b45c50;background:#fff0ec}.ledger-type-icon.credit{color:#4f8762;background:#e9f5eb}.ledger-description strong,.ledger-description small,.ledger-amount strong,.ledger-amount small{display:block}.ledger-description strong{font-size:11px}.ledger-description small{margin-top:4px;color:#929da3;font-size:9px}.ledger-amount{text-align:right}.ledger-amount strong{font-size:12px}.ledger-amount strong.negative{color:#bb554a}.ledger-amount strong.positive{color:#4c8060}.ledger-amount small{margin-top:5px;color:#929da3;font-size:9px}.account-disclaimer{margin-top:22px;color:#9aa4a9;font-size:9px;text-align:center}
@media(max-width:640px){.account-page{width:calc(100% - 30px);padding:24px 0 50px}.account-heading{align-items:start;margin:25px 0 17px}.account-heading h1{font-size:25px}.account-heading .outline-button{padding:8px;font-size:10px}.account-welcome{padding:12px}.wallet-card{padding:18px}.wallet-balance{font-size:30px}.recharge-note{grid-template-columns:auto auto;gap:7px}.recharge-note small{grid-column:1/-1;text-align:left}.account-section{margin-top:28px}.account-section-heading{align-items:start}.account-section-heading h2{font-size:16px}.account-section-heading p:not(.section-kicker){max-width:280px;line-height:1.5}.order-card{grid-template-columns:1fr;padding:14px}.order-payment{justify-content:space-between}.order-payment-error{grid-column:auto}.ledger-row{grid-template-columns:31px minmax(0,1fr) auto;gap:8px;padding:11px}.ledger-description small{line-height:1.4}.ledger-amount strong{font-size:11px}.account-login-card{padding:42px 16px}}
</style>
