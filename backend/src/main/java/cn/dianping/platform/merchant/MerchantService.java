package cn.dianping.platform.merchant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import cn.dianping.platform.merchant.service.MerchantOwnership;

@Service
public class MerchantService implements MerchantOwnership {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Transactional(readOnly = true)
    public Page<MerchantDtos.MerchantResponse> search(String keyword, String category, String region, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), boundedSize(size), Sort.by(Sort.Direction.ASC, "name"));
        return merchantRepository.searchActive(clean(keyword), clean(category), clean(region), pageable)
                .map(MerchantDtos.MerchantResponse::from);
    }

    @Transactional(readOnly = true)
    public MerchantDtos.MerchantResponse getActive(long merchantId) {
        Merchant merchant = merchantRepository.findByIdAndStatus(merchantId, MerchantStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        return MerchantDtos.MerchantResponse.from(merchant);
    }

    @Transactional
    public MerchantDtos.MerchantResponse create(MerchantDtos.CreateMerchantRequest request) {
        Merchant merchant = new Merchant(request.name().trim(), clean(request.category()), clean(request.region()),
                clean(request.address()), clean(request.description()), request.ownerUserId());
        return MerchantDtos.MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Transactional
    public MerchantDtos.MerchantResponse update(long merchantId, MerchantDtos.UpdateMerchantRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        MerchantStatus status = request.status() == null ? merchant.getStatus() : request.status();
        Long ownerUserId = request.ownerUserId() == null ? merchant.getOwnerUserId() : request.ownerUserId();
        merchant.update(request.name().trim(), clean(request.category()), clean(request.region()),
                clean(request.address()), clean(request.description()), status, ownerUserId);
        return MerchantDtos.MerchantResponse.from(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(long userId, long merchantId) {
        return merchantRepository.findById(merchantId)
                .map(merchant -> merchant.getOwnerUserId() != null && merchant.getOwnerUserId() == userId)
                .orElse(false);
    }

    private static int boundedSize(int size) {
        return Math.max(1, Math.min(size, 50));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
