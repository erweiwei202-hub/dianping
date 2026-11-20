package cn.dianping.platform.coupon.service;

import cn.dianping.platform.common.security.CurrentUserProvider;
import cn.dianping.platform.coupon.api.CampaignCreateRequest;
import cn.dianping.platform.coupon.api.CampaignView;
import cn.dianping.platform.coupon.model.CouponCampaign;
import cn.dianping.platform.coupon.model.CouponCampaignStatus;
import cn.dianping.platform.coupon.repository.CouponCampaignRepository;
import cn.dianping.platform.merchant.service.MerchantOwnership;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CouponCampaignService {

    private final CouponCampaignRepository campaigns;
    private final MerchantOwnership merchantOwnership;
    private final CurrentUserProvider currentUser;
    private final SeckillRedisGateway redisGateway;

    public CouponCampaignService(CouponCampaignRepository campaigns,
                                 MerchantOwnership merchantOwnership,
                                 CurrentUserProvider currentUser,
                                 SeckillRedisGateway redisGateway) {
        this.campaigns = campaigns;
        this.merchantOwnership = merchantOwnership;
        this.currentUser = currentUser;
        this.redisGateway = redisGateway;
    }

    @Transactional
    public CampaignView create(CampaignCreateRequest request) {
        requireAdminOrOwner(request.merchantId());
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new ResponseStatusException(BAD_REQUEST, "startsAt must be before endsAt");
        }
        CouponCampaign campaign = new CouponCampaign(request.merchantId(), request.name(), request.description(),
                request.totalStock(), request.priceMinor(), request.startsAt(), request.endsAt());
        return CampaignView.from(campaigns.save(campaign));
    }

    @Transactional
    public CampaignView publish(long campaignId) {
        CouponCampaign campaign = campaigns.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Campaign not found"));
        requireAdminOrOwner(campaign.getMerchantId());
        campaign.activate();
        campaigns.save(campaign);
        CampaignView view = CampaignView.from(campaign);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisGateway.initializeCampaign(view);
            }
        });
        return view;
    }

    @Transactional
    public CampaignView takeOffline(long campaignId) {
        CouponCampaign campaign = campaigns.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Campaign not found"));
        requireAdminOrOwner(campaign.getMerchantId());
        campaign.offline();
        campaigns.save(campaign);
        CampaignView view = CampaignView.from(campaign);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisGateway.markOffline(campaignId);
            }
        });
        return view;
    }

    @Transactional(readOnly = true)
    public CampaignView get(long campaignId) {
        CouponCampaign campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Campaign not found"));
        if (campaign.getStatus() != CouponCampaignStatus.ACTIVE) {
            throw new ResponseStatusException(NOT_FOUND, "Campaign not found");
        }
        return CampaignView.from(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignView> listActive(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return campaigns.findByStatusAndEndsAtAfterOrderByStartsAtAsc(
                        CouponCampaignStatus.ACTIVE, Instant.now(), pageable)
                .stream().map(CampaignView::from).toList();
    }

    private void requireAdminOrOwner(long merchantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals)) {
            return;
        }
        if (!merchantOwnership.isOwner(currentUser.requiredUserId(), merchantId)) {
            throw new AccessDeniedException("You cannot manage this merchant's campaigns");
        }
    }
}
