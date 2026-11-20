package cn.dianping.platform.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "wallet_account")
public class WalletAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private long userId;

    @Column(name = "available_balance_fen", nullable = false)
    private long availableBalanceFen;

    @Column(name = "frozen_balance_fen", nullable = false)
    private long frozenBalanceFen;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletAccount() {
    }

    public WalletAccount(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.userId = userId;
        this.availableBalanceFen = 0;
        this.frozenBalanceFen = 0;
        this.version = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public long getUserId() {
        return userId;
    }

    public long getAvailableBalanceFen() {
        return availableBalanceFen;
    }

    public long getFrozenBalanceFen() {
        return frozenBalanceFen;
    }

    public void debit(long amountFen) {
        if (amountFen <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (availableBalanceFen < amountFen) {
            throw new IllegalStateException("Wallet balance is insufficient");
        }
        availableBalanceFen -= amountFen;
        updatedAt = Instant.now();
    }
}
