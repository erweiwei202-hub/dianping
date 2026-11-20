package cn.dianping.platform.coupon.api;

import cn.dianping.platform.coupon.model.CouponOrder;
import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.model.SeckillRequestStatus;

public record SeckillRequestView(
        String requestId,
        long campaignId,
        SeckillRequestStatus status,
        String failureReason,
        Long orderId,
        String orderStatus
) {
    public static SeckillRequestView from(SeckillRequest request, CouponOrder order) {
        return new SeckillRequestView(request.getRequestId(), request.getCampaignId(), request.getStatus(),
                request.getFailureReason(), order == null ? null : order.getId(),
                order == null ? null : order.getStatus().name());
    }
}
