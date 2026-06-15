package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.pr.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.domain.model.CatalogCategory;
import com.eprocure.pr.domain.model.CatalogCategoryAdmin;
import com.eprocure.pr.domain.repository.CatalogCategoryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CatalogCategoryAdminUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    private FakeCatalogCategoryRepository repository;
    private FakeIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        repository = new FakeCatalogCategoryRepository();
        idempotencyService = new FakeIdempotencyService();
    }

    @Test
    @DisplayName("Tạo catalog category khi command hợp lệ")
    void should_create_catalog_category_when_command_is_valid() {
        var useCase = new CreateCatalogCategoryUseCase(
                repository,
                idempotencyService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(new ManageCatalogCategoryCommand(
                ACTOR_ID,
                "OPS_SERVICE",
                "Operational Service",
                null,
                true,
                "PR_APPROVE_L2",
                new BigDecimal("10000000.0000"),
                false), IDEMPOTENCY_KEY);

        assertThat(result.code()).isEqualTo("OPS_SERVICE");
        assertThat(result.requiresSpecialApproval()).isTrue();
        assertThat(result.requiresRfqAbove()).isEqualTo("10000000.0000");
        assertThat(repository.createdBy).isEqualTo(ACTOR_ID);
        assertThat(idempotencyService.savedResponse).isEqualTo(result);
    }

    @Test
    @DisplayName("Ném PR_017 khi category đã tồn tại")
    void should_throw_when_category_already_exists() {
        repository.categories.put("OPS_SERVICE", category("OPS_SERVICE", 0, false));
        var useCase = new CreateCatalogCategoryUseCase(
                repository,
                idempotencyService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> useCase.execute(new ManageCatalogCategoryCommand(
                ACTOR_ID,
                "OPS_SERVICE",
                "Operational Service",
                null,
                false,
                null,
                null,
                false), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode()).isEqualTo(ErrorCode.PR_017));
    }

    @Test
    @DisplayName("Deactivate category khi không còn active item")
    void should_deactivate_category_when_no_active_items_exist() {
        repository.categories.put("OPS_SERVICE", category("OPS_SERVICE", 0, false));
        var useCase = new DeactivateCatalogCategoryUseCase(
                repository,
                idempotencyService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(new DeactivateCatalogCategoryCommand(ACTOR_ID, "OPS_SERVICE"), IDEMPOTENCY_KEY);

        assertThat(result.isDeleted()).isTrue();
        assertThat(repository.deactivatedCode).isEqualTo("OPS_SERVICE");
        assertThat(repository.deletedAt).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Ném PR_018 khi deactivate category còn active item")
    void should_throw_when_category_has_active_items() {
        repository.categories.put("OPS_SERVICE", category("OPS_SERVICE", 3, false));
        var useCase = new DeactivateCatalogCategoryUseCase(
                repository,
                idempotencyService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> useCase.execute(
                new DeactivateCatalogCategoryCommand(ACTOR_ID, "OPS_SERVICE"),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode()).isEqualTo(ErrorCode.PR_018));
    }

    private static CatalogCategoryAdmin category(String code, long itemCount, boolean deleted) {
        return new CatalogCategoryAdmin(
                code,
                "Operational Service",
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                false,
                itemCount,
                deleted);
    }

    private static final class FakeCatalogCategoryRepository implements CatalogCategoryRepository {
        private final Map<String, CatalogCategoryAdmin> categories = new HashMap<>();
        private UUID createdBy;
        private String deactivatedCode;
        private Instant deletedAt;

        @Override
        public List<CatalogCategory> findAll() {
            return List.of();
        }

        @Override
        public List<CatalogCategoryAdmin> findAdminCategories(boolean includeInactive) {
            return categories.values().stream()
                    .filter(category -> includeInactive || !category.deleted())
                    .toList();
        }

        @Override
        public Optional<CatalogCategoryAdmin> findAdminByCode(String code) {
            return Optional.ofNullable(categories.get(code));
        }

        @Override
        public boolean existsByCode(String code) {
            return categories.containsKey(code);
        }

        @Override
        public boolean existsActiveByCode(String code) {
            return Optional.ofNullable(categories.get(code))
                    .map(category -> !category.deleted())
                    .orElse(false);
        }

        @Override
        public long countActiveItems(String categoryCode) {
            return Optional.ofNullable(categories.get(categoryCode))
                    .map(CatalogCategoryAdmin::itemCount)
                    .orElse(0L);
        }

        @Override
        public void create(CatalogCategory category, UUID actorId) {
            this.createdBy = actorId;
            categories.put(category.getCode(), new CatalogCategoryAdmin(
                    category.getCode(),
                    category.getName(),
                    category.getParentCode(),
                    category.isRequiresSpecialApproval(),
                    category.getSpecialApproverRole(),
                    category.getRequiresRfqAbove(),
                    category.isCapex(),
                    0,
                    false));
        }

        @Override
        public void update(CatalogCategory category, UUID actorId) {
            categories.put(category.getCode(), new CatalogCategoryAdmin(
                    category.getCode(),
                    category.getName(),
                    category.getParentCode(),
                    category.isRequiresSpecialApproval(),
                    category.getSpecialApproverRole(),
                    category.getRequiresRfqAbove(),
                    category.isCapex(),
                    countActiveItems(category.getCode()),
                    false));
        }

        @Override
        public void deactivate(String code, UUID actorId, Instant deletedAt) {
            this.deactivatedCode = code;
            this.deletedAt = deletedAt;
            CatalogCategoryAdmin current = categories.get(code);
            categories.put(code, new CatalogCategoryAdmin(
                    current.code(),
                    current.name(),
                    current.parentCode(),
                    current.requiresSpecialApproval(),
                    current.specialApproverRole(),
                    current.requiresRfqAbove(),
                    current.capex(),
                    current.itemCount(),
                    true));
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private Object savedResponse;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            this.savedResponse = response;
        }
    }
}
