<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ApiError, couponApi, merchantApi, type CampaignDto, type MerchantDto, type ReviewDto } from '../services/api'
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
const campaigns = ref<CampaignDto[]>([])
const campaignLoading = ref(false)
const campaignError = ref('')
const campaignPage = ref(0)
const campaignHasMore = ref(false)
const campaignDetailOpen = ref(false)
const campaignDetailLoading = ref(false)
const campaignDetailError = ref('')
const selectedCampaign = ref<CampaignDto | null>(null)
const seckillRequestId = ref('')
const seckillStatus = ref('')
const seckillMessage = ref('')
const seckillError = ref('')
const seckillBusy = ref(false)
const seckillPollGeneration = ref(0)

const selectedMerchant = computed(() => merchants.value.find((merchant) => merchant.id === selectedMerchantId.value) ?? null)
const canLoadMoreCampaigns = computed(() => campaignHasMore.value && !campaignLoading.value)

function formatMoneyMinor(value: number) {
  return `¥${(value / 100).toFixed(2)}`
}

function campaignStatusLabel(status: string) {
  const labels: Record<string, string> = { ACTIVE: '进行中', DRAFT: '未开始', ENDED: '已结束', OFFLINE: '已下线' }
  return labels[status.toUpperCase()] ?? status
}

function campaignCanSeckill(campaign: CampaignDto) {
  const now = Date.now()
  return campaign.status.toUpperCase() === 'ACTIVE' && campaign.availableStock > 0
    && new Date(campaign.startsAt).getTime() <= now && new Date(campaign.endsAt).getTime() > now
}

async function loadCampaigns(append = false) {
  campaignLoading.value = true
  campaignError.value = ''
  const nextPage = append ? campaignPage.value + 1 : 0
  try {
    const result = await couponApi.listCampaigns(nextPage, 20)
    campaigns.value = append ? [...campaigns.value, ...result] : result
    campaignPage.value = nextPage
    campaignHasMore.value = result.length === 20
  } catch (error) {
    campaignError.value = error instanceof ApiError ? error.message : '暂时无法读取优惠活动，请稍后重试。'
    if (!append) campaigns.value = []
  } finally {
    campaignLoading.value = false
  }
}

async function openCampaign(campaign: CampaignDto) {
  campaignDetailOpen.value = true
  campaignDetailLoading.value = true
  campaignDetailError.value = ''
  selectedCampaign.value = campaign
  seckillRequestId.value = ''
  seckillStatus.value = ''
  seckillMessage.value = ''
  seckillError.value = ''
  try {
    selectedCampaign.value = await couponApi.campaignDetail(campaign.id)
  } catch (error) {
    campaignDetailError.value = error instanceof ApiError ? error.message : '暂时无法读取活动详情，请稍后重试。'
  } finally {
    campaignDetailLoading.value = false
  }
}

function wait(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds))
}

async function pollSeckill(requestId: string) {
  const generation = ++seckillPollGeneration.value
  seckillBusy.value = true
  seckillError.value = ''
  seckillMessage.value = '秒杀请求已受理，正在确认处理结果…'
  try {
    for (let attempt = 0; attempt < 30; attempt += 1) {
      if (attempt > 0) await wait(Math.min(900 + attempt * 120, 3000))
      if (generation !== seckillPollGeneration.value) return
      const result = await couponApi.seckillRequest(requestId)
      seckillStatus.value = result.status
      if (result.status === 'ORDER_CREATED') {
        seckillMessage.value = result.orderId
          ? `抢购成功，订单 #${result.orderId} 已生成。请前往账户页使用余额支付。`
          : '抢购成功，订单已生成。请前往账户页查看并支付。'
        await loadCampaigns()
        return
      }
      if (result.status === 'REJECTED' || result.status === 'FAILED') {
        seckillMessage.value = result.failureReason || (result.status === 'REJECTED' ? '本次抢购未成功。' : '请求处理失败，请稍后重试。')
        return
      }
      seckillMessage.value = result.status === 'PROCESSING' ? '正在排队处理，请稍候…' : '请求已受理，正在排队处理…'
    }
    seckillMessage.value = '请求仍在处理中，可以继续查询结果或稍后到账户页查看订单。'
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      seckillError.value = '登录状态已失效，请重新登录后查看请求结果。'
      session.openAuthDialog()
    } else {
      seckillError.value = error instanceof ApiError ? error.message : '暂时无法查询请求结果，请稍后重试。'
    }
  } finally {
    if (generation === seckillPollGeneration.value) seckillBusy.value = false
  }
}

async function startSeckill(campaign: CampaignDto) {
  if (!session.user) {
    session.openAuthDialog()
    return
  }
  if (seckillBusy.value) return
  seckillRequestId.value = ''
  seckillStatus.value = ''
  seckillMessage.value = ''
  seckillError.value = ''
  seckillBusy.value = true
  try {
    const accepted = await couponApi.submitSeckill(campaign.id)
    seckillRequestId.value = accepted.requestId
    seckillStatus.value = accepted.status
    await pollSeckill(accepted.requestId)
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      seckillError.value = '请登录后参与抢购。'
      session.openAuthDialog()
    } else {
      seckillError.value = error instanceof ApiError ? error.message : '提交抢购请求失败，请稍后重试。'
    }
  } finally {
    seckillBusy.value = false
  }
}

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

