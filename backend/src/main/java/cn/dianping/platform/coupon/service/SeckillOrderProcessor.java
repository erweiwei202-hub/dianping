package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.api.SeckillMessage;
import cn.dianping.platform.coupon.model.CouponCampaign;
import cn.dianping.platform.coupon.model.CouponOrder;
import cn.dianping.platform.coupon.model.CouponOrderStatus;
import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.model.SeckillRequestStatus;
import cn.dianping.platform.coupon.repository.CouponCampaignRepository;
import cn.dianping.platform.coupon.repository.CouponOrderRepository;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class SeckillOrderProcessor {

    private final SeckillRequestRepository requests;
    private final CouponCampaignRepository campaigns;
    private final CouponOrderRepository orders;
    private final SeckillRedisGateway redisGateway;
    private final long paymentTimeoutMinutes;

    public SeckillOrderProcessor(SeckillRequestRepository requests,
                                 CouponCampaignRepository campaigns,
                                 CouponOrderRepository orders,
                                 SeckillRedisGateway redisGateway,
                                 @Value("${app.seckill.order-payment-timeout-minutes:15}") long paymentTimeoutMinutes) {
        this.requests = requests;
        this.campaigns = campaigns;
        this.orders = orders;
        this.redisGateway = redisGateway;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Transactional
    public void process(SeckillMessage message) {
        SeckillRequest request = requests.findByRequestIdForUpdate(message.requestId()).orElse(null);
        if (request == null) {
            return;
        }
        if (request.getStatus() == SeckillRequestStatus.ORDER_CREATED
                || request.getStatus() == SeckillRequestStatus.REJECTED) {
            return;
        }
        if (!request.getCampaignId().equals(message.campaignId())
                || !request.getUserId().equals(message.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seckill event does not match request record");
        }

        request.markProcessing();
        CouponCampaign campaign = campaigns.findById(message.campaignId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (orders.existsByCampaignIdAndUserId(campaign.getId(), message.userId())) {
            request.reject("User already has an order for this campaign");
            afterCommit(() -> redisGateway.compensate(campaign.getId(), message.userId(), message.requestId()));
            return;
        }
        if (campaigns.reserveAvailableStock(campaign.getId()) == 0) {
            request.reject("Campaign is sold out");
            afterCommit(() -> {
                redisGateway.compensate(campaign.getId(), message.userId(), message.requestId(), false);
                redisGateway.setStock(campaign.getId(), 0);
            });
            return;
        }

        Instant now = Instant.now();
        boolean free = campaign.getPriceMinor() == 0;
        Instant paymentExpiresAt = free ? null : now.plusSeconds(paymentTimeoutMinutes * 60);
        CouponOrder order = new CouponOrder(message.requestId(), campaign.getId(), message.userId(),
                campaign.getPriceMinor(), free ? CouponOrderStatus.PAID : CouponOrderStatus.PENDING_PAYMENT,
                paymentExpiresAt);
        orders.save(order);
        request.markOrderCreated();
        afterCommit(() -> redisGateway.clearReservationTracking(campaign.getId(), message.userId(), message.requestId()));
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
