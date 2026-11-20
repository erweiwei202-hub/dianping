package cn.dianping.platform.review;

import cn.dianping.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/pending")
    public ApiResponse<Page<ReviewDtos.ReviewResponse>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviewService.listPending(page, size));
    }

    @PatchMapping("/{reviewId}/status")
    public ApiResponse<ReviewDtos.ReviewResponse> moderate(
            @PathVariable long reviewId,
            @Valid @RequestBody ReviewDtos.ModerateReviewRequest request) {
        return ApiResponse.success(reviewService.moderate(reviewId, request));
    }
}
