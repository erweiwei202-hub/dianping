<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError, merchantApi, type MerchantDto, type ReviewDto } from '../services/api'
import { useSessionStore } from '../stores/session'

const session = useSessionStore()
const categories = [
  { label: '全部', icon: '⌂' }, { label: '美食', icon: '◉' }, { label: '咖啡茶饮', icon: '☕' },
  { label: '休闲娱乐', icon: '✦' }, { label: '丽人美发', icon: '✿' }, { label: '运动健身', icon: '⌁' }, { label: '生活服务', icon: '◇' },
]
const selectedCategory = ref('全部')
const query = ref('')
const submittedQuery = ref('')
const merchants = ref<MerchantDto[]>([])
const merchantLoading = ref(false)
const merchantError = ref('')
const selectedMerchantId = ref<number | null>(null)
const reviews = ref<ReviewDto[]>([])
const reviewLoading = ref(false)
const reviewError = ref('')
const pendingLikes = ref(new Set<number>())
const toast = ref('')
const reviewDialog = ref(false)
const reviewText = ref('')
const score = ref(5)
const editingReviewId = ref<number | null>(null)
const submittingReview = ref(false)

const selectedMerchant = computed(() => merchants.value.find((merchant) => merchant.id === selectedMerchantId.value) ?? null)
const previewDeals = [
  { label: '活动预览', name: '巷里双人春日套餐', merchant: '江南小馆 · 活动页面待接入', price: '示例价格' },
  { label: '活动预览', name: '手冲咖啡体验券', merchant: '慢山咖啡 · 活动页面待接入', price: '示例价格' },
  { label: '活动预览', name: '炭火烧鸟分享券', merchant: '南风烧鸟 · 活动页面待接入', price: '示例价格' },
]

function notify(message: string) {
  toast.value = message
  window.setTimeout(() => { toast.value = '' }, 2600)
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function reviewStatusLabel(status: string) {
  const normalized = status.toUpperCase()
  if (normalized === 'PENDING') return '审核中'
  if (normalized === 'REJECTED') return '未通过'
  if (normalized === 'PUBLISHED' || normalized === 'APPROVED') return '已发布'
  return status
}

async function loadMerchants() {
  merchantLoading.value = true
  merchantError.value = ''
  try {
    const page = await merchantApi.list({
      keyword: submittedQuery.value.trim() || undefined,
      category: selectedCategory.value === '全部' ? undefined : selectedCategory.value,
      page: 0,
      size: 20,
    })
    merchants.value = page.content ?? []
    if (!merchants.value.some((merchant) => merchant.id === selectedMerchantId.value)) {
      selectedMerchantId.value = merchants.value[0]?.id ?? null
    }
    if (selectedMerchantId.value) await loadReviews(selectedMerchantId.value)
    else reviews.value = []
  } catch (error) {
    merchantError.value = error instanceof ApiError ? error.message : '暂时无法连接商户服务，请稍后重试。'
    merchants.value = []
    reviews.value = []
    selectedMerchantId.value = null
  } finally {
    merchantLoading.value = false
  }
}

async function loadReviews(merchantId: number) {
  reviewLoading.value = true
  reviewError.value = ''
  try {
    const page = await merchantApi.reviews(merchantId, 0, 10)
    reviews.value = page.content ?? []
  } catch (error) {
    reviewError.value = error instanceof ApiError ? error.message : '暂时无法读取评价，请稍后重试。'
    reviews.value = []
  } finally {
    reviewLoading.value = false
  }
}

function search() {
  submittedQuery.value = query.value
  void loadMerchants()
}

function chooseCategory(category: string) {
  selectedCategory.value = category
  void loadMerchants()
}

async function selectMerchant(merchant: MerchantDto) {
  selectedMerchantId.value = merchant.id
  await loadReviews(merchant.id)
  document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function toggleLike(review: ReviewDto) {
  if (!session.user) {
    session.openAuthDialog()
    return
  }
  if (pendingLikes.value.has(review.id)) return
  pendingLikes.value = new Set(pendingLikes.value).add(review.id)
  try {
    const result = review.likedByCurrentUser
      ? await merchantApi.unlike(review.id)
      : await merchantApi.like(review.id)
    reviews.value = reviews.value.map((item) => item.id === review.id
      ? { ...item, likeCount: result.likeCount, likedByCurrentUser: result.likedByCurrentUser }
      : item)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) session.openAuthDialog()
    else notify(error instanceof ApiError ? error.message : '点赞操作失败，请稍后重试。')
  } finally {
    const next = new Set(pendingLikes.value)
    next.delete(review.id)
    pendingLikes.value = next
  }
}

function openCreateReview() {
  if (!selectedMerchantId.value) {
    notify('请先选择一家商户。')
    return
  }
  if (!session.user) {
    session.openAuthDialog()
    return
  }
  editingReviewId.value = null
  reviewText.value = ''
  score.value = 5
  reviewError.value = ''
  reviewDialog.value = true
}

function openEditReview(review: ReviewDto) {
  editingReviewId.value = review.id
  reviewText.value = review.content
  score.value = review.rating
  reviewError.value = ''
  reviewDialog.value = true
}

async function submitReview() {
  if (!selectedMerchantId.value || !session.user) {
    session.openAuthDialog()
    return
  }
  submittingReview.value = true
  reviewError.value = ''
  try {
    if (editingReviewId.value) await merchantApi.updateReview(editingReviewId.value, score.value, reviewText.value.trim())
    else await merchantApi.createReview(selectedMerchantId.value, score.value, reviewText.value.trim())
    reviewDialog.value = false
    await loadReviews(selectedMerchantId.value)
    notify('评价已提交，审核状态以页面显示为准。')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      reviewDialog.value = false
      session.openAuthDialog()
    } else reviewError.value = error instanceof ApiError ? error.message : '评价提交失败，请稍后重试。'
  } finally {
    submittingReview.value = false
  }
}

