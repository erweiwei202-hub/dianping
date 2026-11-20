package cn.dianping.platform.review;

import cn.dianping.platform.common.security.CurrentUserProvider;
import cn.dianping.platform.identity.UserAccount;
import cn.dianping.platform.identity.UserAccountRepository;
import cn.dianping.platform.merchant.MerchantRepository;
import cn.dianping.platform.merchant.MerchantStatus;
import cn.dianping.platform.merchant.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final MerchantRepository merchantRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserProvider currentUserProvider;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            MerchantRepository merchantRepository,
            UserAccountRepository userAccountRepository,
            CurrentUserProvider currentUserProvider) {
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.merchantRepository = merchantRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDtos.ReviewResponse> listPublished(long merchantId, int page, int size) {
        ensureActiveMerchant(merchantId);
        Page<Review> reviews = reviewRepository.findByMerchantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                merchantId, ReviewStatus.PUBLISHED, pageRequest(page, size, Sort.Direction.DESC));
        return mapPage(reviews, currentUserProvider.optionalUserId());
    }

    @Transactional(readOnly = true)
    public Page<ReviewDtos.ReviewResponse> listMine(int page, int size) {
        long userId = currentUserProvider.requiredUserId();
        return mapPage(reviewRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, pageRequest(page, size, Sort.Direction.DESC)), OptionalLong.of(userId));
    }

    @Transactional(readOnly = true)
    public ReviewDtos.ReviewSummaryResponse summary(long merchantId) {
        Merchant merchant = merchantRepository.findByIdAndStatus(merchantId, MerchantStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        Map<Integer, Long> distribution = new HashMap<>();
        for (ReviewRepository.RatingCount row : reviewRepository.findRatingDistribution(merchantId, ReviewStatus.PUBLISHED)) {
            distribution.put((int) row.getRating(), row.getReviewCount());
        }
        for (int rating = 1; rating <= 5; rating++) {
            distribution.putIfAbsent(rating, 0L);
        }
        return new ReviewDtos.ReviewSummaryResponse(merchantId, merchant.getAverageRating(), merchant.getReviewCount(), Map.copyOf(distribution));
    }

    @Transactional
    public ReviewDtos.ReviewResponse create(long merchantId, ReviewDtos.CreateReviewRequest request) {
        ensureActiveMerchant(merchantId);
        long userId = currentUserProvider.requiredUserId();
        Review review = reviewRepository.save(new Review(merchantId, userId, request.rating(), request.content().trim()));
        return toResponse(review, authorName(userId), false);
    }

    @Transactional
    public ReviewDtos.ReviewResponse update(long reviewId, ReviewDtos.UpdateReviewRequest request) {
        long userId = currentUserProvider.requiredUserId();
        Review review = findReviewForUpdate(reviewId);
        if (review.getUserId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own review");
        }
        ReviewStatus previousStatus = review.getStatus();
        review.revise(request.rating(), request.content().trim());
        if (previousStatus == ReviewStatus.PUBLISHED) {
            recalculateMerchantSummary(review.getMerchantId());
        }
        return toResponse(review, authorName(userId), false);
    }

    @Transactional
    public void delete(long reviewId) {
        long userId = currentUserProvider.requiredUserId();
        Review review = findReviewForUpdate(reviewId);
        if (review.getUserId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own review");
        }
        review.softDelete();
        reviewLikeRepository.deleteAllForReview(reviewId);
        recalculateMerchantSummary(review.getMerchantId());
    }

    @Transactional
    public ReviewDtos.LikeResponse like(long reviewId) {
        long userId = currentUserProvider.requiredUserId();
        Review review = findPublishedReviewForUpdate(reviewId);
        int inserted = reviewLikeRepository.insertIfAbsent(reviewId, userId);
        if (inserted > 0) {
            reviewRepository.incrementLikeCount(reviewId, Instant.now());
        }
        return new ReviewDtos.LikeResponse(reviewId, reviewLikeRepository.countByReviewId(reviewId), true);
    }

    @Transactional
    public ReviewDtos.LikeResponse unlike(long reviewId) {
        long userId = currentUserProvider.requiredUserId();
        findPublishedReviewForUpdate(reviewId);
        int deleted = reviewLikeRepository.deleteRelation(reviewId, userId);
        if (deleted > 0) {
            reviewRepository.decrementLikeCount(reviewId, Instant.now());
        }
        return new ReviewDtos.LikeResponse(reviewId, reviewLikeRepository.countByReviewId(reviewId), false);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDtos.ReviewResponse> listPending(int page, int size) {
        return mapPage(reviewRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                ReviewStatus.PENDING, pageRequest(page, size, Sort.Direction.ASC)), OptionalLong.empty());
    }

    @Transactional
    public ReviewDtos.ReviewResponse moderate(long reviewId, ReviewDtos.ModerateReviewRequest request) {
        if (request.status() != ReviewStatus.PUBLISHED && request.status() != ReviewStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderation status must be PUBLISHED or REJECTED");
        }
        Review review = findReviewForUpdate(reviewId);
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted reviews cannot be moderated");
        }
        review.setStatus(request.status());
        recalculateMerchantSummary(review.getMerchantId());
        return toResponse(review, authorName(review.getUserId()), false);
    }

    private Page<ReviewDtos.ReviewResponse> mapPage(Page<Review> page, OptionalLong currentUserId) {
        Collection<Long> reviewIds = page.getContent().stream().map(Review::getId).toList();
        if (reviewIds.isEmpty()) {
            return page.map(review -> toResponse(review, null, false));
        }
        Set<Long> likedIds = currentUserId.isPresent()
                ? reviewLikeRepository.findLikedReviewIds(currentUserId.getAsLong(), reviewIds)
                : Set.of();
        Set<Long> authorIds = page.getContent().stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, String> displayNames = userAccountRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, UserAccount::getDisplayName));
        return page.map(review -> toResponse(review, displayNames.get(review.getUserId()), likedIds.contains(review.getId())));
    }

    private ReviewDtos.ReviewResponse toResponse(Review review, String displayName, boolean liked) {
        return new ReviewDtos.ReviewResponse(review.getId(), review.getMerchantId(), review.getUserId(), displayName,
                review.getRating(), review.getContent(), review.getStatus(), review.getLikeCount(),
                liked, review.getCreatedAt());
    }

    private Review findReview(long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (review.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        return review;
    }

    private Review findPublishedReview(long reviewId) {
        Review review = findReview(reviewId);
        if (review.getStatus() != ReviewStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        return review;
    }

    private Review findReviewForUpdate(long reviewId) {
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (review.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        return review;
    }

    private Review findPublishedReviewForUpdate(long reviewId) {
        Review review = findReviewForUpdate(reviewId);
        if (review.getStatus() != ReviewStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        return review;
    }

    private void ensureActiveMerchant(long merchantId) {
        if (merchantRepository.findByIdAndStatus(merchantId, MerchantStatus.ACTIVE).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found");
        }
    }

    private void recalculateMerchantSummary(long merchantId) {
        reviewRepository.flush();
        reviewRepository.recalculateMerchantSummary(merchantId, Instant.now());
    }

    private String authorName(long userId) {
        return userAccountRepository.findById(userId).map(UserAccount::getDisplayName).orElse(null);
    }

    private static PageRequest pageRequest(int page, int size, Sort.Direction direction) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)), Sort.by(direction, "createdAt"));
    }
}
