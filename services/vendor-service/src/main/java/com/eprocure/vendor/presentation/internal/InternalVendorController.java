package com.eprocure.vendor.presentation.internal;

import com.eprocure.vendor.application.service.InternalApiKeyGuard;
import com.eprocure.vendor.application.service.VendorPoSourceView;
import com.eprocure.vendor.application.usecase.GetVendorPoSourceUseCase;
import com.eprocure.vendor.common.api.ApiResponse;
import com.eprocure.vendor.common.api.RequestIdUtil;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/vendors")
public class InternalVendorController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalVendorController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final GetVendorPoSourceUseCase getVendorPoSourceUseCase;

    public InternalVendorController(
            InternalApiKeyGuard internalApiKeyGuard,
            GetVendorPoSourceUseCase getVendorPoSourceUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.getVendorPoSourceUseCase = getVendorPoSourceUseCase;
    }

    @GetMapping("/{id}/po-source")
    public ResponseEntity<ApiResponse<VendorPoSourceView>> getPoSource(
            @PathVariable UUID id,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/vendors/{}/po-source | userId=internal",
                LogMaskingUtil.maskId(id));
        return ResponseEntity.ok(ApiResponse.success(
                getVendorPoSourceUseCase.execute(id),
                RequestIdUtil.resolve(request)));
    }
}
