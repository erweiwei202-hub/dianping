package cn.dianping.platform.wallet.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class WalletInsufficientBalanceException extends ResponseStatusException {

    public WalletInsufficientBalanceException(long availableFen, long requiredFen) {
        super(HttpStatus.PAYMENT_REQUIRED,
                "Insufficient wallet balance: available " + availableFen + " fen, required " + requiredFen + " fen");
    }
}
