package cn.dianping.platform.coupon.service;

import cn.dianping.platform.common.security.CurrentUserProvider;
import cn.dianping.platform.coupon.api.SeckillAcceptedView;
import cn.dianping.platform.coupon.api.SeckillRequestView;
import cn.dianping.platform.coupon.model.CouponOrder;
import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.repository.CouponOrderRepository;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SeckillService {

    private final SeckillRedisGateway redisGateway;
    private final SeckillRequestWriter requestWriter;
    private final SeckillRequestRepository requests;
    private final CouponOrderRepository orders;
    private final CurrentUserProvider currentUser;

    public SeckillService(SeckillRedisGateway redisGateway,
                          SeckillRequestWriter requestWriter,
                          SeckillRequestRepository requests,
                          CouponOrderRepository orders,
                          CurrentUserProvider currentUser) {
        this.redisGateway = redisGateway;
        this.requestWriter = requestWriter;
        this.requests = requests;
        this.orders = orders;
        this.currentUser = currentUser;
    }

    public SeckillAcceptedView submit(long campaignId) {
        long userId = currentUser.requiredUserId();
        String requestId = UUID.randomUUID().toString();
        String result = redisGateway.reserve(campaignId, userId, requestId);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Seckill service is unavailable");
        }
        if (result.startsWith("DUPLICATE:")) {
            String previousRequestId = result.substring("DUPLICATE:".length());
            return new SeckillAcceptedView(previousRequestId, "ALREADY_ACCEPTED");
        }
        if (!"ACCEPTED".equals(result)) {
            throw translateReserveResult(result);
        }

        try {
            if (orders.existsByCampaignIdAndUserId(campaignId, userId)) {
                redisGateway.compensate(campaignId, userId, requestId);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has an order for this campaign");
            }
            requestWriter.saveAccepted(requestId, campaignId, userId);
        } catch (DataIntegrityViolationException exception) {
            redisGateway.compensate(campaignId, userId, requestId);
            List<SeckillRequest> existing = requests.findByCampaignIdAndUserIdOrderByRequestIdDesc(campaignId, userId);
            if (!existing.isEmpty()) {
                return new SeckillAcceptedView(existing.getFirst().getRequestId(), "ALREADY_ACCEPTED");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already submitted a request for this campaign");
        } catch (RuntimeException exception) {
            redisGateway.compensate(campaignId, userId, requestId);
            throw exception;
        }
        return new SeckillAcceptedView(requestId, "ACCEPTED");
    }

    @Transactional(readOnly = true)
    public SeckillRequestView getRequest(String requestId) {
        long userId = currentUser.requiredUserId();
        SeckillRequest request = requests.findByRequestIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seckill request not found"));
        CouponOrder order = orders.findByRequestId(requestId).orElse(null);
        return SeckillRequestView.from(request, order);
    }

    private ResponseStatusException translateReserveResult(String result) {
        return switch (result) {
            case "NOT_ACTIVE" -> new ResponseStatusException(HttpStatus.CONFLICT, "Campaign is not active");
            case "NOT_STARTED" -> new ResponseStatusException(HttpStatus.CONFLICT, "Campaign has not started");
            case "ENDED" -> new ResponseStatusException(HttpStatus.CONFLICT, "Campaign has ended");
            case "SOLD_OUT" -> new ResponseStatusException(HttpStatus.CONFLICT, "Campaign is sold out");
            default -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Campaign cache is not ready");
        };
    }
}
