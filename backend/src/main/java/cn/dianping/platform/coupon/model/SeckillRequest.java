package cn.dianping.platform.coupon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seckill_request")
public class SeckillRequest {

    @Id
    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SeckillRequestStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected SeckillRequest() {
    }

    public SeckillRequest(String requestId, Long campaignId, Long userId) {
        this.requestId = requestId;
        this.campaignId = campaignId;
        this.userId = userId;
        this.status = SeckillRequestStatus.ACCEPTED;
    }

    public void markProcessing() {
        if (status == SeckillRequestStatus.ACCEPTED) {
            status = SeckillRequestStatus.PROCESSING;
        }
    }

    public void markOrderCreated() {
        status = SeckillRequestStatus.ORDER_CREATED;
        failureReason = null;
    }

    public void reject(String reason) {
        status = SeckillRequestStatus.REJECTED;
        failureReason = reason;
    }

    public void fail(String reason) {
        status = SeckillRequestStatus.FAILED;
        failureReason = reason;
    }

    public void requeue() {
        if (status == SeckillRequestStatus.FAILED || status == SeckillRequestStatus.PROCESSING) {
            status = SeckillRequestStatus.ACCEPTED;
            failureReason = null;
        }
    }

    public String getRequestId() { return requestId; }
    public Long getCampaignId() { return campaignId; }
    public Long getUserId() { return userId; }
    public SeckillRequestStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}
