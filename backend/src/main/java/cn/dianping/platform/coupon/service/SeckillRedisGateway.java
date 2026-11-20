package cn.dianping.platform.coupon.service;

import cn.dianping.platform.coupon.api.CampaignView;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class SeckillRedisGateway {

    private static final String RESERVE_SCRIPT = """
            local state = redis.call('HGET', KEYS[1], 'status')
            if state ~= 'ACTIVE' then return 'NOT_ACTIVE' end
            local now = tonumber(ARGV[1])
            local starts = tonumber(redis.call('HGET', KEYS[1], 'startsAt'))
            local ends = tonumber(redis.call('HGET', KEYS[1], 'endsAt'))
            if not starts or now < starts then return 'NOT_STARTED' end
            if not ends or now >= ends then return 'ENDED' end
            local previous = redis.call('GET', KEYS[3])
            if previous then return 'DUPLICATE:' .. previous end
            local stock = tonumber(redis.call('GET', KEYS[2]))
            if not stock or stock <= 0 then return 'SOLD_OUT' end
            redis.call('DECR', KEYS[2])
            local reservationTtl = (ends - now) + tonumber(ARGV[3])
            redis.call('SET', KEYS[3], ARGV[2], 'PX', reservationTtl)
            redis.call('ZADD', KEYS[4], now, ARGV[2] .. '|' .. ARGV[4] .. '|' .. ARGV[5])
            return 'ACCEPTED'
            """;

    private static final String COMPENSATE_SCRIPT = """
            local current = redis.call('GET', KEYS[2])
            local restored = 0
            if current and current == ARGV[1] then
              redis.call('DEL', KEYS[2])
              if ARGV[3] == '1' then
                redis.call('INCR', KEYS[1])
                restored = 1
              end
            end
            redis.call('ZREM', KEYS[3], ARGV[2])
            return restored
            """;

    private static final String CLEAR_RESERVATION_SCRIPT = """
            return redis.call('ZREM', KEYS[1], ARGV[1])
            """;

    private static final String RELEASE_ORDER_STOCK_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]))
            local total = tonumber(ARGV[1])
            if stock and stock >= total then return 0 end
            redis.call('INCR', KEYS[1])
            return 1
            """;

    private final StringRedisTemplate redis;
    private final Duration reservationTtl;
    private static final String RESERVATION_INDEX = "coupon:reservations:index";

    public SeckillRedisGateway(StringRedisTemplate redis) {
        this.redis = redis;
        this.reservationTtl = Duration.ofDays(7);
    }

    public void initializeCampaign(CampaignView campaign) {
        String stateKey = stateKey(campaign.id());
        redis.opsForHash().putAll(stateKey, Map.of(
                "status", "ACTIVE",
                "startsAt", Long.toString(campaign.startsAt().toEpochMilli()),
                "endsAt", Long.toString(campaign.endsAt().toEpochMilli())
        ));
        redis.opsForValue().set(stockKey(campaign.id()), Integer.toString(campaign.availableStock()));
        Duration keepFor = Duration.between(Instant.now(), campaign.endsAt()).plus(reservationTtl);
        redis.expire(stateKey, keepFor.isNegative() || keepFor.isZero() ? reservationTtl : keepFor);
        redis.expire(stockKey(campaign.id()), keepFor.isNegative() || keepFor.isZero() ? reservationTtl : keepFor);
    }

    public void markOffline(long campaignId) {
        redis.opsForHash().put(stateKey(campaignId), "status", "OFFLINE");
    }

    public String reserve(long campaignId, long userId, String requestId) {
        String userKey = userKey(campaignId, userId);
        return redis.execute(new DefaultRedisScript<>(RESERVE_SCRIPT, String.class),
                List.of(stateKey(campaignId), stockKey(campaignId), userKey, RESERVATION_INDEX),
                Long.toString(Instant.now().toEpochMilli()), requestId,
                Long.toString(reservationTtl.toMillis()), Long.toString(campaignId), Long.toString(userId));
    }

    public void compensate(long campaignId, long userId, String requestId) {
        compensate(campaignId, userId, requestId, true);
    }

    public void compensate(long campaignId, long userId, String requestId, boolean restoreStock) {
        redis.execute(new DefaultRedisScript<>(COMPENSATE_SCRIPT, Long.class),
                List.of(stockKey(campaignId), userKey(campaignId, userId), RESERVATION_INDEX),
                requestId, reservationMember(requestId, campaignId, userId), restoreStock ? "1" : "0");
    }

    public void clearReservationTracking(long campaignId, long userId, String requestId) {
        redis.execute(new DefaultRedisScript<>(CLEAR_RESERVATION_SCRIPT, Long.class),
                List.of(RESERVATION_INDEX), reservationMember(requestId, campaignId, userId));
    }

    public java.util.Set<String> staleReservations(Instant olderThan, int limit) {
        return redis.opsForZSet().rangeByScore(RESERVATION_INDEX, 0,
                olderThan.toEpochMilli(), 0, Math.max(1, limit));
    }

    public void setStock(long campaignId, int stock) {
        redis.opsForValue().set(stockKey(campaignId), Integer.toString(Math.max(0, stock)));
    }

    public void releaseExpiredOrderStock(long campaignId, int totalStock) {
        redis.execute(new DefaultRedisScript<>(RELEASE_ORDER_STOCK_SCRIPT, Long.class),
                List.of(stockKey(campaignId)), Integer.toString(totalStock));
    }

    public long currentStock(long campaignId) {
        String value = redis.opsForValue().get(stockKey(campaignId));
        return value == null ? -1 : Long.parseLong(value);
    }

    private static String stateKey(long campaignId) {
        return "coupon:campaign:" + campaignId + ":state";
    }

    private static String stockKey(long campaignId) {
        return "coupon:campaign:" + campaignId + ":stock";
    }

    private static String userKey(long campaignId, long userId) {
        return "coupon:campaign:" + campaignId + ":user:" + userId;
    }

    private static String reservationMember(String requestId, long campaignId, long userId) {
        return requestId + "|" + campaignId + "|" + userId;
    }
}
