package cn.dianping.platform.coupon.repository;

import cn.dianping.platform.coupon.model.SeckillRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface SeckillRequestRepository extends JpaRepository<SeckillRequest, String> {

    Optional<SeckillRequest> findByRequestIdAndUserId(String requestId, long userId);

    List<SeckillRequest> findByCampaignIdAndUserIdOrderByRequestIdDesc(long campaignId, long userId);

    long countByCampaignIdAndStatusIn(long campaignId, List<cn.dianping.platform.coupon.model.SeckillRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SeckillRequest r where r.requestId = :requestId")
    Optional<SeckillRequest> findByRequestIdForUpdate(@Param("requestId") String requestId);
}
