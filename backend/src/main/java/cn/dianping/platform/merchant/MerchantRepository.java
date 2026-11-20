package cn.dianping.platform.merchant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    @Query("""
            select m from Merchant m
            where m.status = cn.dianping.platform.merchant.MerchantStatus.ACTIVE
              and (:keyword is null or lower(m.name) like lower(concat('%', :keyword, '%')))
              and (:category is null or m.category = :category)
              and (:region is null or m.region = :region)
            """)
    Page<Merchant> searchActive(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("region") String region,
            Pageable pageable);

    Optional<Merchant> findByIdAndStatus(Long id, MerchantStatus status);
}
