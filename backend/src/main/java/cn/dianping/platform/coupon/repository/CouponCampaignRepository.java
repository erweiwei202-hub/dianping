package cn.dianping.platform.coupon.repository;

import cn.dianping.platform.coupon.model.CouponCampaign;
import cn.dianping.platform.coupon.model.CouponCampaignStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CouponCampaignRepository extends JpaRepository<CouponCampaign, Long> {

    Page<CouponCampaign> findByStatusAndEndsAtAfterOrderByStartsAtAsc(
            CouponCampaignStatus status, java.time.Instant now, Pageable pageable);

    List<CouponCampaign> findByStatusOrderByStartsAtAsc(CouponCampaignStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponCampaign c where c.id = :id")
    Optional<CouponCampaign> findByIdForUpdate(@Param("id") long id);

    List<CouponCampaign> findByStatusAndEndsAtBefore(CouponCampaignStatus status, Instant time);

    @Modifying(flushAutomatically = true)
    @Query("update CouponCampaign c set c.availableStock = c.availableStock - 1, c.version = c.version + 1 "
            + "where c.id = :id and c.availableStock > 0")
    int reserveAvailableStock(@Param("id") long id);
}
