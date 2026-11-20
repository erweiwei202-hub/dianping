package cn.dianping.platform.review;

import cn.dianping.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/users/me/reviews")
    public ApiResponse<Page<ReviewDtos.ReviewResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviewService.listMine(page, size));
    }

    @PatchMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewDtos.ReviewResponse> update(
            @PathVariable long reviewId,
            @Valid @RequestBody ReviewDtos.UpdateReviewRequest request) {
        return ApiResponse.success(reviewService.update(reviewId, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> delete(@PathVariable long reviewId) {
        reviewService.delete(reviewId);
        return ApiResponse.success(null);
    }

    @PostMapping("/reviews/{reviewId}/likes")
    public ApiResponse<ReviewDtos.LikeResponse> like(@PathVariable long reviewId) {
        return ApiResponse.success(reviewService.like(reviewId));
    }

    @DeleteMapping("/reviews/{reviewId}/likes")
    public ApiResponse<ReviewDtos.LikeResponse> unlike(@PathVariable long reviewId) {
        return ApiResponse.success(reviewService.unlike(reviewId));
    }
}
