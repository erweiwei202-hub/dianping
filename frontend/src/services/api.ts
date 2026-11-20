export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export interface UserDto {
  id: number
  username: string
  displayName: string
  role: string
  createdAt?: string
}

export interface MerchantDto {
  id: number
  name: string
  category: string
  region: string
  address?: string
  description?: string
  averageRating: number
  reviewCount: number
  status?: string
  createdAt?: string
}

export interface ReviewDto {
  id: number
  merchantId: number
  userId: number
  displayName: string
  rating: number
  content: string
  status: string
  likeCount: number
  likedByCurrentUser: boolean
  createdAt: string
}

export interface ReviewSummaryDto {
  merchantId: number
  averageRating: number
  reviewCount: number
  ratingDistribution: Record<number, number>
}

export interface CampaignDto {
  id: number
  merchantId: number
  name: string
  description: string
  totalStock: number
  availableStock: number
  priceMinor: number
  startsAt: string
  endsAt: string
  status: 'DRAFT' | 'ACTIVE' | 'ENDED' | 'OFFLINE' | string
}

export interface SeckillAcceptedDto {
  requestId: string
  status: string
}

export interface SeckillRequestDto {
  requestId: string
  campaignId: number
  status: 'ACCEPTED' | 'PROCESSING' | 'ORDER_CREATED' | 'REJECTED' | 'FAILED' | string
  failureReason: string | null
  orderId: number | null
  orderStatus: string | null
}

export interface CouponOrderDto {
  id: number
  campaignId: number
  amountMinor: number
  status: 'PENDING_PAYMENT' | 'PAID' | 'CLOSED' | 'REDEEMED' | string
  paymentExpiresAt: string | null
  createdAt: string
}

export interface WalletBalanceDto {
  availableBalanceFen: number
  frozenBalanceFen: number
}

export interface WalletLedgerItemDto {
  id: number
  type: string
  businessId: string
  amountFen: number
  balanceAfterFen: number
  description: string
  createdAt: string
}

export interface WalletLedgerPageDto {
  items: WalletLedgerItemDto[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

interface ApiEnvelope<T> {
  success?: boolean
  data?: T
  message?: string
  timestamp?: string
}

export interface PageDto<T> {
  content: T[]
  totalElements?: number
  totalPages?: number
  number?: number
  size?: number
}

let csrfToken: string | null = null

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const value = document.cookie.split(';').map((part) => part.trim()).find((part) => part.startsWith(prefix))
  return value ? decodeURIComponent(value.slice(prefix.length)) : null
}

async function getCsrfToken(): Promise<string> {
  csrfToken = readCookie('XSRF-TOKEN') ?? csrfToken
  if (csrfToken) return csrfToken

  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  const raw = await response.json().catch(() => ({})) as ApiEnvelope<{ token?: string }> & { token?: string }
  if (!response.ok) throw new ApiError(raw.message ?? '无法建立安全会话，请稍后重试。', response.status)

  csrfToken = readCookie('XSRF-TOKEN') ?? raw.token ?? raw.data?.token ?? null
  if (!csrfToken) throw new ApiError('服务器未返回 CSRF 令牌，请刷新后重试。', 500)
  return csrfToken
}

function unwrap<T>(body: unknown): T {
  if (body && typeof body === 'object' && 'success' in body) {
    const envelope = body as ApiEnvelope<T>
    if (envelope.success === false) throw new ApiError(envelope.message ?? '请求失败。', 400)
    return envelope.data as T
  }
  return body as T
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) headers.set('X-XSRF-TOKEN', await getCsrfToken())

  const response = await fetch(path, { ...init, method, headers, credentials: 'include' })
  const raw = await response.json().catch(() => null)
  if (!response.ok) {
    const message = raw && typeof raw === 'object' && 'message' in raw ? String(raw.message) : '服务暂时不可用，请稍后重试。'
    throw new ApiError(message, response.status)
  }
  return unwrap<T>(raw)
}

export const authApi = {
  me: () => request<UserDto>('/api/auth/me'),
  login: (username: string, password: string) => request<UserDto>('/api/auth/login', {
    method: 'POST', body: JSON.stringify({ username, password }),
  }),
  register: (username: string, password: string, displayName: string) => request<UserDto>('/api/auth/register', {
    method: 'POST', body: JSON.stringify({ username, password, displayName }),
  }),
  logout: () => request<null>('/api/auth/logout', { method: 'POST' }),
}

export const merchantApi = {
  list: (params: { keyword?: string; category?: string; region?: string; page?: number; size?: number }) => {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => { if (value !== undefined && value !== '') query.set(key, String(value)) })
    return request<PageDto<MerchantDto>>(`/api/merchants?${query.toString()}`)
  },
  detail: (merchantId: number) => request<MerchantDto>(`/api/merchants/${merchantId}`),
  reviews: (merchantId: number, page = 0, size = 10) => request<PageDto<ReviewDto>>(`/api/merchants/${merchantId}/reviews?page=${page}&size=${size}`),
  summary: (merchantId: number) => request<ReviewSummaryDto>(`/api/merchants/${merchantId}/review-summary`),
  createReview: (merchantId: number, rating: number, content: string) => request<ReviewDto>(`/api/merchants/${merchantId}/reviews`, {
    method: 'POST', body: JSON.stringify({ rating, content }),
  }),
  updateReview: (reviewId: number, rating: number, content: string) => request<ReviewDto>(`/api/reviews/${reviewId}`, {
    method: 'PATCH', body: JSON.stringify({ rating, content }),
  }),
  deleteReview: (reviewId: number) => request<null>(`/api/reviews/${reviewId}`, { method: 'DELETE' }),
  like: (reviewId: number) => request<{ reviewId: number; likeCount: number; likedByCurrentUser: boolean }>(`/api/reviews/${reviewId}/likes`, { method: 'POST' }),
  unlike: (reviewId: number) => request<{ reviewId: number; likeCount: number; likedByCurrentUser: boolean }>(`/api/reviews/${reviewId}/likes`, { method: 'DELETE' }),
}

export const couponApi = {
  listCampaigns: (page = 0, size = 20) => request<CampaignDto[]>(`/api/coupons/activities?page=${page}&size=${size}`),
  campaignDetail: (campaignId: number) => request<CampaignDto>(`/api/coupons/activities/${campaignId}`),
  submitSeckill: (campaignId: number) => request<SeckillAcceptedDto>(`/api/coupons/activities/${campaignId}/seckill`, { method: 'POST' }),
  seckillRequest: (requestId: string) => request<SeckillRequestDto>(`/api/seckill-requests/${encodeURIComponent(requestId)}`),
  myOrders: (page = 0, size = 20) => request<CouponOrderDto[]>(`/api/orders?page=${page}&size=${size}`),
  payWithBalance: (orderId: number) => request<CouponOrderDto>(`/api/orders/${orderId}/payments`, { method: 'POST' }),
}

export const walletApi = {
  balance: () => request<WalletBalanceDto>('/api/wallet'),
  ledger: (page = 0, size = 20) => request<WalletLedgerPageDto>(`/api/wallet/ledger?page=${page}&size=${size}`),
}
