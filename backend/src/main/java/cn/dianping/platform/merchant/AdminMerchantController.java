package cn.dianping.platform.merchant;

import cn.dianping.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/merchants")
public class AdminMerchantController {

    private final MerchantService merchantService;

    public AdminMerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ApiResponse<MerchantDtos.MerchantResponse> create(@Valid @RequestBody MerchantDtos.CreateMerchantRequest request) {
        return ApiResponse.success(merchantService.create(request));
    }

    @PatchMapping("/{merchantId}")
    public ApiResponse<MerchantDtos.MerchantResponse> update(
            @PathVariable long merchantId,
            @Valid @RequestBody MerchantDtos.UpdateMerchantRequest request) {
        return ApiResponse.success(merchantService.update(merchantId, request));
    }
}
