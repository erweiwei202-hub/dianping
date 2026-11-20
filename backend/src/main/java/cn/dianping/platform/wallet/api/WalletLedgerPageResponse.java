package cn.dianping.platform.wallet.api;

import java.util.List;

public record WalletLedgerPageResponse(
        List<WalletLedgerItemResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {
}
