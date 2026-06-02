package com.eprocure.pr.infrastructure.finance;

import com.eprocure.pr.application.port.out.BudgetCheckPort;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.BudgetCheckStatus;
import com.eprocure.pr.domain.model.vo.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Primary
@ConditionalOnProperty(
        name = "eprocure.pr.integration.finance-enabled",
        havingValue = "true"
)
public class FinanceBudgetCheckAdapter implements BudgetCheckPort {
    private static final Logger log = LogManager.getLogger(FinanceBudgetCheckAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final ParameterizedTypeReference<ApiResponse<FinanceBudgetCheckResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient financeRestClient;
    private final String internalApiKey;
    private final String defaultGlAccountCode;

    public FinanceBudgetCheckAdapter(
            @Qualifier("financeRestClient") RestClient financeRestClient,
            @Value("${eprocure.pr.integration.finance.internal-api-key:}") String internalApiKey,
            @Value("${eprocure.pr.integration.finance.default-gl-account-code:6002}") String defaultGlAccountCode) {
        this.financeRestClient = financeRestClient;
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
        this.defaultGlAccountCode = defaultGlAccountCode == null || defaultGlAccountCode.isBlank()
                ? "6002"
                : defaultGlAccountCode.trim().toUpperCase();
    }

    @Override
    public BudgetCheckResult check(BudgetCheckQuery query) {
        if (internalApiKey.isBlank()) {
            log.error("[ACTION] Step FinanceBudgetCheck | prId={} | result=missing_api_key",
                    LogMaskingUtil.maskId(query.purchaseRequestId()));
            throw new BusinessException(ErrorCode.SYS_001);
        }
        try {
            ApiResponse<FinanceBudgetCheckResponse> response = financeRestClient.get()
                    .uri(builder -> builder
                            .path("/internal/budgets/check")
                            .queryParam("department_id", query.departmentId())
                            .queryParam("fiscal_year", query.fiscalYear())
                            .queryParam("amount", query.totalAmount().amount().toPlainString())
                            .queryParam("currency", query.totalAmount().currency())
                            .queryParam("gl_account_code", defaultGlAccountCode)
                            .build())
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(RESPONSE_TYPE);
            if (response == null || response.data() == null) {
                throw new BusinessException(ErrorCode.SYS_001);
            }
            FinanceBudgetCheckResponse data = response.data();
            log.info("[ACTION] Step FinanceBudgetCheck | prId={} | budgetId={} | status={}",
                    LogMaskingUtil.maskId(query.purchaseRequestId()),
                    LogMaskingUtil.maskId(data.budgetId()),
                    data.status());
            return data.toBudgetCheckResult();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] Step FinanceBudgetCheck | prId={} | status={}",
                    LogMaskingUtil.maskId(query.purchaseRequestId()),
                    exception.getStatusCode().value());
            int status = exception.getStatusCode().value();
            if (status == 404 || status == 409 || status == 422) {
                throw new BusinessException(ErrorCode.PR_002);
            }
            throw new BusinessException(ErrorCode.SYS_001);
        } catch (RestClientException exception) {
            log.error("[EXCEPTION][SYS_001] Finance budget check failed | prId={} | error={}",
                    LogMaskingUtil.maskId(query.purchaseRequestId()),
                    exception.getMessage());
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }

    public record FinanceMoneyResponse(BigDecimal amount, String currency) {
        Money toMoney() {
            return new Money(amount, currency);
        }
    }

    public record FinanceBudgetCheckResponse(
            UUID budgetId,
            UUID departmentId,
            int fiscalYear,
            Integer quarter,
            String glAccountCode,
            FinanceMoneyResponse allocated,
            FinanceMoneyResponse committed,
            FinanceMoneyResponse spent,
            FinanceMoneyResponse available,
            BudgetCheckStatus status,
            String warningMessage) {

        BudgetCheckResult toBudgetCheckResult() {
            return new BudgetCheckResult(
                    allocated.toMoney(),
                    committed.toMoney(),
                    spent.toMoney(),
                    available.toMoney(),
                    status,
                    warningMessage);
        }
    }
}