onMounted(() => { void loadMerchants(); void loadCampaigns() })
onBeforeUnmount(() => { seckillPollGeneration.value += 1 })
watch(() => session.user?.id, () => {
  if (selectedMerchantId.value) void loadReviews(selectedMerchantId.value)
})
</script>

<template>
  <main class="page-wrap">
    <div class="preview-banner"><span class="preview-dot"></span>商户、评价与优惠活动已连接服务端 · 秒杀结果异步确认</div>

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
      <div class="section-heading"><div><p class="section-kicker">GOOD THINGS, RIGHT NOW</p><h2>正在发生的好优惠</h2><p>活动信息由服务端提供；抢购会排队处理并返回可查询的请求编号。</p></div><RouterLink class="text-link" to="/account">查看我的订单　↗</RouterLink></div>
      <div v-if="campaignLoading && campaigns.length === 0" class="empty-state">正在加载优惠活动…</div>
      <div v-else-if="campaignError && campaigns.length === 0" class="empty-state campaign-error"><p>{{ campaignError }}</p><button class="outline-button" type="button" @click="loadCampaigns()">重试</button></div>
      <div v-else-if="campaigns.length === 0" class="empty-state">目前没有进行中的优惠活动，稍后再来看看。</div>
      <div v-else class="deal-grid">
        <article v-for="(campaign, index) in campaigns" :key="campaign.id" class="deal-card live-deal-card" :class="`campaign-tone-${(index % 3) + 1}`">
          <span class="deal-label">{{ campaignStatusLabel(campaign.status) }}</span><div class="deal-ghost"></div>
          <h3>{{ campaign.name }}</h3><p>{{ campaign.description || '商户优惠活动' }}</p>
          <div class="campaign-meta"><span>剩余 {{ campaign.availableStock }} / {{ campaign.totalStock }}</span><span>截止 {{ formatDate(campaign.endsAt) }}</span></div>
          <div class="deal-price">{{ formatMoneyMinor(campaign.priceMinor) }}</div>
          <button class="campaign-open-button" type="button" @click="openCampaign(campaign)">查看活动详情</button>
        </article>
      </div>
      <div v-if="campaignError && campaigns.length" class="campaign-inline-error" role="alert">{{ campaignError }}</div>
      <div v-if="campaignHasMore" class="campaign-more"><button class="outline-button" type="button" :disabled="!canLoadMoreCampaigns" @click="loadCampaigns(true)">{{ campaignLoading ? '正在加载…' : '加载更多活动' }}</button></div>
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
    <div v-if="campaignDetailOpen" class="modal-backdrop" @click.self="campaignDetailOpen = false"><section class="campaign-modal" role="dialog" aria-modal="true" aria-labelledby="campaign-detail-title">
      <button class="modal-close" type="button" aria-label="关闭" @click="campaignDetailOpen = false">×</button>
      <p class="section-kicker">CAMPAIGN DETAILS</p>
      <div v-if="campaignDetailLoading" class="campaign-detail-state">正在读取活动详情…</div>
      <div v-else-if="campaignDetailError" class="campaign-detail-state campaign-detail-error"><p>{{ campaignDetailError }}</p><button class="outline-button" type="button" @click="selectedCampaign && openCampaign(selectedCampaign)">重试</button></div>
      <template v-else-if="selectedCampaign">
        <span class="deal-label">{{ campaignStatusLabel(selectedCampaign.status) }}</span>
        <h2 id="campaign-detail-title">{{ selectedCampaign.name }}</h2>
        <p class="campaign-description">{{ selectedCampaign.description || '商户优惠活动' }}</p>
        <dl class="campaign-detail-grid"><div><dt>活动价格</dt><dd>{{ formatMoneyMinor(selectedCampaign.priceMinor) }}</dd></div><div><dt>剩余库存</dt><dd>{{ selectedCampaign.availableStock }} / {{ selectedCampaign.totalStock }}</dd></div><div><dt>开始时间</dt><dd>{{ formatDate(selectedCampaign.startsAt) }}</dd></div><div><dt>结束时间</dt><dd>{{ formatDate(selectedCampaign.endsAt) }}</dd></div></dl>
        <div class="seckill-panel">
          <button class="primary-button" type="button" :disabled="!campaignCanSeckill(selectedCampaign) || seckillBusy" @click="startSeckill(selectedCampaign)">{{ seckillBusy ? '正在处理…' : !session.user ? '登录后抢购' : campaignCanSeckill(selectedCampaign) ? '提交抢购请求' : selectedCampaign.availableStock < 1 ? '库存不足' : '当前不可抢购' }}</button>
          <p v-if="seckillMessage" class="seckill-message" role="status">{{ seckillMessage }}</p>
          <p v-if="seckillStatus" class="seckill-status">请求状态：{{ seckillStatus }}</p>
          <p v-if="seckillRequestId" class="seckill-request-id">请求编号：{{ seckillRequestId }}</p>
          <p v-if="seckillError" class="seckill-error" role="alert">{{ seckillError }}</p>
          <button v-if="seckillRequestId && !seckillBusy && seckillStatus !== 'ORDER_CREATED' && seckillStatus !== 'REJECTED' && seckillStatus !== 'FAILED'" class="text-link continue-poll" type="button" @click="pollSeckill(seckillRequestId)">继续查询结果</button>
          <RouterLink v-if="seckillStatus === 'ORDER_CREATED'" class="account-order-link" to="/account" @click="campaignDetailOpen = false">前往账户查看订单并支付　↗</RouterLink>
        </div>
        <p class="campaign-note">页面库存是服务端展示值，订单消费者处理期间可能短暂延迟。抢购请求受理不代表订单已创建；订单创建后需在账户页使用可用余额支付。</p>
      </template>
    </section></div>
    <div v-if="toast" class="toast" role="status">{{ toast }}</div>
  </main>
