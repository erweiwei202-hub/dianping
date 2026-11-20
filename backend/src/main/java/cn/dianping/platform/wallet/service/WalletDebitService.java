package cn.dianping.platform.wallet.service;

/**
 * Called inside the coupon-order transaction after the caller has locked and validated the order.
 * The REQUIRED transaction joins that order transaction so the order state and wallet debit commit together.
 */
public interface WalletDebitService {

    WalletDebitResult debitForOrder(long userId, long orderId, long amountMinor);
}
