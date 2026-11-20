package cn.dianping.platform.coupon.order;

public interface WalletDebitPort {

    void debitForOrder(long userId, long orderId, long amountMinor);
}
