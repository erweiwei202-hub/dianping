package cn.dianping.platform.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class MerchantDtos {

    private MerchantDtos() {
    }

    public record CreateMerchantRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80) String category,
            @Size(max = 100) String region,
            @Size(max = 255) String address,
            @Size(max = 1000) String description,
            Long ownerUserId) {
    }

    public record UpdateMerchantRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80) String category,
            @Size(max = 100) String region,
            @Size(max = 255) String address,
            @Size(max = 1000) String description,
            MerchantStatus status,
            Long ownerUserId) {
    }

    public record MerchantResponse(
            long id,
            String name,
            String category,
            String region,
            String address,
            String description,
            BigDecimal averageRating,
            long reviewCount,
            MerchantStatus status,
            Instant createdAt) {
        public static MerchantResponse from(Merchant merchant) {
            return new MerchantResponse(
                    merchant.getId(), merchant.getName(), merchant.getCategory(), merchant.getRegion(),
                    merchant.getAddress(), merchant.getDescription(), merchant.getAverageRating(),
                    merchant.getReviewCount(), merchant.getStatus(), merchant.getCreatedAt());
        }
    }
}
