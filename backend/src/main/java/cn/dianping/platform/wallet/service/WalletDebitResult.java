package cn.dianping.platform.wallet.service;

public record WalletDebitResult(
        long paymentId,
        long orderId,
        long amountFen,
        long balanceAfterFen,
        boolean alreadyDebited) {
}
