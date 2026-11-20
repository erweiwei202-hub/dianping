package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.model.CouponCampaign;
import cn.dianping.platform.coupon.model.CouponCampaignStatus;
import cn.dianping.platform.coupon.model.SeckillRequestStatus;
import cn.dianping.platform.coupon.repository.CouponCampaignRepository;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeckillStockReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeckillStockReconciliationScheduler.class);
    private static final List<SeckillRequestStatus> RESERVED_NOT_PERSISTED =
            List.of(SeckillRequestStatus.ACCEPTED, SeckillRequestStatus.PROCESSING);

    private final CouponCampaignRepository campaigns;
    private final SeckillRequestRepository requests;
    private final SeckillRedisGateway redisGateway;

    public SeckillStockReconciliationScheduler(CouponCampaignRepository campaigns,
                                               SeckillRequestRepository requests,
                                               SeckillRedisGateway redisGateway) {
        this.campaigns = campaigns;
        this.requests = requests;
        this.redisGateway = redisGateway;
    }

    @Scheduled(fixedDelayString = "${app.seckill.stock-reconcile-interval-ms:60000}")
    public void reportStockDrift() {
        for (CouponCampaign campaign : campaigns.findByStatusOrderByStartsAtAsc(CouponCampaignStatus.ACTIVE)) {
            long outstanding = requests.countByCampaignIdAndStatusIn(campaign.getId(), RESERVED_NOT_PERSISTED);
            long expectedRedisStock = Math.max(0, campaign.getAvailableStock() - outstanding);
            long actualRedisStock = redisGateway.currentStock(campaign.getId());
            if (actualRedisStock != expectedRedisStock) {
                log.error("Seckill stock drift campaignId={} mysqlAvailable={} outstandingRequests={} redisStock={}",
                        campaign.getId(), campaign.getAvailableStock(), outstanding, actualRedisStock);
            }
        }
    }
}
