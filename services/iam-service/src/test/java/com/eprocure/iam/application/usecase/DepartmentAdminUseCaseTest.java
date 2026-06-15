package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eprocure.iam.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.iam.application.port.in.ManageDepartmentCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentAdminUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    private CreateDepartmentUseCase createUseCase;
    private UpdateDepartmentUseCase updateUseCase;
    private DeactivateDepartmentUseCase deactivateUseCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        IdempotencyGuard idempotencyGuard = new IdempotencyGuard();
        createUseCase = new CreateDepartmentUseCase(departmentRepository, userRepository, idempotencyGuard, clock);
        updateUseCase = new UpdateDepartmentUseCase(departmentRepository, userRepository, idempotencyGuard, clock);
        deactivateUseCase = new DeactivateDepartmentUseCase(departmentRepository, idempotencyGuard, clock);
    }

    @Test
    @DisplayName("Tạo phòng ban khi command hợp lệ")
    void should_create_department_when_command_is_valid() {
        when(departmentRepository.findById(PARENT_ID)).thenReturn(Optional.of(parentDepartment()));

        var result = createUseCase.execute(
                new ManageDepartmentCommand(ACTOR_ID, null, "legal", "Legal", PARENT_ID, null),
                IDEMPOTENCY_KEY);

        assertThat(result.code()).isEqualTo("LEGAL");
        assertThat(result.name()).isEqualTo("Legal");
        assertThat(result.parentId()).isEqualTo(PARENT_ID);
        verify(departmentRepository).save(any(Department.class), eq(ACTOR_ID));
    }

    @Test
    @DisplayName("Ném IAM_009 khi department code đã tồn tại")
    void should_throw_conflict_when_department_code_exists() {
        when(departmentRepository.existsActiveByCode("LEGAL")).thenReturn(true);

        assertThatThrownBy(() -> createUseCase.execute(
                new ManageDepartmentCommand(ACTOR_ID, null, "LEGAL", "Legal", null, null),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.IAM_009));
    }

    @Test
    @DisplayName("Ném IAM_005 khi update parent tạo vòng lặp")
    void should_throw_validation_when_parent_is_descendant() {
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(activeDepartment()));
        when(departmentRepository.findById(PARENT_ID)).thenReturn(Optional.of(parentDepartment()));
        when(departmentRepository.isDescendant(PARENT_ID, DEPARTMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> updateUseCase.execute(
                new ManageDepartmentCommand(ACTOR_ID, DEPARTMENT_ID, "LEGAL", "Legal", PARENT_ID, null),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.IAM_005));
    }

    @Test
    @DisplayName("Ném IAM_037 khi deactivate department còn active member")
    void should_throw_when_deactivating_department_with_active_members() {
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(activeDepartment()));
        when(departmentRepository.countActiveMembersByDepartmentId(DEPARTMENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> deactivateUseCase.execute(
                new DeactivateDepartmentCommand(ACTOR_ID, DEPARTMENT_ID),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.IAM_037));
    }

    @Test
    @DisplayName("Deactivate phòng ban khi không còn active dependency")
    void should_deactivate_department_when_no_active_dependencies() {
        when(departmentRepository.findById(DEPARTMENT_ID)).thenReturn(Optional.of(activeDepartment()));
        when(departmentRepository.findByIdIncludingInactive(DEPARTMENT_ID)).thenReturn(Optional.of(deletedDepartment()));

        var result = deactivateUseCase.execute(
                new DeactivateDepartmentCommand(ACTOR_ID, DEPARTMENT_ID),
                IDEMPOTENCY_KEY);

        assertThat(result.deleted()).isTrue();
        verify(departmentRepository).deactivate(DEPARTMENT_ID, ACTOR_ID, NOW);
    }

    private static Department activeDepartment() {
        return Department.create(DEPARTMENT_ID, "LEGAL", "Legal", null, null, NOW);
    }

    private static Department parentDepartment() {
        return Department.create(PARENT_ID, "HQ", "Headquarters", NOW);
    }

    private static Department deletedDepartment() {
        return Department.reconstitute(DEPARTMENT_ID, "LEGAL", "Legal", null, null, NOW, NOW, true);
    }
}
