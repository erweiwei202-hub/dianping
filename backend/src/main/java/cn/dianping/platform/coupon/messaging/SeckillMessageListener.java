package cn.dianping.platform.coupon.messaging;

import cn.dianping.platform.coupon.api.SeckillMessage;
import cn.dianping.platform.coupon.service.SeckillOrderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "${app.seckill.topic:seckill-requests}",
        consumerGroup = "${app.seckill.consumer-group:dianping-seckill-consumer}",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 16,
        consumeThreadMax = 64
)
public class SeckillMessageListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(SeckillMessageListener.class);

    private final ObjectMapper objectMapper;
    private final SeckillOrderProcessor processor;

    public SeckillMessageListener(ObjectMapper objectMapper, SeckillOrderProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @Override
    public void onMessage(String payload) {
        try {
            processor.process(objectMapper.readValue(payload, SeckillMessage.class));
        } catch (Exception exception) {
            log.error("Could not process seckill message", exception);
            throw new IllegalStateException("Seckill message processing failed", exception);
        }
    }
}
