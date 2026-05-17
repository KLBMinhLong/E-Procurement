## SK-15 · JUnit Unit Test

### Trigger
Agent tạo unit test cho domain, use case, hoặc service.

### Inputs Required
- Target class under test
- Dependencies để mock
- TestDataFactory inputs

### Rules
```
[R1] Test method name: should_{expected}_when_{condition}
[R2] @DisplayName với tiếng Việt — dễ đọc trong CI report
[R3] Given / When / Then rõ ràng — 3 block comments
[R4] TestDataFactory class riêng — không build test data inline
[R5] Coverage target: ≥ 70% cho domain + application layer
[R6] Không test private method trực tiếp
[R7] Mỗi test: test một điều duy nhất (Single Assertion principle)
[R8] Mockito: @ExtendWith(MockitoExtension.class) — không dùng @SpringBootTest cho unit test
```

### Template
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePurchaseRequestUseCase")
class CreatePurchaseRequestUseCaseTest {

    @Mock PurchaseRequestRepository repository;
    @Mock IdempotencyService idempotencyService;
    @Mock BudgetServicePort budgetServicePort;
    @Mock PrNumberGenerator prNumberGenerator;

    @InjectMocks CreatePurchaseRequestUseCase useCase;

    @Test
    @DisplayName("Tạo PR thành công với dữ liệu hợp lệ")
    void should_create_pr_when_valid_command() {
        // Given
        CreatePrCommand command = TestDataFactory.validCreatePrCommand();
        given(idempotencyService.getOrExecute(any(), any(), any()))
            .willAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
        given(prNumberGenerator.next()).willReturn("PR-2025-01-00001");
        given(budgetServicePort.check(any(), any(), any(), any()))
            .willReturn(BudgetCheckResult.passed(BigDecimal.valueOf(10_000_000)));

        // When
        PurchaseRequest result = useCase.execute(command, "idem-key-123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrNumber()).isEqualTo("PR-2025-01-00001");
        assertThat(result.getStatus()).isEqualTo(PrStatus.DRAFT);
        verify(repository).save(argThat(pr -> pr.getRequesterId().equals(command.getRequesterId())));
    }

    @Test
    @DisplayName("Ném InsufficientBudgetException khi ngân sách không đủ")
    void should_throw_budget_exception_when_budget_exceeded() {
        // Given
        CreatePrCommand command = TestDataFactory.commandWithHighAmount(BigDecimal.valueOf(999_000_000));
        given(idempotencyService.getOrExecute(any(), any(), any()))
            .willAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
        given(budgetServicePort.check(any(), any(), any(), any()))
            .willReturn(BudgetCheckResult.exceeded(BigDecimal.valueOf(100_000), BigDecimal.valueOf(999_000_000), BigDecimal.valueOf(898_900_000)));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(command, "idem-key-456"))
            .isInstanceOf(InsufficientBudgetException.class)
            .extracting("errorCode")
            .isEqualTo("PR-2201");
    }
}

// === TestDataFactory ===
public final class TestDataFactory {

    private TestDataFactory() {}

    public static CreatePrCommand validCreatePrCommand() {
        return new CreatePrCommand(
            UUID.randomUUID(),          // requesterId
            UUID.randomUUID(),          // departmentId
            "OFFICE_SUPPLIES",          // categoryCode
            PrPriority.NORMAL,
            List.of(validLineItemCommand())
        );
    }

    public static PrLineItemCommand validLineItemCommand() {
        return new PrLineItemCommand(
            "Laptop Dell XPS 15",
            2,
            BigDecimal.valueOf(25_000_000),
            "VND"
        );
    }
}
```

### Checklist
```
[ ] Naming: should_{expected}_when_{condition}
[ ] @DisplayName tiếng Việt
[ ] Given/When/Then rõ ràng
[ ] MockitoExtension cho unit test
```
