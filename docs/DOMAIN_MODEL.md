# DOMAIN MODEL DOCUMENT
## eProcure Enterprise — Mô hình Nghiệp vụ

---

> **Version:** 1.0.0  
> Domain được thiết kế theo **Domain-Driven Design (DDD)**.  
> Domain Layer là POJO thuần — không phụ thuộc framework.

---

## 1. BOUNDED CONTEXTS & AGGREGATE MAP

```
┌─────────────────────────────────────────────────────────────────────┐
│  IAM CONTEXT                                                         │
│  Aggregates: User, Role, Department, OrgNode, Delegation            │
├─────────────────────────────────────────────────────────────────────┤
│  PROCUREMENT CONTEXT                                                 │
│  Aggregates: PurchaseRequest, PurchaseOrder, RFQ, Contract          │
├─────────────────────────────────────────────────────────────────────┤
│  APPROVAL CONTEXT                                                    │
│  Aggregates: ApprovalProcess, ApprovalRule, ApprovalTask            │
├─────────────────────────────────────────────────────────────────────┤
│  FINANCE CONTEXT                                                     │
│  Aggregates: Budget, Invoice, Payment, BudgetTransfer               │
├─────────────────────────────────────────────────────────────────────┤
│  INVENTORY CONTEXT                                                   │
│  Aggregates: Item, StockEntry, GoodsReceipt, StockMovement          │
├─────────────────────────────────────────────────────────────────────┤
│  VENDOR CONTEXT                                                      │
│  Aggregates: Vendor, VendorContact, VendorScore                     │
├─────────────────────────────────────────────────────────────────────┤
│  NOTIFICATION CONTEXT                                                │
│  Aggregates: Notification, NotificationTemplate                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. IAM CONTEXT

### 2.1 Aggregate: User

```
User (Aggregate Root)
├── id: UserId                   (UUID, immutable)
├── employeeCode: String         (VD: "EMP-2025-00123")
├── username: String             (lowercase, unique)
├── passwordHash: String         (BCrypt, salt bao gồm userId)
├── email: Email                 (Value Object, masked in log)
├── phone: PhoneNumber           (Value Object, masked in log)
├── fullName: String
├── avatarUrl: String
├── departmentId: DepartmentId
├── orgNodeId: OrgNodeId         (vị trí trong org chart)
├── status: UserStatus           (ACTIVE, INACTIVE, LOCKED, PENDING_VERIFY)
├── twoFactorEnabled: boolean
├── twoFactorSecret: String      (encrypted at rest)
├── googleOauthId: String        (nullable, dùng khi login Google)
├── roles: Set<RoleCode>         (Many-to-Many via UserRole)
├── lastLoginAt: Instant
├── lastLoginIp: String
├── passwordChangedAt: Instant
├── createdAt: Instant
├── updatedAt: Instant
├── isDeleted: boolean
├── deletedAt: Instant
└── deletedBy: UserId

