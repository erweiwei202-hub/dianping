package cn.dianping.platform.wallet.repository;

import cn.dianping.platform.wallet.domain.WalletLedgerEntry;
import cn.dianping.platform.wallet.domain.WalletLedgerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, Long> {

    Optional<WalletLedgerEntry> findByUserIdAndEntryTypeAndBusinessId(
            long userId, WalletLedgerType entryType, String businessId);

    Page<WalletLedgerEntry> findAllByUserId(long userId, Pageable pageable);
}
