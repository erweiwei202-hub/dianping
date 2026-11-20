package cn.dianping.platform.merchant;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.review.ReviewDtos;
import cn.dianping.platform.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final ReviewService reviewService;

    public MerchantController(MerchantService merchantService, ReviewService reviewService) {
        this.merchantService = merchantService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<Page<MerchantDtos.MerchantResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(merchantService.search(keyword, category, region, page, size));
    }

    @GetMapping("/{merchantId}")
    public ApiResponse<MerchantDtos.MerchantResponse> get(@PathVariable long merchantId) {
        return ApiResponse.success(merchantService.getActive(merchantId));
    }

    @GetMapping("/{merchantId}/reviews")
    public ApiResponse<Page<ReviewDtos.ReviewResponse>> reviews(
            @PathVariable long merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviewService.listPublished(merchantId, page, size));
    }

    @GetMapping("/{merchantId}/review-summary")
    public ApiResponse<ReviewDtos.ReviewSummaryResponse> reviewSummary(@PathVariable long merchantId) {
        return ApiResponse.success(reviewService.summary(merchantId));
    }

    @PostMapping("/{merchantId}/reviews")
    public ApiResponse<ReviewDtos.ReviewResponse> createReview(
            @PathVariable long merchantId,
            @Valid @RequestBody ReviewDtos.CreateReviewRequest request) {
        return ApiResponse.success(reviewService.create(merchantId, request));
    }

}
