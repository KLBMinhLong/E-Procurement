package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eprocure.admin.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.admin.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.admin.application.port.out.ProcurementCatalogAdminPort;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import com.eprocure.admin.application.service.IdempotencyGuard;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogCategoryUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString(IDEMPOTENCY_KEY);

    @Mock
    private ProcurementCatalogAdminPort procurementCatalogAdminPort;

    private CreateCatalogCategoryUseCase createUseCase;
    private ListCatalogCategoriesUseCase listUseCase;
    private DeactivateCatalogCategoryUseCase deactivateUseCase;

    @BeforeEach
    void setUp() {
        createUseCase = new CreateCatalogCategoryUseCase(procurementCatalogAdminPort, new IdempotencyGuard());
        listUseCase = new ListCatalogCategoriesUseCase(procurementCatalogAdminPort);
        deactivateUseCase = new DeactivateCatalogCategoryUseCase(procurementCatalogAdminPort, new IdempotencyGuard());
    }

    @Test
    @DisplayName("List catalog categories delegate sang procurement port")
    void should_delegate_when_listing_categories() {
        given(procurementCatalogAdminPort.listCategories(true)).willReturn(List.of(category()));

        var result = listUseCase.execute(true);

        assertThat(result).hasSize(1);
        verify(procurementCatalogAdminPort).listCategories(true);
    }

    @Test
    @DisplayName("Create category validate Idempotency-Key và delegate sang procurement port")
    void should_delegate_create_with_idempotency_key() {
        var command = command();
        given(procurementCatalogAdminPort.createCategory(command, IDEMPOTENCY_UUID)).willReturn(category());

        var result = createUseCase.execute(command, IDEMPOTENCY_KEY);

        assertThat(result.code()).isEqualTo("OPS_SERVICE");
        verify(procurementCatalogAdminPort).createCategory(command, IDEMPOTENCY_UUID);
    }

    @Test
    @DisplayName("Deactivate category validate Idempotency-Key và delegate sang procurement port")
    void should_delegate_deactivate_with_idempotency_key() {
        var command = new DeactivateCatalogCategoryCommand(ACTOR_ID, "OPS_SERVICE");
        given(procurementCatalogAdminPort.deactivateCategory(command, IDEMPOTENCY_UUID)).willReturn(category());

        var result = deactivateUseCase.execute(command, IDEMPOTENCY_KEY);

        assertThat(result.code()).isEqualTo("OPS_SERVICE");
        verify(procurementCatalogAdminPort).deactivateCategory(command, IDEMPOTENCY_UUID);
    }

    private ManageCatalogCategoryCommand command() {
        return new ManageCatalogCategoryCommand(
                ACTOR_ID,
                "OPS_SERVICE",
                "Operational Service",
                null,
                true,
                "PR_APPROVE_L2",
                new BigDecimal("10000000.0000"),
                false);
    }

    private CatalogCategoryAdminView category() {
        return new CatalogCategoryAdminView(
                "OPS_SERVICE",
                "Operational Service",
                null,
                true,
                "PR_APPROVE_L2",
                "10000000.0000",
                false,
                0,
                false);
    }
}
