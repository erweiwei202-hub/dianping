package cn.dianping.platform.coupon.repository;

import cn.dianping.platform.coupon.model.SeckillOutbox;
import cn.dianping.platform.coupon.model.SeckillOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeckillOutboxRepository extends JpaRepository<SeckillOutbox, Long> {

    List<SeckillOutbox> findByStatusOrderByCreatedAtAsc(SeckillOutboxStatus status, Pageable pageable);

    Optional<SeckillOutbox> findByRequestId(String requestId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SeckillOutbox o where o.requestId = :requestId")
    Optional<SeckillOutbox> findByRequestIdForUpdate(@Param("requestId") String requestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeckillOutbox o set o.status = :sending, o.attempts = o.attempts + 1 where o.id = :id and o.status = :newStatus")
    int claim(@Param("id") long id,
              @Param("newStatus") SeckillOutboxStatus newStatus,
              @Param("sending") SeckillOutboxStatus sending);

    @Modifying
    @Query("update SeckillOutbox o set o.status = :newStatus where o.status = :sending and o.updatedAt < :before")
    int resetStale(@Param("sending") SeckillOutboxStatus sending,
                   @Param("newStatus") SeckillOutboxStatus newStatus,
                   @Param("before") Instant before);
}
