package com.eprocure.admin.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eprocure.admin.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
import com.eprocure.admin.application.port.out.IamDepartmentAdminPort;
import com.eprocure.admin.application.service.DepartmentAdminView;
import com.eprocure.admin.application.service.IdempotencyGuard;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString(IDEMPOTENCY_KEY);

    @Mock
    private IamDepartmentAdminPort port;

    private CreateDepartmentUseCase createUseCase;
    private UpdateDepartmentUseCase updateUseCase;
    private DeactivateDepartmentUseCase deactivateUseCase;

    @BeforeEach
    void setUp() {
        IdempotencyGuard idempotencyGuard = new IdempotencyGuard();
        createUseCase = new CreateDepartmentUseCase(port, idempotencyGuard);
        updateUseCase = new UpdateDepartmentUseCase(port, idempotencyGuard);
        deactivateUseCase = new DeactivateDepartmentUseCase(port, idempotencyGuard);
    }

    @Test
    @DisplayName("Forward create department sang IAM port")
    void should_delegate_create_to_iam_port_when_command_is_valid() {
        ManageDepartmentCommand command = new ManageDepartmentCommand(
                ACTOR_ID,
                null,
                "LEGAL",
                "Legal",
                PARENT_ID,
                null,
                "642");
        when(port.createDepartment(command, IDEMPOTENCY_UUID)).thenReturn(departmentView(false));

        createUseCase.execute(command, IDEMPOTENCY_KEY);

        verify(port).createDepartment(command, IDEMPOTENCY_UUID);
    }

    @Test
    @DisplayName("Forward update department sang IAM port")
    void should_delegate_update_to_iam_port_when_command_is_valid() {
        ManageDepartmentCommand command = new ManageDepartmentCommand(
                ACTOR_ID,
                DEPARTMENT_ID,
                "LEGAL",
                "Legal Affairs",
                PARENT_ID,
                null,
                null);
        when(port.updateDepartment(command, IDEMPOTENCY_UUID)).thenReturn(departmentView(false));

        updateUseCase.execute(command, IDEMPOTENCY_KEY);

        verify(port).updateDepartment(command, IDEMPOTENCY_UUID);
    }

    @Test
    @DisplayName("Forward deactivate department sang IAM port")
    void should_delegate_deactivate_to_iam_port_when_command_is_valid() {
        DeactivateDepartmentCommand command = new DeactivateDepartmentCommand(ACTOR_ID, DEPARTMENT_ID);
        when(port.deactivateDepartment(command, IDEMPOTENCY_UUID)).thenReturn(departmentView(true));

        deactivateUseCase.execute(command, IDEMPOTENCY_KEY);

        verify(port).deactivateDepartment(command, IDEMPOTENCY_UUID);
    }

    private static DepartmentAdminView departmentView(boolean deleted) {
        return new DepartmentAdminView(DEPARTMENT_ID, "LEGAL", "Legal", PARENT_ID, null, 0, 0, deleted);
    }
}