</template>

<style scoped>
.merchant-selected{border-color:#e8a99d;box-shadow:0 0 0 2px rgba(232,93,74,.08)}
.merchant-photo.photo-1{background:linear-gradient(135deg,#f2d1b4,#ba7255)}.merchant-photo.photo-2{background:linear-gradient(135deg,#d7e2ca,#66846c)}.merchant-photo.photo-3{background:linear-gradient(135deg,#e4c8ba,#7b5b4f)}
.merchant-address{margin:6px 0 0;color:#99a2a9;font-size:10px}.error-state p{margin:0 0 12px}.review-empty{padding:22px 6px;color:#929ca2;text-align:center;font-size:11px;line-height:1.6}.review-empty p{margin:0 0 8px}.review-write-link,.review-owner-action{border:0;color:#c75b4a;background:transparent;font-size:10px;font-weight:700}.review-write-link:disabled{color:#a8b0b5;cursor:not-allowed}.review-status{margin:8px 0 0;width:fit-content;padding:3px 7px;border-radius:999px;color:#4d7960;background:#eaf5ec;font-size:9px}.review-status.pending{color:#926927;background:#fff5df}.review-status.rejected{color:#a34a40;background:#fff0ed}.review-action-group{display:flex;align-items:center;gap:3px}.review-owner-action{padding:5px}.review-form-error{margin:9px 0 0;color:#ac453a;font-size:10px}
.live-deal-card{padding-bottom:52px}.campaign-tone-2{border-color:#e6eddf!important;background:linear-gradient(135deg,#fff,#f7fbf2)!important}.campaign-tone-3{border-color:#e4e8f4!important;background:linear-gradient(135deg,#fff,#f5f7ff)!important}.campaign-meta{display:flex;flex-direction:column;gap:5px;margin-top:11px;color:#78858d;font-size:10px}.campaign-open-button{position:absolute;left:20px;bottom:17px;border:0;padding:0;color:#bb563e;background:transparent;font-size:10px;font-weight:800}.campaign-tone-2 .campaign-open-button{color:#4d7b58}.campaign-tone-3 .campaign-open-button{color:#586fa9}.campaign-error p,.campaign-detail-error p{margin:0 0 12px}.campaign-inline-error{margin-top:10px;color:#ad4a40;font-size:11px}.campaign-more{display:flex;justify-content:center;margin-top:16px}.campaign-modal{position:relative;width:min(520px,100%);max-height:min(90vh,760px);overflow-y:auto;padding:30px;border-radius:20px;background:#fff;box-shadow:0 24px 80px rgba(23,35,42,.24)}.campaign-modal h2{margin:12px 40px 8px 0;color:#27343d;font-size:23px}.campaign-detail-state{padding:30px 0;color:#7c878e;font-size:12px}.campaign-detail-error p{margin:0 0 12px}.campaign-description{color:#6f7d84;font-size:12px;line-height:1.7}.campaign-detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:20px 0}.campaign-detail-grid div{padding:12px;border-radius:10px;background:#f7f8f6}.campaign-detail-grid dt{color:#89939a;font-size:10px}.campaign-detail-grid dd{margin:5px 0 0;color:#35434b;font-size:12px;font-weight:700}.seckill-panel{padding:16px;border-radius:12px;background:#fff8f4}.seckill-message,.seckill-status,.seckill-request-id,.seckill-error{margin:10px 0 0;color:#56636b;font-size:11px;line-height:1.6;overflow-wrap:anywhere}.seckill-error{color:#ac453a}.seckill-request-id{color:#88939a}.continue-poll{display:block;margin:10px 0 0;padding:0}.account-order-link{display:inline-block;margin-top:12px;color:#bd5848;font-size:11px;font-weight:700}.campaign-note{margin:12px 0 0;color:#929ba0;font-size:10px;line-height:1.6}
@media(max-width:640px){.campaign-modal{padding:24px 19px;border-radius:17px}.campaign-modal h2{font-size:20px}.campaign-detail-grid{gap:8px}.campaign-detail-grid div{padding:10px}.live-deal-card{min-height:168px}.campaign-meta{max-width:70%}}
</style>
