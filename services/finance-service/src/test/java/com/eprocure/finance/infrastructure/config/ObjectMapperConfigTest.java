package com.eprocure.finance.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.infrastructure.persistence.entity.BudgetLedgerSummaryDbEntity;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObjectMapperConfigTest {

    @Test
    void should_convert_budget_ledger_summary_entity_to_domain_when_money_is_exposed_by_getters() {
        BudgetLedgerSummaryDbEntity entity = new BudgetLedgerSummaryDbEntity();
        entity.setId(UUID.fromString("70000000-0000-4000-8000-000000000001"));
        entity.setDepartmentId(UUID.fromString("33333333-3333-3333-8333-333333333333"));
        entity.setFiscalYear(2026);
        entity.setQuarter(null);
        entity.setGlAccountCode("6002");
        entity.setAllocatedAmount(new BigDecimal("500000000.0000"));
        entity.setCommittedAmount(new BigDecimal("100000000.0000"));
        entity.setSpentAmount(new BigDecimal("50000000.0000"));
        entity.setCurrency("VND");
        entity.setStatus(BudgetStatus.ACTIVE);

        BudgetLedgerSummary summary = new ObjectMapperConfig()
                .domainObjectMapper()
                .convertValue(entity, BudgetLedgerSummary.class);

        assertThat(summary.allocated().amount()).isEqualByComparingTo("500000000.0000");
        assertThat(summary.committed().amount()).isEqualByComparingTo("100000000.0000");
        assertThat(summary.spent().amount()).isEqualByComparingTo("50000000.0000");
        assertThat(summary.available().amount()).isEqualByComparingTo("350000000.0000");
    }
}
