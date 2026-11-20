package cn.dianping.platform.wallet.api;

import cn.dianping.platform.common.api.ApiResponse;
import cn.dianping.platform.common.security.CurrentUserProvider;
import cn.dianping.platform.wallet.domain.WalletAccount;
import cn.dianping.platform.wallet.domain.WalletLedgerEntry;
import cn.dianping.platform.wallet.repository.WalletAccountRepository;
import cn.dianping.platform.wallet.repository.WalletLedgerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final CurrentUserProvider currentUserProvider;
    private final WalletAccountRepository accountRepository;
    private final WalletLedgerRepository ledgerRepository;

    public WalletController(CurrentUserProvider currentUserProvider,
                            WalletAccountRepository accountRepository,
                            WalletLedgerRepository ledgerRepository) {
        this.currentUserProvider = currentUserProvider;
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @GetMapping
    public ApiResponse<WalletBalanceResponse> balance() {
        long userId = currentUserProvider.requiredUserId();
        WalletBalanceResponse response = accountRepository.findByUserId(userId)
                .map(this::toBalanceResponse)
                .orElseGet(() -> new WalletBalanceResponse(0, 0));
        return ApiResponse.success(response);
    }

    @GetMapping("/ledger")
    public ApiResponse<WalletLedgerPageResponse> ledger(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        long userId = currentUserProvider.requiredUserId();
        Page<WalletLedgerEntry> entries = ledgerRepository.findAllByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        List<WalletLedgerItemResponse> items = entries.getContent().stream()
                .map(this::toLedgerItem)
                .toList();
        WalletLedgerPageResponse response = new WalletLedgerPageResponse(
                items, entries.getNumber(), entries.getSize(), entries.getTotalElements(), entries.getTotalPages());
        return ApiResponse.success(response);
    }

    @PostMapping("/recharges")
    public ResponseEntity<ApiResponse<Void>> recharge(@Valid @RequestBody RechargeRequest request) {
        currentUserProvider.requiredUserId();
        ApiResponse<Void> response = new ApiResponse<>(false, null, "RECHARGE_DISABLED", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    private WalletBalanceResponse toBalanceResponse(WalletAccount account) {
        return new WalletBalanceResponse(account.getAvailableBalanceFen(), account.getFrozenBalanceFen());
    }

    private WalletLedgerItemResponse toLedgerItem(WalletLedgerEntry entry) {
        return new WalletLedgerItemResponse(entry.getId(), entry.getEntryType(), entry.getBusinessId(),
                entry.getAmountFen(), entry.getBalanceAfterFen(), entry.getDescription(), entry.getCreatedAt());
    }
}
