package cn.dianping.platform.coupon.api;

import cn.dianping.platform.coupon.model.CouponCampaign;
import cn.dianping.platform.coupon.model.CouponCampaignStatus;

import java.time.Instant;

public record CampaignView(
        long id,
        long merchantId,
        String name,
        String description,
        int totalStock,
        int availableStock,
        long priceMinor,
        Instant startsAt,
        Instant endsAt,
        CouponCampaignStatus status
) {
    public static CampaignView from(CouponCampaign campaign) {
        return new CampaignView(
                campaign.getId(), campaign.getMerchantId(), campaign.getName(), campaign.getDescription(),
                campaign.getTotalStock(), campaign.getAvailableStock(), campaign.getPriceMinor(),
                campaign.getStartsAt(), campaign.getEndsAt(), campaign.getStatus());
    }
}
