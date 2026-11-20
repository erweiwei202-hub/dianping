package cn.dianping.platform.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import cn.dianping.platform.wallet.service.WalletAccountProvisioningService;

@Service
public class IdentityService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletAccountProvisioningService walletAccountProvisioningService;

    public IdentityService(UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder,
                           WalletAccountProvisioningService walletAccountProvisioningService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletAccountProvisioningService = walletAccountProvisioningService;
    }

    @Transactional
    public UserAccount register(IdentityDtos.RegisterRequest request) {
        String normalizedUsername = request.username().trim().toLowerCase();
        if (userAccountRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
        UserAccount account = new UserAccount(
                normalizedUsername,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                UserRole.CONSUMER);
        UserAccount registered = userAccountRepository.saveAndFlush(account);
        walletAccountProvisioningService.ensureAccountForUser(registered.getId());
        return registered;
    }
}
