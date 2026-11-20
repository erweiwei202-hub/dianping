package cn.dianping.platform.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Map;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record CreateReviewRequest(
            @Min(1) @Max(5) short rating,
            @NotBlank @Size(min = 1, max = 2000) String content) {
    }

    public record UpdateReviewRequest(
            @Min(1) @Max(5) short rating,
            @NotBlank @Size(min = 1, max = 2000) String content) {
    }

    public record ModerateReviewRequest(ReviewStatus status) {
    }

    public record ReviewResponse(
            long id,
            long merchantId,
            long userId,
            String displayName,
            short rating,
            String content,
            ReviewStatus status,
            long likeCount,
            boolean likedByCurrentUser,
            Instant createdAt) {
    }

    public record LikeResponse(long reviewId, long likeCount, boolean likedByCurrentUser) {
    }

    public record ReviewSummaryResponse(long merchantId, BigDecimal averageRating, long reviewCount, Map<Integer, Long> ratingDistribution) {
    }
}
