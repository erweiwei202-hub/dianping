package cn.dianping.platform.coupon.api;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.coupon.service.SeckillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/coupons/activities/{campaignId}/seckill")
    public ApiResponse<SeckillAcceptedView> submit(@PathVariable long campaignId) {
        return ApiResponse.success(seckillService.submit(campaignId));
    }

    @GetMapping("/seckill-requests/{requestId}")
    public ApiResponse<SeckillRequestView> get(@PathVariable String requestId) {
        return ApiResponse.success(seckillService.getRequest(requestId));
    }
}
