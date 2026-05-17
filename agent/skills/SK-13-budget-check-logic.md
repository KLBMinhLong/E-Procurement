## SK-13 · Budget Check Logic

### Trigger
Agent implement kiểm tra ngân sách khi tạo PR hoặc commit ngân sách khi approve.

### Inputs Required
- departmentId
- fiscalYear
- glCode
- requestAmount

### Rules
```
[R1] Budget check: Available = Allocated - Committed - Spent
[R2] Committed = tổng PR PENDING + APPROVED chưa thanh toán + PO chưa nhận hàng
[R3] Real-time check gọi Finance Service qua HTTP (sync)
[R4] Tentative commit khi PR submit, FIRM commit khi PR approve
[R5] Release commit khi PR reject hoặc cancel
[R6] Cảnh báo: < 20% còn lại → WARNING; 0% → BLOCK
[R7] Override flow: Manager confirm + Finance approval bắt buộc
```

### Template — Budget Check
```java
@Service
public class BudgetCheckService {

    public BudgetCheckResult check(UUID departmentId, int fiscalYear,
                                    String glCode, BigDecimal requestAmount) {
        BudgetSummary summary = financeServicePort.getBudgetSummary(departmentId, fiscalYear, glCode);

        BigDecimal available = summary.getAllocated()
            .subtract(summary.getCommitted())
            .subtract(summary.getSpent());

        BigDecimal utilizationAfter = summary.getCommitted()
            .add(summary.getSpent())
            .add(requestAmount)
            .divide(summary.getAllocated(), 4, RoundingMode.HALF_UP);

        if (available.compareTo(requestAmount) < 0) {
            return BudgetCheckResult.exceeded(available, requestAmount,
                requestAmount.subtract(available)); // overage amount
        }

        if (utilizationAfter.compareTo(new BigDecimal("0.80")) >= 0) {
            return BudgetCheckResult.warning(available, requestAmount, utilizationAfter);
        }

        return BudgetCheckResult.passed(available);
    }
}
```

### Checklist
```
[ ] Available = Allocated - Committed - Spent
[ ] Warning khi còn < 20%
[ ] Release commit khi reject/cancel
```
