package cn.dianping.platform.wallet.repository;

import cn.dianping.platform.wallet.domain.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    Optional<WalletAccount> findByUserId(long userId);

    boolean existsByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from WalletAccount account where account.userId = :userId")
    Optional<WalletAccount> findByUserIdForUpdate(@Param("userId") long userId);
}
