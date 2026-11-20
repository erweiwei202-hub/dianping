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
@Table(name = "wallet_ledger")
public class WalletLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 32)
    private WalletLedgerType entryType;

    @Column(name = "business_id", nullable = false, length = 100)
    private String businessId;

    @Column(name = "amount_fen", nullable = false)
    private long amountFen;

    @Column(name = "balance_after_fen", nullable = false)
    private long balanceAfterFen;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletLedgerEntry() {
    }

    private WalletLedgerEntry(long userId, WalletLedgerType entryType, String businessId,
                              long amountFen, long balanceAfterFen, String description) {
        this.userId = userId;
        this.entryType = entryType;
        this.businessId = businessId;
        this.amountFen = amountFen;
        this.balanceAfterFen = balanceAfterFen;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public static WalletLedgerEntry orderDebit(long userId, long orderId, long amountFen, long balanceAfterFen) {
        return new WalletLedgerEntry(userId, WalletLedgerType.ORDER_DEBIT, Long.toString(orderId),
                -amountFen, balanceAfterFen, "Coupon order payment");
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public WalletLedgerType getEntryType() {
        return entryType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public long getAmountFen() {
        return amountFen;
    }

    public long getBalanceAfterFen() {
        return balanceAfterFen;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
