package cn.dianping.platform.wallet.repository;

import cn.dianping.platform.wallet.domain.WalletPaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletPaymentRecordRepository extends JpaRepository<WalletPaymentRecord, Long> {

    Optional<WalletPaymentRecord> findByOrderId(long orderId);
}
