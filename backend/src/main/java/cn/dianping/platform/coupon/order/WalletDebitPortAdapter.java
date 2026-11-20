package cn.dianping.platform.coupon.order;

import cn.dianping.platform.wallet.service.WalletDebitService;
import org.springframework.stereotype.Component;

@Component
public class WalletDebitPortAdapter implements WalletDebitPort {

    private final WalletDebitService walletDebitService;

    public WalletDebitPortAdapter(WalletDebitService walletDebitService) {
        this.walletDebitService = walletDebitService;
    }

    @Override
    public void debitForOrder(long userId, long orderId, long amountMinor) {
        walletDebitService.debitForOrder(userId, orderId, amountMinor);
    }
}
