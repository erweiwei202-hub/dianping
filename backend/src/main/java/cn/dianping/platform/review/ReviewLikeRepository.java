package cn.dianping.platform.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO review_like (review_id, user_id, created_at) VALUES (:reviewId, :userId, CURRENT_TIMESTAMP(6))", nativeQuery = true)
    int insertIfAbsent(@Param("reviewId") long reviewId, @Param("userId") long userId);

    @Modifying
    @Query(value = "DELETE FROM review_like WHERE review_id = :reviewId AND user_id = :userId", nativeQuery = true)
    int deleteRelation(@Param("reviewId") long reviewId, @Param("userId") long userId);

    boolean existsByReviewIdAndUserId(long reviewId, long userId);

    long countByReviewId(long reviewId);

    @Query("select l.reviewId from ReviewLike l where l.userId = :userId and l.reviewId in :reviewIds")
    Set<Long> findLikedReviewIds(@Param("userId") long userId, @Param("reviewIds") Collection<Long> reviewIds);

    @Modifying
    @Query(value = "DELETE FROM review_like WHERE review_id = :reviewId", nativeQuery = true)
    int deleteAllForReview(@Param("reviewId") long reviewId);
}
