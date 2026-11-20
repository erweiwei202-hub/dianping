package cn.dianping.platform.coupon.api;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.coupon.service.CouponOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class CouponOrderController {

    private final CouponOrderService orderService;

    public CouponOrderController(CouponOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<CouponOrderView>> listMine(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.listMine(page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<CouponOrderView> getMine(@PathVariable long orderId) {
        return ApiResponse.success(orderService.getMine(orderId));
    }

    @PostMapping("/{orderId}/payments")
    public ApiResponse<CouponOrderView> payWithBalance(@PathVariable long orderId) {
        return ApiResponse.success(orderService.payWithBalance(orderId));
    }
}
