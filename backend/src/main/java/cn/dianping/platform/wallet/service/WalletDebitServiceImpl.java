package cn.dianping.platform.wallet.service;

import cn.dianping.platform.wallet.domain.WalletAccount;
import cn.dianping.platform.wallet.domain.WalletLedgerEntry;
import cn.dianping.platform.wallet.domain.WalletLedgerType;
import cn.dianping.platform.wallet.domain.WalletPaymentRecord;
import cn.dianping.platform.wallet.repository.WalletAccountRepository;
import cn.dianping.platform.wallet.repository.WalletLedgerRepository;
import cn.dianping.platform.wallet.repository.WalletPaymentRecordRepository;
import cn.dianping.platform.wallet.service.exception.WalletDebitConflictException;
import cn.dianping.platform.wallet.service.exception.WalletInsufficientBalanceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletDebitServiceImpl implements WalletDebitService {

    private final WalletAccountRepository accountRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final WalletPaymentRecordRepository paymentRecordRepository;

    public WalletDebitServiceImpl(WalletAccountRepository accountRepository,
                                  WalletLedgerRepository ledgerRepository,
                                  WalletPaymentRecordRepository paymentRecordRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletDebitResult debitForOrder(long userId, long orderId, long amountMinor) {
        if (userId <= 0 || orderId <= 0) {
            throw new IllegalArgumentException("userId and orderId must be positive");
        }
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Paid order amount must be positive fen");
        }

        WalletAccount account = accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletInsufficientBalanceException(0, amountMinor));

        var previousPayment = paymentRecordRepository.findByOrderId(orderId);
        if (previousPayment.isPresent()) {
            WalletPaymentRecord payment = previousPayment.get();
            if (payment.getUserId() != userId || payment.getAmountFen() != amountMinor) {
                throw new WalletDebitConflictException(orderId);
            }
            return new WalletDebitResult(payment.getId(), orderId, amountMinor,
                    account.getAvailableBalanceFen(), true);
        }

        long currentBalance = account.getAvailableBalanceFen();
        if (currentBalance < amountMinor) {
            throw new WalletInsufficientBalanceException(currentBalance, amountMinor);
        }

        account.debit(amountMinor);
        long balanceAfter = account.getAvailableBalanceFen();
        WalletPaymentRecord payment = paymentRecordRepository.saveAndFlush(
                new WalletPaymentRecord(orderId, userId, amountMinor));
        ledgerRepository.save(WalletLedgerEntry.orderDebit(userId, orderId, amountMinor, balanceAfter));

        return new WalletDebitResult(payment.getId(), orderId, amountMinor, balanceAfter, false);
    }
}
