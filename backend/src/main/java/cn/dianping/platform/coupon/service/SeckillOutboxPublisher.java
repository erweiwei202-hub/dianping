package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.model.SeckillOutbox;
import cn.dianping.platform.coupon.model.SeckillOutboxStatus;
import cn.dianping.platform.coupon.repository.SeckillOutboxRepository;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeckillOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(SeckillOutboxPublisher.class);

    private final SeckillOutboxRepository outbox;
    private final SeckillOutboxStateService stateService;
    private final RocketMQTemplate rocketMQTemplate;
    private final int batchSize;
    private final long sendTimeoutMillis;

    public SeckillOutboxPublisher(SeckillOutboxRepository outbox,
                                 SeckillOutboxStateService stateService,
                                 RocketMQTemplate rocketMQTemplate,
                                 @Value("${app.seckill.outbox.batch-size:500}") int batchSize,
                                 @Value("${app.seckill.outbox.send-timeout-ms:3000}") long sendTimeoutMillis) {
        this.outbox = outbox;
        this.stateService = stateService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.batchSize = Math.max(1, Math.min(batchSize, 2000));
        this.sendTimeoutMillis = sendTimeoutMillis;
    }

    @Scheduled(fixedDelayString = "${app.seckill.outbox.poll-interval-ms:100}")
    public void publishPending() {
        stateService.resetStaleSending();
        List<SeckillOutbox> batch = outbox.findByStatusOrderByCreatedAtAsc(
                SeckillOutboxStatus.NEW, PageRequest.of(0, batchSize));
        for (SeckillOutbox event : batch) {
            if (!stateService.claim(event.getId())) {
                continue;
            }
            Message<String> message = MessageBuilder.withPayload(event.getPayload())
                    .setHeader(RocketMQHeaders.KEYS, event.getRequestId())
                    .build();
            try {
                rocketMQTemplate.asyncSend(event.getTopic(), message, new SendCallback() {
                    @Override
                    public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                        stateService.markSent(event.getId());
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        stateService.markFailure(event.getId(), throwable);
                        log.warn("Failed to publish seckill request {}", event.getRequestId(), throwable);
                    }
                }, sendTimeoutMillis);
            } catch (RuntimeException exception) {
                stateService.markFailure(event.getId(), exception);
                log.warn("Could not enqueue seckill request {}", event.getRequestId(), exception);
            }
        }
    }
}
