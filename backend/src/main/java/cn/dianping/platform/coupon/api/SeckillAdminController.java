package cn.dianping.platform.coupon.api;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.coupon.service.SeckillReplayService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/seckill/requests")
@PreAuthorize("hasRole('ADMIN')")
public class SeckillAdminController {

    private final SeckillReplayService replayService;

    public SeckillAdminController(SeckillReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/{requestId}/replay")
    public ApiResponse<Map<String, String>> replay(@PathVariable String requestId) {
        replayService.replay(requestId);
        return ApiResponse.success(Map.of("requestId", requestId, "status", "REQUEUED"));
    }
}
