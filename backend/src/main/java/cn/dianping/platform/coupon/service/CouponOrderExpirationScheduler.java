package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.model.CouponOrder;
import cn.dianping.platform.coupon.model.CouponOrderStatus;
import cn.dianping.platform.coupon.repository.CouponOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class CouponOrderExpirationScheduler {

    private final CouponOrderRepository orders;
    private final CouponOrderService orderService;

    public CouponOrderExpirationScheduler(CouponOrderRepository orders, CouponOrderService orderService) {
        this.orders = orders;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.seckill.order-expiry-scan-ms:15000}")
    public void closeExpiredOrders() {
        List<CouponOrder> expired = orders.findByStatusAndPaymentExpiresAtLessThanEqualOrderByPaymentExpiresAtAsc(
                CouponOrderStatus.PENDING_PAYMENT, Instant.now(), PageRequest.of(0, 200));
        Instant now = Instant.now();
        for (CouponOrder order : expired) {
            orderService.closeExpiredOrder(order.getId(), now);
        }
    }
}
