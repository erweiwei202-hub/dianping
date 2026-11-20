package cn.dianping.platform.coupon.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CampaignCreateRequest(
        @NotNull @Positive Long merchantId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @Positive int totalStock,
        @PositiveOrZero long priceMinor,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {
}
