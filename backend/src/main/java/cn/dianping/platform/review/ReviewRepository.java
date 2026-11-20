package cn.dianping.platform.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByMerchantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            long merchantId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(long userId, Pageable pageable);

    Page<Review> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);

    List<Review> findByIdIn(Collection<Long> ids);

    @Query("select r.rating as rating, count(r.id) as reviewCount from Review r " +
            "where r.merchantId = :merchantId and r.status = :status and r.deletedAt is null group by r.rating")
    List<RatingCount> findRatingDistribution(@Param("merchantId") long merchantId, @Param("status") ReviewStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :reviewId")
    Optional<Review> findByIdForUpdate(@Param("reviewId") long reviewId);

    @Modifying
    @Query(value = "UPDATE review SET like_count = like_count + 1, updated_at = :now WHERE id = :reviewId", nativeQuery = true)
    int incrementLikeCount(@Param("reviewId") long reviewId, @Param("now") Instant now);

    @Modifying
    @Query(value = "UPDATE review SET like_count = GREATEST(like_count - 1, 0), updated_at = :now WHERE id = :reviewId", nativeQuery = true)
    int decrementLikeCount(@Param("reviewId") long reviewId, @Param("now") Instant now);

    @Modifying
    @Query(value = "UPDATE merchant " +
            "SET review_count = (SELECT COUNT(*) FROM review r WHERE r.merchant_id = :merchantId AND r.status = 'PUBLISHED' AND r.deleted_at IS NULL), " +
            "average_rating = (SELECT COALESCE(AVG(r.rating), 0) FROM review r WHERE r.merchant_id = :merchantId AND r.status = 'PUBLISHED' AND r.deleted_at IS NULL), " +
            "updated_at = :now WHERE id = :merchantId", nativeQuery = true)
    int recalculateMerchantSummary(@Param("merchantId") long merchantId, @Param("now") Instant now);

    interface RatingCount {
        short getRating();

        long getReviewCount();
    }
}
