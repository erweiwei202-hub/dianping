package cn.dianping.platform.coupon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "coupon_order")
public class CouponOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CouponOrderStatus status;

    @Column(name = "payment_expires_at")
    private Instant paymentExpiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected CouponOrder() {
    }

    public CouponOrder(String requestId, Long campaignId, Long userId, long amountMinor,
                       CouponOrderStatus status, Instant paymentExpiresAt) {
        this.requestId = requestId;
        this.campaignId = campaignId;
        this.userId = userId;
        this.amountMinor = amountMinor;
        this.status = status;
        this.paymentExpiresAt = paymentExpiresAt;
        this.paidAt = status == CouponOrderStatus.PAID ? Instant.now() : null;
    }

    public void markPaid(Instant paidAt) {
        if (status != CouponOrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending orders can be paid");
        }
        if (paymentExpiresAt != null && paymentExpiresAt.isBefore(paidAt)) {
            throw new IllegalStateException("Payment deadline has passed");
        }
        status = CouponOrderStatus.PAID;
        this.paidAt = paidAt;
    }

    public boolean closeIfExpired(Instant now) {
        if (status != CouponOrderStatus.PENDING_PAYMENT
                || paymentExpiresAt == null
                || paymentExpiresAt.isAfter(now)) {
            return false;
        }
        status = CouponOrderStatus.CLOSED;
        closedAt = now;
        return true;
    }

    public Long getId() { return id; }
    public String getRequestId() { return requestId; }
    public Long getCampaignId() { return campaignId; }
    public Long getUserId() { return userId; }
    public long getAmountMinor() { return amountMinor; }
    public CouponOrderStatus getStatus() { return status; }
    public Instant getPaymentExpiresAt() { return paymentExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getClosedAt() { return closedAt; }
}
