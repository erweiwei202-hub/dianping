package cn.dianping.platform.coupon.service;

import cn.dianping.platform.common.security.CurrentUserProvider;
import cn.dianping.platform.coupon.api.CouponOrderView;
import cn.dianping.platform.coupon.model.CouponCampaign;
import cn.dianping.platform.coupon.model.CouponOrder;
import cn.dianping.platform.coupon.model.CouponOrderStatus;
import cn.dianping.platform.coupon.repository.CouponCampaignRepository;
import cn.dianping.platform.coupon.repository.CouponOrderRepository;
import cn.dianping.platform.coupon.order.WalletDebitPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class CouponOrderService {

    private final CouponOrderRepository orders;
    private final CouponCampaignRepository campaigns;
    private final WalletDebitPort walletDebit;
    private final CurrentUserProvider currentUser;
    private final SeckillRedisGateway redisGateway;

    public CouponOrderService(CouponOrderRepository orders,
                              CouponCampaignRepository campaigns,
                              WalletDebitPort walletDebit,
                              CurrentUserProvider currentUser,
                              SeckillRedisGateway redisGateway) {
        this.orders = orders;
        this.campaigns = campaigns;
        this.walletDebit = walletDebit;
        this.currentUser = currentUser;
        this.redisGateway = redisGateway;
    }

    @Transactional(readOnly = true)
    public List<CouponOrderView> listMine(int page, int size) {
        long userId = currentUser.requiredUserId();
        return orders.findByUserIdOrderByCreatedAtDesc(userId,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)))
                .stream().map(CouponOrderView::from).toList();
    }

    @Transactional(readOnly = true)
    public CouponOrderView getMine(long orderId) {
        long userId = currentUser.requiredUserId();
        CouponOrder order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return CouponOrderView.from(order);
    }

    @Transactional
    public CouponOrderView payWithBalance(long orderId) {
        long userId = currentUser.requiredUserId();
        CouponOrder order = orders.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == CouponOrderStatus.PAID) {
            return CouponOrderView.from(order);
        }
        if (order.getStatus() != CouponOrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be paid in its current state");
        }
        Instant now = Instant.now();
        if (order.getPaymentExpiresAt() != null && !order.getPaymentExpiresAt().isAfter(now)) {
            closeExpiredOrder(order, now);
            return CouponOrderView.from(order);
        }
        if (order.getAmountMinor() > 0) {
            walletDebit.debitForOrder(userId, order.getId(), order.getAmountMinor());
        }
        order.markPaid(now);
        return CouponOrderView.from(order);
    }

    @Transactional
    public boolean closeExpiredOrder(long orderId, Instant now) {
        CouponOrder order = orders.findByIdAndStatusForUpdate(orderId, CouponOrderStatus.PENDING_PAYMENT)
                .orElse(null);
        if (order == null || !order.closeIfExpired(now)) {
            return false;
        }
        CouponCampaign campaign = campaigns.findByIdForUpdate(order.getCampaignId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        campaign.releaseOne();
        campaigns.save(campaign);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisGateway.releaseExpiredOrderStock(campaign.getId(), campaign.getTotalStock());
            }
        });
        return true;
    }

    private void closeExpiredOrder(CouponOrder order, Instant now) {
        if (!order.closeIfExpired(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is no longer payable");
        }
        CouponCampaign campaign = campaigns.findByIdForUpdate(order.getCampaignId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        campaign.releaseOne();
        campaigns.save(campaign);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisGateway.releaseExpiredOrderStock(campaign.getId(), campaign.getTotalStock());
            }
        });
    }
}
