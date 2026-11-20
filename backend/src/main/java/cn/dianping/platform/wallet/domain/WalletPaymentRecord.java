package cn.dianping.platform.wallet.domain;

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
@Table(name = "wallet_payment_record")
public class WalletPaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private long orderId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "amount_fen", nullable = false)
    private long amountFen;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private WalletPaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletPaymentRecord() {
    }

    public WalletPaymentRecord(long orderId, long userId, long amountFen) {
        this.orderId = orderId;
        this.userId = userId;
        this.amountFen = amountFen;
        this.status = WalletPaymentStatus.SUCCEEDED;
        this.idempotencyKey = "wallet-order:" + orderId;
        this.createdAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getUserId() {
        return userId;
    }

    public long getAmountFen() {
        return amountFen;
    }

    public WalletPaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
