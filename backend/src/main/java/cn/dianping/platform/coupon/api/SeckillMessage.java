package cn.dianping.platform.coupon.api;

import java.time.Instant;

public record SeckillMessage(String requestId, long campaignId, long userId, Instant requestedAt) {
}
