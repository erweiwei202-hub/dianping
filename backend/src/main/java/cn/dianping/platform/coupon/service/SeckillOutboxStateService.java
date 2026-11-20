package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.model.SeckillOutbox;
import cn.dianping.platform.coupon.model.SeckillOutboxStatus;
import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.model.SeckillRequestStatus;
import cn.dianping.platform.coupon.repository.SeckillOutboxRepository;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class SeckillOutboxStateService {

    private static final int MAX_ATTEMPTS = 20;

    private final SeckillOutboxRepository outbox;
    private final SeckillRequestRepository requests;
    private final SeckillRedisGateway redisGateway;

    public SeckillOutboxStateService(SeckillOutboxRepository outbox,
                                     SeckillRequestRepository requests,
                                     SeckillRedisGateway redisGateway) {
        this.outbox = outbox;
        this.requests = requests;
        this.redisGateway = redisGateway;
    }

    @Transactional
    public boolean claim(long id) {
        return outbox.claim(id, SeckillOutboxStatus.NEW, SeckillOutboxStatus.SENDING) == 1;
    }

    @Transactional
    public void markSent(long id) {
        outbox.findById(id).ifPresent(SeckillOutbox::markSent);
    }

    @Transactional
    public void markFailure(long id, Throwable error) {
        outbox.findById(id).ifPresent(event -> {
            String message = error == null ? "Unknown RocketMQ publish error" : error.getMessage();
            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.markFailed(message);
                requests.findByRequestIdForUpdate(event.getRequestId()).ifPresent(request -> {
                    if (request.getStatus() == SeckillRequestStatus.ACCEPTED
                            || request.getStatus() == SeckillRequestStatus.PROCESSING) {
                        request.fail("Message could not be published after retries");
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                redisGateway.compensate(request.getCampaignId(), request.getUserId(), request.getRequestId());
                            }
                        });
                    }
                });
            } else {
                event.retry(message);
            }
        });
    }

    @Transactional
    public int resetStaleSending() {
        return outbox.resetStale(SeckillOutboxStatus.SENDING, SeckillOutboxStatus.NEW,
                Instant.now().minusSeconds(60));
    }
}
