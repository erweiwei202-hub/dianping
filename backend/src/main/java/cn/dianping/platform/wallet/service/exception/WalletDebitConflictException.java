package cn.dianping.platform.wallet.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class WalletDebitConflictException extends ResponseStatusException {

    public WalletDebitConflictException(long orderId) {
        super(HttpStatus.CONFLICT, "Wallet payment already exists with a different user or amount for order " + orderId);
    }
}
