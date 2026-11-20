package cn.dianping.platform.coupon.repository;

import cn.dianping.platform.coupon.model.CouponOrder;
import cn.dianping.platform.coupon.model.CouponOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CouponOrderRepository extends JpaRepository<CouponOrder, Long> {

    boolean existsByCampaignIdAndUserId(long campaignId, long userId);

    Optional<CouponOrder> findByRequestId(String requestId);

    Optional<CouponOrder> findByIdAndUserId(long id, long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CouponOrder o where o.id = :id and o.userId = :userId")
    Optional<CouponOrder> findByIdAndUserIdForUpdate(@Param("id") long id, @Param("userId") long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CouponOrder o where o.id = :id and o.status = :status")
    Optional<CouponOrder> findByIdAndStatusForUpdate(@Param("id") long id,
                                                     @Param("status") CouponOrderStatus status);

    List<CouponOrder> findByStatusAndPaymentExpiresAtLessThanEqualOrderByPaymentExpiresAtAsc(
            CouponOrderStatus status, Instant time, Pageable pageable);

    List<CouponOrder> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}
