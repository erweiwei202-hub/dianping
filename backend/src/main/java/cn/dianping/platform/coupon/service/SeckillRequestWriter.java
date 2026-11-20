package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.api.SeckillMessage;
import cn.dianping.platform.coupon.model.SeckillOutbox;
import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.repository.SeckillOutboxRepository;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SeckillRequestWriter {

    private final SeckillRequestRepository requests;
    private final SeckillOutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final String topic;

    public SeckillRequestWriter(SeckillRequestRepository requests,
                                SeckillOutboxRepository outbox,
                                ObjectMapper objectMapper,
                                @Value("${app.seckill.topic:seckill-requests}") String topic) {
        this.requests = requests;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Transactional
    public void saveAccepted(String requestId, long campaignId, long userId) {
        requests.save(new SeckillRequest(requestId, campaignId, userId));
        SeckillMessage message = new SeckillMessage(requestId, campaignId, userId, Instant.now());
        try {
            outbox.save(new SeckillOutbox(requestId, topic, objectMapper.writeValueAsString(message)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize seckill request", exception);
        }
    }
}
