package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.model.SeckillOutbox;
import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.model.SeckillRequestStatus;
import cn.dianping.platform.coupon.repository.CouponOrderRepository;
import cn.dianping.platform.coupon.repository.SeckillOutboxRepository;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeckillReplayService {

    private final SeckillRequestRepository requests;
    private final SeckillOutboxRepository outbox;
    private final CouponOrderRepository orders;

    public SeckillReplayService(SeckillRequestRepository requests,
                                SeckillOutboxRepository outbox,
                                CouponOrderRepository orders) {
        this.requests = requests;
        this.outbox = outbox;
        this.orders = orders;
    }

    @Transactional
    public void replay(String requestId) {
        SeckillRequest request = requests.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seckill request not found"));
        if (request.getStatus() == SeckillRequestStatus.ORDER_CREATED
                || request.getStatus() == SeckillRequestStatus.REJECTED
                || orders.existsByCampaignIdAndUserId(request.getCampaignId(), request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already has a terminal result");
        }
        SeckillOutbox event = outbox.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox event not found"));
        request.requeue();
        event.requeue();
    }
}
