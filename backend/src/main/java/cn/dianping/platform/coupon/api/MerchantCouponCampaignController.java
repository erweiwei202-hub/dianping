package cn.dianping.platform.coupon.api;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.coupon.service.CouponCampaignService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/coupons")
@PreAuthorize("hasAnyRole('ADMIN', 'MERCHANT')")
public class MerchantCouponCampaignController {

    private final CouponCampaignService campaignService;

    public MerchantCouponCampaignController(CouponCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ApiResponse<CampaignView> create(@Valid @RequestBody CampaignCreateRequest request) {
        return ApiResponse.success(campaignService.create(request));
    }

    @PostMapping("/{campaignId}/publish")
    public ApiResponse<CampaignView> publish(@PathVariable long campaignId) {
        return ApiResponse.success(campaignService.publish(campaignId));
    }

    @PostMapping("/{campaignId}/offline")
    public ApiResponse<CampaignView> offline(@PathVariable long campaignId) {
        return ApiResponse.success(campaignService.takeOffline(campaignId));
    }
}
