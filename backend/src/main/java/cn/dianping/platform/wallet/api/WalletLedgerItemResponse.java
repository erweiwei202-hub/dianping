package cn.dianping.platform.wallet.api;

import cn.dianping.platform.wallet.domain.WalletLedgerType;

import java.time.Instant;

public record WalletLedgerItemResponse(
        long id,
        WalletLedgerType type,
        String businessId,
        long amountFen,
        long balanceAfterFen,
        String description,
        Instant createdAt) {
}
