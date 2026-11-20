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
@Table(name = "seckill_outbox")
public class SeckillOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false, length = 160)
    private String topic;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SeckillOutboxStatus status = SeckillOutboxStatus.NEW;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected SeckillOutbox() {
    }

    public SeckillOutbox(String requestId, String topic, String payload) {
        this.requestId = requestId;
        this.topic = topic;
        this.payload = payload;
        this.status = SeckillOutboxStatus.NEW;
    }

    public void markSending() {
        status = SeckillOutboxStatus.SENDING;
        attempts++;
        lastError = null;
    }

    public void markSent() {
        status = SeckillOutboxStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
    }

    public void retry(String error) {
        status = SeckillOutboxStatus.NEW;
        lastError = error == null ? "Unknown publish failure" : error.substring(0, Math.min(error.length(), 1000));
    }

    public void markFailed(String error) {
        status = SeckillOutboxStatus.FAILED;
        lastError = error == null ? "Maximum publish attempts reached" : error.substring(0, Math.min(error.length(), 1000));
    }

    public void resetStaleSending() {
        if (status == SeckillOutboxStatus.SENDING) {
            status = SeckillOutboxStatus.NEW;
        }
    }

    public void requeue() {
        status = SeckillOutboxStatus.NEW;
        attempts = 0;
        lastError = null;
        sentAt = null;
    }

    public Long getId() { return id; }
    public String getRequestId() { return requestId; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public SeckillOutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSentAt() { return sentAt; }
}
