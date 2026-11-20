<script setup lang="ts">
import { computed, ref } from 'vue'

type Merchant = { id: number; name: string; kind: string; rating: string; count: string; tags: string[]; distance: string; style: string }
type Review = { id: number; name: string; avatar: string; time: string; text: string; likes: number; rating: number }

const categories = [
  { label: '全部', icon: '⌂' }, { label: '美食', icon: '◉' }, { label: '咖啡茶饮', icon: '☕' },
  { label: '休闲娱乐', icon: '✦' }, { label: '丽人美发', icon: '✿' }, { label: '运动健身', icon: '⌁' }, { label: '生活服务', icon: '◇' },
]
const selectedCategory = ref('全部')
const query = ref('')
const submittedQuery = ref('')
const toast = ref('')
const liked = ref<number[]>([])
const reviewDialog = ref(false)
const reviewText = ref('')
const score = ref(5)

const merchants: Merchant[] = [
  { id: 1, name: '巷里·江南小馆', kind: '江浙菜 · 城东街道', rating: '4.8', count: '1,286', tags: ['招牌松鼠鱼', '环境雅致', '适合聚餐'], distance: '距你 1.2 km', style: 'photo-one' },
  { id: 2, name: '慢山咖啡研究所', kind: '咖啡 · 文创园区', rating: '4.9', count: '936', tags: ['手冲咖啡', '安静办公', '宠物友好'], distance: '距你 2.4 km', style: 'photo-two' },
  { id: 3, name: '南风炭火烧鸟', kind: '日式料理 · 湖滨商圈', rating: '4.7', count: '752', tags: ['炭火现烤', '下班小聚', '夜宵好去处'], distance: '距你 3.1 km', style: 'photo-three' },
]

const visibleMerchants = computed(() => merchants.filter((merchant) => {
  const matchesCategory = selectedCategory.value === '全部' || merchant.kind.includes(selectedCategory.value)
  const keyword = submittedQuery.value.trim().toLowerCase()
  const matchesQuery = !keyword || `${merchant.name} ${merchant.kind} ${merchant.tags.join(' ')}`.toLowerCase().includes(keyword)
  return matchesCategory && matchesQuery
}))

const reviews: Review[] = [
  { id: 11, name: '小满', avatar: '满', time: '今天 12:40', text: '第一次来就被松鼠鱼惊艳到，外酥里嫩，酸甜度刚刚好。店里很舒服，适合周末慢慢吃一顿。', likes: 28, rating: 5 },
  { id: 12, name: '阿哲同学', avatar: '哲', time: '昨天 18:16', text: '手冲风味很干净，店员会认真介绍豆子。窗边的位置特别适合一个人放空。', likes: 16, rating: 5 },
]

function notify(message: string) {
  toast.value = message
  window.setTimeout(() => { toast.value = '' }, 2400)
}

function search() {
  submittedQuery.value = query.value
}

function toggleLike(id: number) {
  liked.value = liked.value.includes(id) ? liked.value.filter((item) => item !== id) : [...liked.value, id]
  notify('当前为界面预览，登录并连接服务后才会保存点赞')
}

function submitReview() {
  reviewDialog.value = false
  reviewText.value = ''
  notify('评价表单预览完成，服务端接入后可正式提交')
}
</script>