async function deleteReview(review: ReviewDto) {
  if (!window.confirm('确定删除这条评价吗？')) return
  try {
    await merchantApi.deleteReview(review.id)
    if (selectedMerchantId.value) await loadReviews(selectedMerchantId.value)
    notify('评价已删除。')
  } catch (error) {
    notify(error instanceof ApiError ? error.message : '删除失败，请稍后重试。')
  }
}

onMounted(() => { void loadMerchants() })
watch(() => session.user?.id, () => {
  if (selectedMerchantId.value) void loadReviews(selectedMerchantId.value)
})
</script>

<template>
  <main class="page-wrap">
    <div class="preview-banner"><span class="preview-dot"></span>商户与评价读取服务端 · 优惠活动区仍为预览内容</div>

    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">A BETTER WAY TO LIVE LOCAL</p>
        <h1>好店不必远，<em>好生活在身边。</em></h1>
        <p class="hero-desc">发现认真做事的本地商户，读真实评价，趁喜欢的优惠正好开始。</p>
        <form class="search-box" @submit.prevent="search">
          <span class="search-icon" aria-hidden="true">⌕</span>
          <input v-model="query" aria-label="搜索商户或服务" placeholder="搜店名、服务或区域" />
          <button type="submit">开始发现</button>
        </form>
      </div>
      <div class="hero-art" aria-hidden="true"><div class="hero-orbit one"></div><div class="hero-orbit two"></div><div class="hero-sun"></div><div class="hero-plate"></div><div class="hero-leaf"></div><span class="hero-spark a">✳</span><span class="hero-spark b">✦</span></div>
    </section>

    <section class="section">
      <div class="section-heading"><div><p class="section-kicker">EXPLORE BY MOOD</p><h2>今天想体验什么？</h2><p>从附近开始，找到刚好适合你的地方。</p></div><button class="text-link" type="button" @click="chooseCategory('全部')">查看全部　↗</button></div>
      <div class="category-row" role="group" aria-label="商户分类">
        <button v-for="category in categories" :key="category.label" class="category-chip" :class="{ selected: selectedCategory === category.label }" type="button" @click="chooseCategory(category.label)"><span>{{ category.icon }}</span>{{ category.label }}</button>
      </div>
    </section>

    <section id="deals" class="section">
      <div class="section-heading"><div><p class="section-kicker">GOOD THINGS, RIGHT NOW</p><h2>正在发生的好优惠</h2><p>活动、库存与支付页面尚未接入服务端。</p></div><span class="text-link">功能预览</span></div>
      <div class="deal-grid">
        <article v-for="deal in previewDeals" :key="deal.name" class="deal-card"><span class="deal-label">{{ deal.label }}</span><div class="deal-ghost"></div><h3>{{ deal.name }}</h3><p>{{ deal.merchant }}</p><div class="deal-price">{{ deal.price }}</div></article>
      </div>
    </section>

    <section id="merchant-list" class="section">
      <div class="section-heading"><div><p class="section-kicker">LOCAL FAVORITES</p><h2>附近值得去的店</h2><p>评分来自消费者体验，看看大家为什么喜欢它。</p></div><span class="text-link">{{ merchants.length }} 家商户</span></div>
      <div class="merchant-layout">
        <div class="merchant-list">
          <div v-if="merchantLoading" class="empty-state">正在加载商户…</div>
          <div v-else-if="merchantError" class="empty-state error-state"><p>{{ merchantError }}</p><button class="outline-button" type="button" @click="loadMerchants">重试</button></div>
          <div v-else-if="merchants.length === 0" class="empty-state">暂无符合条件的商户。可以稍后再来看看。</div>
          <article v-for="(merchant, index) in merchants" :key="merchant.id" class="merchant-card" :class="{ 'merchant-selected': selectedMerchantId === merchant.id }">
            <div class="merchant-photo" :class="`photo-${(index % 3) + 1}`" role="img" :aria-label="`${merchant.name}示意图`"></div>
            <div class="merchant-info"><h3>{{ merchant.name }}</h3><p class="subline">{{ [merchant.category, merchant.region].filter(Boolean).join(' · ') }}</p><div class="rating-line"><strong class="rating-score">{{ Number(merchant.averageRating ?? 0).toFixed(1) }}</strong><span class="stars">★★★★★</span><span class="review-count">{{ Number(merchant.reviewCount ?? 0).toLocaleString('zh-CN') }} 条评价</span></div><p v-if="merchant.address" class="merchant-address">{{ merchant.address }}</p></div>
            <div class="merchant-cta"><span class="distance">{{ selectedMerchantId === merchant.id ? '正在查看评价' : '服务端商户' }}</span><button class="outline-button" type="button" @click="selectMerchant(merchant)">{{ selectedMerchantId === merchant.id ? '已选择' : '看评价' }}</button></div>
          </article>
        </div>

        <aside id="reviews" class="review-panel">
          <div class="review-panel-head"><div><h3>{{ selectedMerchant?.name ?? '商户评价' }}</h3><span>{{ selectedMerchant ? `${selectedMerchant.reviewCount} 条评价 · ${Number(selectedMerchant.averageRating ?? 0).toFixed(1)} 分` : '选择商户查看评价' }}</span></div><button class="review-write-link" type="button" :disabled="!selectedMerchant" @click="openCreateReview">写评价</button></div>
          <div v-if="reviewLoading" class="review-empty">正在加载评价…</div>
          <div v-else-if="reviewError" class="review-empty"><p>{{ reviewError }}</p><button class="text-link" type="button" @click="selectedMerchantId && loadReviews(selectedMerchantId)">重试</button></div>
          <div v-else-if="!selectedMerchant" class="review-empty">先从左侧选择一家商户。</div>
          <div v-else-if="reviews.length === 0" class="review-empty">这家商户还没有评价，来分享你的体验吧。</div>
          <article v-for="review in reviews" :key="review.id" class="review-item">
            <div class="reviewer"><span class="review-avatar">{{ review.displayName.slice(0, 1) || '访' }}</span><div><strong>{{ review.displayName }}</strong><small>{{ formatDate(review.createdAt) }}</small></div><span class="review-stars">{{ '★'.repeat(review.rating) }}</span></div>
            <div class="review-status" :class="review.status.toLowerCase()">{{ reviewStatusLabel(review.status) }}</div>
            <p class="review-text">{{ review.content }}</p>
            <div class="review-actions"><span>{{ selectedMerchant?.name }}</span><div class="review-action-group">
              <button class="like-button" :class="{ liked: review.likedByCurrentUser }" type="button" :disabled="pendingLikes.has(review.id)" @click="toggleLike(review)"><span>{{ review.likedByCurrentUser ? '♥' : '♡' }}</span>{{ review.likeCount }} 赞</button>
              <template v-if="session.user?.id === review.userId"><button class="review-owner-action" type="button" @click="openEditReview(review)">编辑</button><button class="review-owner-action" type="button" @click="deleteReview(review)">删除</button></template>
            </div></div>
          </article>
          <div class="review-demo-note">点赞、发布、编辑和删除都通过登录态与服务端权限校验。</div>
        </aside>
      </div>
    </section>

    <section class="wide-cta"><div><h3>去过一家喜欢的店？把体验分享给大家。</h3><p>评分和评价帮助更多人找到值得信赖的本地商户。</p></div><button class="primary-button" type="button" @click="openCreateReview">写一条评价</button></section>

    <div v-if="reviewDialog" class="modal-backdrop" @click.self="reviewDialog = false"><section class="review-modal" role="dialog" aria-modal="true" aria-labelledby="review-title">
      <button class="modal-close" type="button" aria-label="关闭" @click="reviewDialog = false">×</button><p class="section-kicker">SHARE YOUR EXPERIENCE</p><h2 id="review-title">{{ editingReviewId ? '编辑评价' : `评价 ${selectedMerchant?.name ?? '这家商户'}` }}</h2>
      <label class="field-label">选择评分</label><div class="score-picker"><button v-for="n in 5" :key="n" type="button" :aria-label="`${n} 星`" @click="score = n" :class="{ active: n <= score }">★</button></div>
      <label class="field-label" for="review-body">说说你的体验</label><textarea id="review-body" v-model="reviewText" maxlength="500" minlength="5" placeholder="分享菜品、环境或服务细节，帮助其他人做出选择"></textarea>
      <p v-if="reviewError" class="review-form-error" role="alert">{{ reviewError }}</p>
      <div class="modal-bottom"><span>{{ reviewText.length }}/500</span><button class="primary-button" :disabled="reviewText.trim().length < 5 || submittingReview" type="button" @click="submitReview">{{ submittingReview ? '正在提交…' : editingReviewId ? '保存修改' : '提交评价' }}</button></div>
      <p class="modal-hint">新评价提交后会进入审核；最终状态以服务端返回为准。</p>
    </section></div>
    <div v-if="toast" class="toast" role="status">{{ toast }}</div>
  </main>
