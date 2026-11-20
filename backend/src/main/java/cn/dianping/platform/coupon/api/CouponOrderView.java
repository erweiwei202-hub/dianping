package cn.dianping.platform.coupon.api;

import cn.dianping.platform.coupon.model.CouponOrder;

import java.time.Instant;

public record CouponOrderView(
        long id,
        long campaignId,
        long amountMinor,
        String status,
        Instant paymentExpiresAt,
        Instant createdAt
) {
    public static CouponOrderView from(CouponOrder order) {
        return new CouponOrderView(order.getId(), order.getCampaignId(), order.getAmountMinor(),
                order.getStatus().name(), order.getPaymentExpiresAt(), order.getCreatedAt());
    }
}