<template>
  <main class="page-wrap">
    <div class="preview-banner"><span class="preview-dot"></span>产品界面预览 · 当前展示内容为示例数据</div>

    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">A BETTER WAY TO LIVE LOCAL</p>
        <h1>好店不必远，<em>好生活在身边。</em></h1>
        <p class="hero-desc">发现认真做事的本地商户，读真实评价，趁喜欢的优惠正好开始。</p>
        <form class="search-box" @submit.prevent="search">
          <span class="search-icon" aria-hidden="true">⌕</span>
          <input v-model="query" aria-label="搜索商户或服务" placeholder="搜店名、菜品、服务关键词" />
          <button type="submit">开始发现</button>
        </form>
      </div>
      <div class="hero-art" aria-hidden="true">
        <div class="hero-orbit one"></div><div class="hero-orbit two"></div>
        <div class="hero-sun"></div><div class="hero-plate"></div><div class="hero-leaf"></div>
        <span class="hero-spark a">✳</span><span class="hero-spark b">✦</span>
      </div>
    </section>

    <section class="section">
      <div class="section-heading">
        <div><p class="section-kicker">EXPLORE BY MOOD</p><h2>今天想体验什么？</h2><p>从附近开始，找到刚好适合你的地方。</p></div>
        <button class="text-link" type="button" @click="selectedCategory = '全部'">查看全部　↗</button>
      </div>
      <div class="category-row" role="group" aria-label="商户分类">
        <button v-for="category in categories" :key="category.label" class="category-chip" :class="{ selected: selectedCategory === category.label }" type="button" @click="selectedCategory = category.label">
          <span>{{ category.icon }}</span>{{ category.label }}
        </button>
      </div>
    </section>

    <section id="deals" class="section">
      <div class="section-heading">
        <div><p class="section-kicker">GOOD THINGS, RIGHT NOW</p><h2>正在发生的好优惠</h2><p>活动时间与库存以服务端实时信息为准。</p></div>
        <a class="text-link" href="#merchant-list">更多活动　↗</a>
      </div>
      <div class="deal-grid">
        <article class="deal-card"><span class="deal-label">限时秒杀</span><div class="deal-ghost"></div><h3>巷里双人春日套餐</h3><p>江南小馆 · 今日 18:00 开抢</p><div class="deal-price">¥ 88<small>起</small></div></article>
        <article class="deal-card"><span class="deal-label">新客礼遇</span><div class="deal-ghost"></div><h3>手冲咖啡体验券</h3><p>慢山咖啡 · 限量 120 份</p><div class="deal-price">¥ 25<small>起</small></div></article>
        <article class="deal-card"><span class="deal-label">周末精选</span><div class="deal-ghost"></div><h3>炭火烧鸟分享券</h3><p>南风烧鸟 · 周末可用</p><div class="deal-price">¥ 68<small>起</small></div></article>
      </div>
    </section>

    <section id="merchant-list" class="section">
      <div class="section-heading">
        <div><p class="section-kicker">LOCAL FAVORITES</p><h2>附近值得去的店</h2><p>评分来自消费者体验，看看大家为什么喜欢它。</p></div>
        <button class="text-link" type="button" @click="notify('地图视图将在后续版本接入')">地图找店　↗</button>
      </div>
      <div class="merchant-layout">
        <div class="merchant-list">
          <article v-for="merchant in visibleMerchants" :key="merchant.id" class="merchant-card">
            <div class="merchant-photo" :class="merchant.style" role="img" :aria-label="`${merchant.name}示意图`"></div>
            <div class="merchant-info">
              <h3>{{ merchant.name }}</h3><p class="subline">{{ merchant.kind }}</p>
              <div class="rating-line"><strong class="rating-score">{{ merchant.rating }}</strong><span class="stars">★★★★★</span><span class="review-count">{{ merchant.count }} 条评价</span></div>
              <div class="tag-list"><span v-for="tag in merchant.tags" :key="tag" class="tag">{{ tag }}</span></div>
            </div>
            <div class="merchant-cta"><span class="distance">{{ merchant.distance }}</span><button class="outline-button" type="button" @click="notify('商户详情接口接入后可查看完整信息')">看详情</button></div>
          </article>
          <div v-if="visibleMerchants.length === 0" class="empty-state">没有找到匹配商户，试试其他关键词或分类。</div>
        </div>

        <aside id="reviews" class="review-panel">
          <div class="review-panel-head"><h3>大家最近在聊</h3><span>真实体验 · 友善交流</span></div>
          <article v-for="review in reviews" :key="review.id" class="review-item">
            <div class="reviewer"><span class="review-avatar">{{ review.avatar }}</span><div><strong>{{ review.name }}</strong><small>{{ review.time }}</small></div><span class="review-stars">{{ '★'.repeat(review.rating) }}</span></div>
            <p class="review-text">{{ review.text }}</p>
            <div class="review-actions"><span>分享于 巷里·江南小馆</span><button class="like-button" :class="{ liked: liked.includes(review.id) }" type="button" @click="toggleLike(review.id)"><span>{{ liked.includes(review.id) ? '♥' : '♡' }}</span>{{ review.likes + (liked.includes(review.id) ? 1 : 0) }} 赞</button></div>
          </article>
          <div class="review-demo-note">点赞与评价为交互预览；登录、权限校验及持久化由后端接口提供。</div>
        </aside>
      </div>
    </section>

    <section class="wide-cta">
      <div><h3>去过一家喜欢的店？把体验分享给大家。</h3><p>评分和评价帮助更多人找到值得信赖的本地商户。</p></div>
      <button class="primary-button" type="button" @click="reviewDialog = true">写一条评价</button>
    </section>

    <div v-if="reviewDialog" class="modal-backdrop" @click.self="reviewDialog = false">
      <section class="review-modal" role="dialog" aria-modal="true" aria-labelledby="review-title">
        <button class="modal-close" type="button" aria-label="关闭" @click="reviewDialog = false">×</button>
        <p class="section-kicker">SHARE YOUR EXPERIENCE</p><h2 id="review-title">评价一家好店</h2>
        <label class="field-label">选择评分</label><div class="score-picker"><button v-for="n in 5" :key="n" type="button" :aria-label="`${n} 星`" @click="score = n" :class="{ active: n <= score }">★</button></div>
        <label class="field-label" for="review-body">说说你的体验</label><textarea id="review-body" v-model="reviewText" maxlength="500" placeholder="分享菜品、环境或服务细节，帮助其他人做出选择"></textarea>
        <div class="modal-bottom"><span>{{ reviewText.length }}/500</span><button class="primary-button" :disabled="reviewText.trim().length < 5" type="button" @click="submitReview">提交评价</button></div>
        <p class="modal-hint">当前表单为界面预览，提交不会写入服务端。</p>
      </section>
    </div>
    <div v-if="toast" class="toast" role="status">{{ toast }}</div>
  </main>
</template>