</template>

<style scoped>
.merchant-selected{border-color:#e8a99d;box-shadow:0 0 0 2px rgba(232,93,74,.08)}
.merchant-photo.photo-1{background:linear-gradient(135deg,#f2d1b4,#ba7255)}.merchant-photo.photo-2{background:linear-gradient(135deg,#d7e2ca,#66846c)}.merchant-photo.photo-3{background:linear-gradient(135deg,#e4c8ba,#7b5b4f)}
.merchant-address{margin:6px 0 0;color:#99a2a9;font-size:10px}.error-state p{margin:0 0 12px}.review-empty{padding:22px 6px;color:#929ca2;text-align:center;font-size:11px;line-height:1.6}.review-empty p{margin:0 0 8px}.review-write-link,.review-owner-action{border:0;color:#c75b4a;background:transparent;font-size:10px;font-weight:700}.review-write-link:disabled{color:#a8b0b5;cursor:not-allowed}.review-status{margin:8px 0 0;width:fit-content;padding:3px 7px;border-radius:999px;color:#4d7960;background:#eaf5ec;font-size:9px}.review-status.pending{color:#926927;background:#fff5df}.review-status.rejected{color:#a34a40;background:#fff0ed}.review-action-group{display:flex;align-items:center;gap:3px}.review-owner-action{padding:5px}.review-form-error{margin:9px 0 0;color:#ac453a;font-size:10px}
</style>
