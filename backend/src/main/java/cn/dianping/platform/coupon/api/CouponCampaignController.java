package cn.dianping.platform.coupon.api;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.coupon.service.CouponCampaignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons/activities")
public class CouponCampaignController {

    private final CouponCampaignService campaignService;

    public CouponCampaignController(CouponCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public ApiResponse<List<CampaignView>> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(campaignService.listActive(page, size));
    }

    @GetMapping("/{campaignId}")
    public ApiResponse<CampaignView> get(@PathVariable long campaignId) {
        return ApiResponse.success(campaignService.get(campaignId));
    }

}
