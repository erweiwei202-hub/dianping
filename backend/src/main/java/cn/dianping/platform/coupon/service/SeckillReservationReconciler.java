package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.model.SeckillRequest;
import cn.dianping.platform.coupon.model.SeckillRequestStatus;
import cn.dianping.platform.coupon.repository.SeckillRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class SeckillReservationReconciler {

    private static final Logger log = LoggerFactory.getLogger(SeckillReservationReconciler.class);

    private final SeckillRedisGateway redisGateway;
    private final SeckillRequestRepository requests;

    public SeckillReservationReconciler(SeckillRedisGateway redisGateway, SeckillRequestRepository requests) {
        this.redisGateway = redisGateway;
        this.requests = requests;
    }

    @Scheduled(fixedDelayString = "${app.seckill.reservation-reconcile-interval-ms:30000}")
    public void reconcileOrphans() {
        Instant cutoff = Instant.now().minusSeconds(120);
        Set<String> stale = redisGateway.staleReservations(cutoff, 500);
        int released = 0;
        int cleared = 0;
        int waiting = 0;
        int invalid = 0;
        for (String value : stale) {
            Reservation reservation = parse(value);
            if (reservation == null) {
                invalid++;
                continue;
            }
            SeckillRequest request = requests.findById(reservation.requestId()).orElse(null);
            if (request == null) {
                redisGateway.compensate(reservation.campaignId(), reservation.userId(), reservation.requestId());
                released++;
                continue;
            }
            if (request.getStatus() == SeckillRequestStatus.ORDER_CREATED) {
                redisGateway.clearReservationTracking(reservation.campaignId(), reservation.userId(), reservation.requestId());
                cleared++;
            } else if (request.getStatus() == SeckillRequestStatus.REJECTED
                    || request.getStatus() == SeckillRequestStatus.FAILED) {
                redisGateway.compensate(reservation.campaignId(), reservation.userId(), reservation.requestId());
                released++;
            } else {
                waiting++;
            }
        }
        if (released + cleared + waiting + invalid > 0) {
            log.warn("Seckill reservation reconciliation released={}, cleared={}, waitingForConsumer={}, invalid={}",
                    released, cleared, waiting, invalid);
        }
    }

    private Reservation parse(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Reservation(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record Reservation(String requestId, long campaignId, long userId) {
    }
}
