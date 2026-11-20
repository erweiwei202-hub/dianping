<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

const toast = ref('')
const activeNav = ref('经营概览')
const navItems = [
  { label: '经营概览', icon: '▦' }, { label: '商户资料', icon: '⌂' },
  { label: '评价管理', icon: '☆' }, { label: '优惠券活动', icon: '◇' },
  { label: '秒杀订单', icon: '◷' }, { label: '库存与对账', icon: '▤' },
]
const campaigns = [
  { name: '春日双人分享餐', merchant: '巷里·江南小馆', status: '进行中', sold: '—', inventory: '—' },
  { name: '手冲咖啡体验券', merchant: '慢山咖啡研究所', status: '即将开始', sold: '—', inventory: '—' },
  { name: '周末烧鸟分享券', merchant: '南风炭火烧鸟', status: '草稿', sold: '—', inventory: '—' },
]

function showPreview() {
  toast.value = `${activeNav.value}功能将在对应服务接口接入后启用`
  window.setTimeout(() => { toast.value = '' }, 2400)
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-brand"><span class="brand-mark">享</span><div><strong>智享生活</strong><small>商户工作台</small></div></div>
      <p class="admin-nav-title">WORKSPACE</p>
      <nav class="admin-nav" aria-label="运营管理">
        <button v-for="item in navItems" :key="item.label" type="button" :class="{ active: activeNav === item.label }" @click="activeNav = item.label; showPreview()"><span>{{ item.icon }}</span>{{ item.label }}</button>
      </nav>
      <div class="admin-sidebar-foot"><RouterLink to="/">← 返回消费者首页</RouterLink><p>商户服务 · 运营支持</p></div>
    </aside>
    <main class="admin-main">
      <header class="admin-topbar"><div class="breadcrumb">工作台　/　<strong>{{ activeNav }}</strong></div><div class="admin-user"><span class="review-avatar">运</span><span>运营账号</span><i>⌄</i></div></header>
      <div class="admin-content">
        <div class="admin-title-row"><div><p class="section-kicker">MERCHANT CONSOLE</p><h1>{{ activeNav }}</h1><p>管理商户服务与消费者体验。</p></div><button class="admin-date" type="button" @click="showPreview()">今日　⌄</button></div>
        <div class="admin-preview"><strong>预览模式</strong>　以下数据为占位展示；运营写入、评价审核及订单操作需要登录权限和后端接口。</div>
        <section class="stat-grid" aria-label="经营指标">
          <article class="stat-card"><div class="stat-card-top"><span>活动曝光</span><span class="stat-icon">◉</span></div><div class="stat-value">—</div><div class="stat-hint">接入数据后展示趋势</div></article>
          <article class="stat-card"><div class="stat-card-top"><span>秒杀订单</span><span class="stat-icon">◷</span></div><div class="stat-value">—</div><div class="stat-hint">按活动统计订单进度</div></article>
          <article class="stat-card"><div class="stat-card-top"><span>待处理评价</span><span class="stat-icon">☆</span></div><div class="stat-value">—</div><div class="stat-hint">及时回应消费者反馈</div></article>
          <article class="stat-card"><div class="stat-card-top"><span>商户评分</span><span class="stat-icon">✦</span></div><div class="stat-value">—</div><div class="stat-hint">基于已发布评价统计</div></article>
        </section>
        <div class="admin-columns">
          <section class="admin-panel">
            <div class="admin-panel-head"><div><h2>优惠券活动</h2><p>跟进活动状态与库存信息</p></div><button type="button" @click="showPreview()">＋ 新建活动</button></div>
            <div class="table-wrap"><table class="admin-table"><thead><tr><th>活动名称</th><th>商户</th><th>状态</th><th>已售 / 库存</th><th></th></tr></thead><tbody>
              <tr v-for="campaign in campaigns" :key="campaign.name"><td><strong>{{ campaign.name }}</strong></td><td>{{ campaign.merchant }}</td><td><span class="status-pill" :class="{ soon: campaign.status !== '进行中' }">{{ campaign.status }}</span></td><td>{{ campaign.sold }} / {{ campaign.inventory }}</td><td><button class="table-action" type="button" @click="showPreview()">查看</button></td></tr>
            </tbody></table></div>
          </section>
          <section class="admin-panel">
            <div class="admin-panel-head"><div><h2>服务动态</h2><p>近期需要关注的事项</p></div><button type="button" @click="showPreview()">查看全部</button></div>
            <div class="event-list">
              <div class="event-item"><span class="event-mark">☆</span><div><strong>评价审核</strong><small>待审核评价进入后端队列后显示</small></div><span class="event-time">—</span></div>
              <div class="event-item"><span class="event-mark">◇</span><div><strong>活动配置</strong><small>发布前确认库存与活动时间</small></div><span class="event-time">—</span></div>
              <div class="event-item"><span class="event-mark">▤</span><div><strong>库存对账</strong><small>检查 Redis 与数据库库存差异</small></div><span class="event-time">—</span></div>
            </div>
          </section>
        </div>
        <section class="admin-panel chart-panel"><div class="admin-panel-head"><div><h2>订单处理趋势</h2><p>展示异步处理完成的订单数量</p></div><span class="admin-subtitle">暂无实时数据</span></div><div class="activity-bars" aria-hidden="true"><i v-for="n in 14" :key="n" class="activity-bar" :style="{ height: `${30 + ((n * 31) % 65)}%` }"></i></div><div class="activity-labels"><span>周一</span><span>周二</span><span>周三</span><span>周四</span><span>周五</span><span>周六</span><span>周日</span></div></section>
      </div>
    </main>
    <div v-if="toast" class="toast" role="status">{{ toast }}</div>
  </div>
</template>
