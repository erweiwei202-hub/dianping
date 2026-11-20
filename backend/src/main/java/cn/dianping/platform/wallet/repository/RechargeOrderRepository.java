package cn.dianping.platform.wallet.repository;

import cn.dianping.platform.wallet.domain.RechargeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechargeOrderRepository extends JpaRepository<RechargeOrder, Long> {
}
