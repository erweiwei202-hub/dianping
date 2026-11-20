package cn.dianping.platform.wallet.service;

import cn.dianping.platform.wallet.domain.WalletAccount;
import cn.dianping.platform.wallet.repository.WalletAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletAccountProvisioningService {

    private final WalletAccountRepository accountRepository;

    public WalletAccountProvisioningService(WalletAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void ensureAccountForUser(long userId) {
        if (!accountRepository.existsByUserId(userId)) {
            accountRepository.save(new WalletAccount(userId));
        }
    }
}
