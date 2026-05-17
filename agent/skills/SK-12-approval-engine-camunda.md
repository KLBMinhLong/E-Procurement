## SK-12 · Approval Engine (Camunda BPMN)

### Trigger
Agent tạo/sửa approval workflow, thêm step mới, hoặc implement escalation.

### Inputs Required
- Process key
- Approval chain rules
- SLA durations
- Process variables

### Rules
```
[R1] Camunda BPMN 7 — không phải Camunda 8 (SaaS)
[R2] Process key naming: {entity}-approval-process
[R3] User Task naming: {role}-approval-task (VD: manager-approval-task)
[R4] Service Task cho tất cả integration (gửi notification, cập nhật DB)
[R5] Timer Event cho SLA / escalation — dùng ISO 8601 duration (PT2H, P2D)
[R6] Process Variables: prId, requesterId, totalAmount, priority, approvalChain
[R7] Không viết business logic trong BPMN delegate — chỉ delegate xuống Spring bean
[R8] Approval chain được build động bởi Java (không hardcode trong BPMN)
[R9] Maker-Checker pattern: người tạo ≠ người duyệt (enforce ở delegate)
```

### Template — Approval Chain Builder
```java
@Service
public class ApprovalChainBuilder {

    /**
     * Xây dựng chuỗi approver động dựa trên PR value và category.
     * Theo ma trận phê duyệt đã định nghĩa.
     */
    public List<ApprovalStep> buildChain(UUID prId, BigDecimal totalAmount,
                                          String categoryCode, UUID departmentId,
                                          UUID requesterId) {
        List<ApprovalStep> chain = new ArrayList<>();

        // 1. Load approver theo phòng ban từ org chart
        UUID directManager = orgChartService.getDirectManager(requesterId);

        // 2. Kiểm tra self-approval conflict
        if (directManager.equals(requesterId)) {
            directManager = orgChartService.getDirectManager(directManager); // Leo cấp
        }

        // 3. Thêm Manager (luôn là bước đầu)
        chain.add(ApprovalStep.of(1, directManager, ApprovalRole.MANAGER, getSla(PR_PRIORITY)));

        // 4. Theo giá trị
        if (totalAmount.compareTo(new BigDecimal("5000000")) >= 0) {
            UUID director = orgChartService.getDirector(departmentId);
            chain.add(ApprovalStep.of(2, director, ApprovalRole.DIRECTOR, getSla(PR_PRIORITY)));
        }

        if (totalAmount.compareTo(new BigDecimal("200000000")) >= 0) {
            chain.add(ApprovalStep.of(3, cLevelService.getCEO(), ApprovalRole.CEO, getSla(PR_PRIORITY)));
        }

        if (totalAmount.compareTo(new BigDecimal("500000000")) >= 0) {
            // Thay CEO bằng HĐQT
            chain.removeIf(step -> step.getRole() == ApprovalRole.CEO);
            cLevelService.getBoardMembers().forEach(member ->
                chain.add(ApprovalStep.of(3, member, ApprovalRole.BOARD, getSla(PR_PRIORITY))));
        }

        // 5. Bổ sung theo loại hàng (category rules)
        chain.addAll(buildCategorySteps(categoryCode, totalAmount));

        // 6. Bước Tài chính luôn là bước CUỐI
        if (totalAmount.compareTo(new BigDecimal("5000000")) >= 0) {
            int lastOrder = chain.stream().mapToInt(ApprovalStep::getOrder).max().orElse(0);
            UUID cfo = cLevelService.getCFO();
            chain.add(ApprovalStep.of(lastOrder + 1, cfo, ApprovalRole.FINANCE, getSla(PR_PRIORITY)));
        }

        return chain;
    }
}
```

### Checklist
```
[ ] Camunda BPMN 7 (không dùng Camunda 8)
[ ] Approval chain build động bằng Java
[ ] Maker-Checker enforced
[ ] Timer/SLA dùng ISO 8601 duration
```
