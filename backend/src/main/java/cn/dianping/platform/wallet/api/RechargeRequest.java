package cn.dianping.platform.wallet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RechargeRequest(
        @Positive long amountFen,
        @NotBlank String channel,
        @NotBlank String idempotencyKey) {
}