Invariants:
- passwordHash phải include userId salt (tránh rainbow table cross-user)
- Không thể ACTIVE nếu email chưa verify
- LOCKED sau 5 lần login sai liên tiếp
```

**Value Objects:**
```java
// Email — immutable, validation built-in
public record Email(String value) {
    public Email {
        if (!value.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"))
            throw new InvalidEmailException(value);
    }
    public String masked() { /* u***@***.com */ }
}

// PhoneNumber
public record PhoneNumber(String value) {
    public String masked() { /* 09*****678 */ }
}
```

### 2.2 Aggregate: Role

```
Role (Aggregate Root)
├── code: RoleCode               (String, VD: "MANAGER", "PURCHASING")
├── name: String                 (Hiển thị)
├── description: String
├── permissions: Set<PermissionCode>
├── isSystemRole: boolean        (không thể xoá nếu true)
└── (soft delete fields)
```

**Permission Code Master List:**
```
── PR (Purchase Request) ──────────────────────────────────────────
PR_CREATE                  Tạo PR mới
PR_VIEW_OWN                Xem PR của mình
PR_VIEW_DEPARTMENT         Xem PR của phòng ban
PR_VIEW_ALL                Xem tất cả PR
PR_EDIT_OWN_DRAFT          Sửa PR nháp của mình
PR_CANCEL_OWN              Huỷ PR của mình
PR_APPROVE_L1              Duyệt PR cấp 1 (Manager)
PR_APPROVE_L2              Duyệt PR cấp 2 (Director)
PR_APPROVE_L3              Duyệt PR cấp 3 (C-Level)
PR_APPROVE_FINANCE         Duyệt PR - bước Tài chính
PR_APPROVE_EMERGENCY       Duyệt PR khẩn cấp
PR_REQUEST_CHANGES         Yêu cầu bổ sung thông tin
PR_FORWARD                 Chuyển cho người khác xem xét

── PO (Purchase Order) ────────────────────────────────────────────
PO_CREATE                  Tạo PO từ PR đã duyệt
PO_VIEW_OWN                Xem PO của mình
PO_VIEW_ALL                Xem tất cả PO
PO_EDIT                    Sửa PO (trước khi gửi)
PO_SEND_TO_VENDOR          Gửi PO cho nhà cung cấp
PO_CANCEL                  Huỷ PO

── RFQ (Request for Quotation) ────────────────────────────────────
RFQ_CREATE                 Tạo RFQ
RFQ_VIEW                   Xem RFQ
RFQ_EVALUATE               Đánh giá và chọn báo giá
RFQ_AWARD                  Chốt nhà cung cấp thắng thầu

── GR (Goods Receipt) ─────────────────────────────────────────────
GR_CREATE                  Tạo phiếu nhận hàng
GR_VIEW                    Xem GR
GR_ISSUE_OUT               Cấp phát hàng từ kho

── INVOICE ────────────────────────────────────────────────────────
INVOICE_CREATE             Nhập hoá đơn từ vendor
INVOICE_VIEW               Xem hoá đơn
INVOICE_MATCH              Thực hiện 3-way match
INVOICE_APPROVE            Duyệt hoá đơn
PAYMENT_CONFIRM            Xác nhận thanh toán

── BUDGET ─────────────────────────────────────────────────────────
BUDGET_VIEW_OWN_DEPT       Xem ngân sách phòng mình
BUDGET_VIEW_ALL            Xem ngân sách tất cả phòng
BUDGET_OVERRIDE            Duyệt vượt ngân sách
BUDGET_TRANSFER_APPROVE    Duyệt điều chuyển ngân sách

── VENDOR ─────────────────────────────────────────────────────────
VENDOR_CREATE              Thêm nhà cung cấp
VENDOR_VIEW                Xem danh sách nhà cung cấp
VENDOR_EDIT                Sửa thông tin nhà cung cấp
VENDOR_APPROVE             Duyệt nhà cung cấp mới vào AVL

── REPORT ─────────────────────────────────────────────────────────
REPORT_VIEW                Xem báo cáo
REPORT_EXPORT              Xuất báo cáo PDF/Excel

── ADMIN ──────────────────────────────────────────────────────────
ADMIN_USER_VIEW            Xem danh sách người dùng
ADMIN_USER_MANAGE          Tạo/sửa/khoá người dùng
ADMIN_ROLE_MANAGE          Quản lý role và permission
ADMIN_APPROVAL_RULE        Cấu hình approval rules
ADMIN_CATALOG_MANAGE       Quản lý danh mục hàng hoá
ADMIN_DELEGATION_MANAGE    Quản lý uỷ quyền
ADMIN_DEPARTMENT_MANAGE    Quản lý phòng ban và org chart
SYSTEM_CONFIG              Truy cập trang cấu hình hệ thống
SYSTEM_AUDIT_VIEW          Xem audit log
```

### 2.3 Aggregate: Department

```
Department (Aggregate Root)
├── id: DepartmentId
├── code: String                 (VD: "IT", "HR", "FIN")
├── name: String
├── parentId: DepartmentId       (nullable, phân cấp phòng ban)
├── headUserId: UserId           (Trưởng phòng)
├── glAccountPrefix: String      (GL code mặc định cho phòng)
└── (soft delete fields)
```

### 2.4 Aggregate: Delegation (Uỷ quyền)

```
Delegation (Aggregate Root)
├── id: DelegationId
├── delegatorId: UserId          (Người uỷ quyền)
├── delegateId: UserId           (Người được uỷ quyền — phải cùng/cao hơn cấp)
├── startAt: Instant
├── endAt: Instant
├── maxValue: Money              (Giá trị tối đa được uỷ quyền)
├── allowedCategories: Set<CategoryCode>  (nullable = tất cả)
├── scope: DelegationScope       (ALL | OWN_TEAM)
├── status: DelegationStatus     (ACTIVE | EXPIRED | REVOKED)
└── (audit + soft delete fields)

Invariants:
- delegateId.orgLevel >= delegatorId.orgLevel
- Không thể uỷ quyền vượt quyền của bản thân
- Phải có audit log khi tạo/sửa/thu hồi
```

---

## 3. PROCUREMENT CONTEXT

### 3.1 Aggregate: PurchaseRequest

```
PurchaseRequest (Aggregate Root)
├── id: PurchaseRequestId
├── prNumber: String             (PR-YYYY-MM-XXXXX, auto-generated)
├── requesterId: UserId
├── departmentId: DepartmentId
├── title: String
├── justification: String        (> 50 ký tự)
├── priority: PrPriority         (NORMAL | URGENT | EMERGENCY)
├── urgencyReason: String        (bắt buộc nếu URGENT/EMERGENCY)
├── status: PrStatus
├── lineItems: List<PrLineItem>  (Entity)
├── attachments: List<Attachment>(Value Object)
├── budgetCheck: BudgetCheckResult (Value Object, snapshot lúc submit)
├── inventoryCheck: InventoryCheckResult (Value Object)
├── totalAmount: Money           (calculated từ lineItems)
├── fiscalYear: int
├── needByDate: LocalDate
├── relatedContractId: ContractId (nullable, nếu Blanket PO)
├── emergencyReportSubmittedAt: Instant (nullable)
└── (audit + soft delete fields)
```

**PrStatus state machine:**
```
DRAFT ──[submit]──> SUBMITTED
SUBMITTED ──[approval engine picks up]──> PENDING_APPROVAL
PENDING_APPROVAL ──[approver requests changes]──> CHANGES_REQUESTED
CHANGES_REQUESTED ──[requester resubmits]──> PENDING_APPROVAL
PENDING_APPROVAL ──[all approved]──> APPROVED
PENDING_APPROVAL ──[any rejected]──> REJECTED
APPROVED ──[PO created]──> CONVERTED_TO_PO
DRAFT/SUBMITTED/CHANGES_REQUESTED ──[requester cancels]──> CANCELLED
CONVERTED_TO_PO ──[PO closed/delivered]──> CLOSED
```

**PrLineItem (Entity):**
```
PrLineItem
├── id: LineItemId
├── prId: PurchaseRequestId
├── lineNumber: int              (1, 2, 3...)
├── itemCode: String             (nullable nếu free-text)
├── itemName: String
├── description: String
├── categoryCode: CategoryCode
├── quantity: Quantity           (Value Object: amount + unit)
├── unitPrice: Money
├── totalPrice: Money            (= quantity * unitPrice)
├── preferredVendorId: VendorId  (nullable)
├── specifications: String       (thông số kỹ thuật)
├── isFromCatalog: boolean
└── glAccountCode: String        (tài khoản kế toán)
```

**Value Objects:**
```java
public record Money(BigDecimal amount, String currency) {
    public Money { 
        Objects.requireNonNull(amount);
        if (amount.scale() > 4) throw new InvalidMoneyException();
        currency = currency == null ? "VND" : currency;
    }
    public Money add(Money other) { ... }
    public boolean isGreaterThan(Money other) { ... }
}

public record Quantity(BigDecimal amount, String unit) {}

public record BudgetCheckResult(
    Money allocated, Money committed, Money spent, 
    Money available, BudgetCheckStatus status, String warningMessage
) {}
```

### 3.2 Aggregate: PurchaseOrder

```
PurchaseOrder (Aggregate Root)
├── id: PurchaseOrderId
├── poNumber: String             (PO-YYYY-MM-XXXXX)
├── prId: PurchaseRequestId
├── vendorId: VendorId
├── purchasingOfficerId: UserId
├── status: PoStatus
├── lineItems: List<PoLineItem>
├── deliveryAddress: Address     (Value Object)
├── deliveryDeadline: LocalDate
├── paymentTerms: String
├── totalAmount: Money
├── issuedAt: Instant
├── sentToVendorAt: Instant
├── isBlanketRelease: boolean
└── (audit + soft delete fields)
```

**PoStatus:**
```
DRAFT → PENDING_APPROVAL → APPROVED → SENT_TO_VENDOR → 
PARTIALLY_RECEIVED → FULLY_RECEIVED → INVOICED → PAID → CLOSED | CANCELLED
```

### 3.3 Aggregate: RFQ

```
RFQ (Aggregate Root)
├── id: RfqId
├── rfqNumber: String            (RFQ-YYYY-MM-XXXXX)
├── prId: PurchaseRequestId
├── title: String
├── status: RfqStatus
├── submissionDeadline: Instant
├── invitedVendors: List<RfqVendorInvitation>  (Entity)
├── quotes: List<VendorQuote>    (Entity)
├── awardedVendorId: VendorId    (nullable, sau khi chốt)
├── awardedQuoteId: QuoteId      (nullable)
└── (audit + soft delete fields)
```

---

## 4. APPROVAL CONTEXT

### 4.1 Aggregate: ApprovalProcess

```
ApprovalProcess (Aggregate Root)
├── id: ApprovalProcessId
├── entityType: String           ("PURCHASE_REQUEST", "INVOICE", etc.)
├── entityId: UUID
├── camundaProcessInstanceId: String
├── status: ApprovalProcessStatus (RUNNING | COMPLETED | CANCELLED)
├── steps: List<ApprovalStep>    (Entity)
├── currentStepIndex: int
├── startedAt: Instant
├── completedAt: Instant
└── (audit fields)
```

**ApprovalStep (Entity):**
```
ApprovalStep
├── id: ApprovalStepId
├── processId: ApprovalProcessId
├── stepIndex: int               (thứ tự trong chuỗi)
├── stepType: StepType           (SEQUENTIAL | PARALLEL)
├── approverRole: String         (MANAGER | DIRECTOR | C_LEVEL | FINANCE | etc.)
├── approverId: UserId           (resolved từ org chart)
├── delegateId: UserId           (nullable, nếu đang uỷ quyền)
├── status: StepStatus           (PENDING | APPROVED | REJECTED | ESCALATED | SKIPPED)
├── action: ApprovalAction       (APPROVE | REJECT | REQUEST_CHANGES | FORWARD)
├── comment: String
├── slaDeadline: Instant         (tính theo business hours)
├── assignedAt: Instant
├── actedAt: Instant
├── isEscalated: boolean
├── escalatedFromUserId: UserId  (nullable)
└── camundaTaskId: String        (Camunda task reference)
```

### 4.2 Aggregate: ApprovalRule

```
ApprovalRule (Aggregate Root)
├── id: ApprovalRuleId
├── ruleName: String
├── priority: int                (thứ tự áp dụng rule, cao hơn = ưu tiên hơn)
├── isActive: boolean
├── ruleType: RuleType           (VALUE | CATEGORY | DEPARTMENT | DEFAULT)
├── conditions: ApprovalCondition (Value Object)
│   ├── minValue: Money          (nullable)
│   ├── maxValue: Money          (nullable)
│   ├── categories: Set<CategoryCode>
│   ├── departments: Set<DepartmentId>
│   └── priorities: Set<PrPriority>
├── approvalStepTemplates: List<ApprovalStepTemplate>
│   ├── stepIndex: int
│   ├── approverRole: String
│   ├── stepType: StepType       (SEQUENTIAL | PARALLEL)
│   ├── slaHours: int            (giờ làm việc)
│   └── isRequired: boolean
└── (audit + soft delete fields)
```

**Ma trận phê duyệt được lưu dưới dạng ApprovalRule records:**

| Rule | Conditions | Steps |
|---|---|---|
| VALUE_UNDER_5M | value < 5M | [Manager/L1/SEQ/48h] |
| VALUE_5M_20M | 5M ≤ value < 20M | [Manager/L1/SEQ/48h] → [Finance/FIN/SEQ/48h] |
| VALUE_20M_50M | 20M ≤ value < 50M | [Manager/L1/SEQ/48h] → [Director/L2/SEQ/48h] → [Finance/FIN/SEQ/48h] |
| VALUE_50M_200M | 50M ≤ value < 200M | [Manager/L1] → [Director/L2] → [CFO/FIN/SEQ] + RFQ |
| VALUE_200M_500M | 200M ≤ value < 500M | [Manager] → [Director] → [CEO/L3] → [CFO/FIN] |
| VALUE_OVER_500M | value ≥ 500M | [Manager] → [Director] → [BOD/L3/PAR] → [CFO/FIN] |
| EMERGENCY | priority = EMERGENCY | [Manager/L1/PAR/2h, Director/L2/PAR/4h] → PostAudit |
| CAT_IT_SOFTWARE | category IN (SOFTWARE, SAAS) | +[CISO, IT_MANAGER] bất kể giá trị |
| CAT_CAPEX | isCapex = true | +[FinanceDirector, CEO] bất kể giá trị |

---

## 5. FINANCE CONTEXT

### 5.1 Aggregate: Budget

```
Budget (Aggregate Root)
├── id: BudgetId
├── departmentId: DepartmentId
├── fiscalYear: int
├── quarter: int                 (nullable, nếu chia theo quý)
├── glAccountCode: String
├── allocatedAmount: Money
├── committedAmount: Money       (computed: sum of firm-committed PR+PO)
├── spentAmount: Money           (computed: sum of paid invoices)
├── status: BudgetStatus         (PLANNING | SUBMITTED | APPROVED | ACTIVE | CLOSED)
├── approvedBy: UserId
├── approvedAt: Instant
└── (audit + soft delete fields)

Derived:
availableAmount = allocatedAmount - committedAmount - spentAmount
burnRate = spentAmount / monthsElapsed
forecastExhaustedAt = estimateFromBurnRate()
```

### 5.2 Aggregate: Invoice

```
Invoice (Aggregate Root)
├── id: InvoiceId
├── invoiceNumber: String        (số hoá đơn từ vendor)
├── vendorId: VendorId
├── poId: PurchaseOrderId
├── lineItems: List<InvoiceLineItem>
├── subtotal: Money
├── taxAmount: Money
├── totalAmount: Money
├── invoiceDate: LocalDate
├── dueDate: LocalDate
├── status: InvoiceStatus
├── matchResult: ThreeWayMatchResult (Value Object)
│   ├── poMatchStatus: MatchStatus
│   ├── grMatchStatus: MatchStatus
│   ├── quantityVariance: BigDecimal
│   ├── priceVariance: Money
│   └── matchedAt: Instant
└── (audit + soft delete fields)
```

---

## 6. INVENTORY CONTEXT

### 6.1 Aggregate: Item (Catalog Item)

```
Item (Aggregate Root)
├── id: ItemId
├── itemCode: String             (unique, VD: "IT-001")
├── name: String
├── description: String
├── categoryCode: CategoryCode
├── unit: String                 (cái, bộ, kg, m...)
├── unitPrice: Money             (giá tham chiếu)
├── preferredVendorId: VendorId  (nullable)
├── reorderPoint: BigDecimal     (ngưỡng đặt hàng lại)
├── isActive: boolean
└── (audit + soft delete fields)
```

### 6.2 Aggregate: GoodsReceipt

```
GoodsReceipt (Aggregate Root)
├── id: GrId
├── grNumber: String             (GR-YYYY-MM-XXXXX)
├── poId: PurchaseOrderId
├── warehouseKeeperId: UserId
├── receivedAt: Instant
├── status: GrStatus             (DRAFT | PARTIAL | COMPLETE | DISCREPANCY)
├── lineItems: List<GrLineItem>
│   ├── poLineItemId: PoLineItemId
│   ├── orderedQuantity: Quantity
│   ├── receivedQuantity: Quantity
│   ├── rejectedQuantity: Quantity
│   ├── rejectionReason: String
│   └── lotNumber: String
└── (audit + soft delete fields)
```

### 6.3 Aggregate: StockMovement

```
StockMovement (Value Object / Event)
├── id: MovementId
├── itemId: ItemId
├── movementType: MovementType   (RECEIPT_IN | ISSUE_OUT | ADJUSTMENT | TRANSFER)
├── quantity: Quantity
├── sourceRef: String            (GR number / PR number)
├── warehouseId: String
├── performedBy: UserId
├── performedAt: Instant
└── (immutable — không soft delete)
```

---

## 7. VENDOR CONTEXT

### 7.1 Aggregate: Vendor

```
Vendor (Aggregate Root)
├── id: VendorId
├── vendorCode: String           (VD: "VND-001")
├── name: String
├── taxCode: String              (MST)
├── email: Email
├── phone: PhoneNumber
├── address: Address             (Value Object)
├── contacts: List<VendorContact>(Entity)
├── status: VendorStatus         (PENDING | APPROVED | BLACKLISTED | INACTIVE)
├── isOnApprovedVendorList: boolean (AVL)
├── categories: Set<CategoryCode>
├── scorecard: VendorScorecard   (Value Object)
│   ├── qualityScore: int        (0-100)
│   ├── deliveryScore: int
│   ├── priceScore: int
│   ├── overallScore: int
│   └── lastEvaluatedAt: Instant
├── contractIds: List<ContractId>
└── (audit + soft delete fields)
```

---

## 8. DOMAIN EVENTS

Các domain event được publish lên Kafka khi có thay đổi trạng thái quan trọng:

| Event | Topic | Publisher | Consumer |
|---|---|---|---|
| `PrSubmittedEvent` | `procurement.pr.submitted` | PR Service | Approval Engine, Notification |
| `PrApprovedEvent` | `procurement.pr.approved` | Approval Engine | PR Service, Finance, Purchasing |
| `PrRejectedEvent` | `procurement.pr.rejected` | Approval Engine | PR Service, Notification |
| `PrChangesRequestedEvent` | `procurement.pr.changes-requested` | Approval Engine | PR Service, Notification |
| `ApprovalStepAssignedEvent` | `approval.step.assigned` | Approval Engine | Notification |
| `ApprovalEscalatedEvent` | `approval.escalated` | Approval Engine | Notification, Admin |
| `BudgetExceededWarningEvent` | `finance.budget.warning` | Finance | Notification |
| `PoIssuedEvent` | `procurement.po.issued` | PR Service | Inventory, Finance |
| `GrCreatedEvent` | `inventory.gr.created` | Inventory | Finance (3-way match trigger) |
| `InvoiceMatchedEvent` | `finance.invoice.matched` | Finance | Notification |
| `SlaBreachedEvent` | `approval.sla.breached` | Approval Engine | Notification, Admin |
| `EmergencyAbuseDetectedEvent` | `procurement.emergency.abuse` | PR Service | HR, Compliance |

---

## 9. UBIQUITOUS LANGUAGE (THUẬT NGỮ CHUNG)

| Tiếng Việt | Tiếng Anh (code) | Định nghĩa |
|---|---|---|
| Yêu cầu mua sắm | Purchase Request (PR) | Document nhân viên tạo để đề nghị mua hàng |
| Đơn đặt hàng | Purchase Order (PO) | Document chính thức gửi cho nhà cung cấp |
| Thu thập báo giá | Request for Quotation (RFQ) | Quy trình mời nhà cung cấp gửi báo giá |
| Phiếu nhận hàng | Goods Receipt (GR) | Xác nhận hàng đến kho và kiểm tra |
| Đối soát 3 chiều | 3-Way Match | So khớp PO + GR + Invoice |
| Phong toả ngân sách | Budget Commitment | Khóa số tiền ngân sách khi PR/PO được tạo |
| Vượt ngân sách | Budget Override | Phê duyệt đặc biệt khi chi vượt budget |
| Uỷ quyền | Delegation | Approver giao quyền tạm thời cho người khác |
| Leo thang | Escalation | Tự động chuyển lên cấp trên khi quá SLA |
| Danh sách NCC ưu tiên | Approved Vendor List (AVL) | Danh sách NCC đã được phê duyệt |
| Hợp đồng khung | Blanket PO / Framework Contract | Hợp đồng mua nhiều lần trong kỳ |
| Hậu kiểm | Post-Audit | Kiểm tra sau khi đã thực hiện (EMERGENCY) |
| Xoá mềm | Soft Delete | Đánh dấu xoá, không xoá thực khỏi DB |
| Cam kết | Committed | Ngân sách đã được phong toả cho PR/PO |
| Đã chi | Spent | Ngân sách đã thực sự thanh toán |
| Có sẵn | Available | Ngân sách còn lại = Allocated - Committed - Spent |
