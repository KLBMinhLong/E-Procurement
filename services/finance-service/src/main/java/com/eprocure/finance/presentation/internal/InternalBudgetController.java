package com.eprocure.finance.presentation.internal;

import com.eprocure.finance.application.port.in.CheckBudgetCommand;
import com.eprocure.finance.application.service.BudgetCheckView;
import com.eprocure.finance.application.service.InternalApiKeyGuard;
import com.eprocure.finance.application.usecase.CheckBudgetUseCase;
import com.eprocure.finance.common.api.ApiResponse;
import com.eprocure.finance.common.api.RequestIdUtil;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.vo.Money;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/budgets")
public class InternalBudgetController {
    private static final Logger log = LogManager.getLogger(InternalBudgetController.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final CheckBudgetUseCase checkBudgetUseCase;

    public InternalBudgetController(
            InternalApiKeyGuard internalApiKeyGuard,
            CheckBudgetUseCase checkBudgetUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.checkBudgetUseCase = checkBudgetUseCase;
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<BudgetCheckView>> checkBudget(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestParam("department_id") UUID departmentId,
            @RequestParam("fiscal_year") int fiscalYear,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "currency", defaultValue = "VND") String currency,
            @RequestParam(value = "gl_account_code", required = false) String glAccountCode,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/budgets/check | departmentId={}",
                LogMaskingUtil.maskId(departmentId));
        BudgetCheckView view = checkBudgetUseCase.execute(new CheckBudgetCommand(
                departmentId,
                fiscalYear,
                glAccountCode,
                new Money(amount, currency)));
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }
}
