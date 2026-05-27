# domain-glossary.md
## eProcure Enterprise — Thuật Ngữ Nghiệp Vụ VI/EN

> Dùng file này khi đặt tên domain model, command, event, API DTO, translation key và test case. Nếu cần chi tiết business rule, đọc tiếp `docs/DOMAIN_MODEL.md`.

---

## 1. Ubiquitous Language

| Tiếng Việt | English / Code Term | Viết tắt | Ghi chú dùng trong code |
|---|---|---|---|
| Yêu cầu mua sắm | Purchase Request | PR | Aggregate `PurchaseRequest`, route `/purchase-requests` |
| Dòng hàng PR | PR Line Item | PR line | Entity `PrLineItem` |
| Đơn đặt hàng | Purchase Order | PO | Aggregate `PurchaseOrder`, route `/purchase-orders` |
| Thu thập báo giá | Request for Quotation | RFQ | Aggregate `Rfq` hoặc `RFQ` theo existing code style |
| Phiếu nhận hàng | Goods Receipt | GR | Aggregate `GoodsReceipt` |
| Đối soát 3 chiều | 3-Way Match | 3WM | PO + GR + Invoice matching |
| Ngân sách | Budget | Budget | Aggregate `Budget` |
| Phong toả ngân sách | Budget Commitment | Commitment | Khi PR/PO giữ ngân sách |
| Vượt ngân sách | Budget Override | Override | Cần permission đặc biệt |
| Hóa đơn | Invoice | Invoice | Finance context |
| Thanh toán | Payment | Payment | Finance context |
| Nhà cung cấp | Vendor | Vendor | Vendor context |
| Danh sách NCC ưu tiên | Approved Vendor List | AVL | Vendor approved list |
| Hợp đồng khung | Blanket PO / Framework Contract | Blanket | Mua nhiều lần trong kỳ |
| Phê duyệt | Approval | Approval | Approval context |
| Người duyệt | Approver | Approver | User được resolve từ org chart/RBAC |
| Uỷ quyền | Delegation | Delegation | IAM aggregate, ảnh hưởng approval |
| Leo thang | Escalation | Escalation | SLA breach routing |
| Hậu kiểm | Post-Audit | PostAudit | Emergency purchase review |
| Xoá mềm | Soft Delete | SoftDelete | `is_deleted`, `deleted_at`, `deleted_by` |
| Nhật ký kiểm toán | Audit Log | Audit | Append-only audit context |
| Sơ đồ tổ chức | Organization Chart | Org Chart | IAM departments/org nodes |
| Phòng ban | Department | Department | Aggregate `Department` |
| Vai trò | Role | Role | RBAC role code, không dùng trực tiếp trong `@PreAuthorize` |
| Quyền | Permission | Permission | Permission code dùng trong `hasAuthority` |

---

## 2. Bounded Context Names

| Context | Package / Service Prefix | Aggregates chính |
|---|---|---|
| IAM | `com.eprocure.iam` | `User`, `Role`, `Permission`, `Department`, `Delegation`, `SessionRecord` |
| Procurement / PR | `com.eprocure.pr` | `PurchaseRequest`, `PrLineItem`, `CatalogItem`, `CatalogCategory` |
| Approval | `com.eprocure.approval` | `ApprovalProcess`, `ApprovalStep`, `ApprovalRule`, `ApprovalTask` |
| Finance | `com.eprocure.finance` | `Budget`, `PurchaseOrder`, `Invoice`, `Payment` |
| Inventory | `com.eprocure.inventory` | `Item`, `Warehouse`, `StockEntry`, `GoodsReceipt`, `StockMovement` |
| Vendor | `com.eprocure.vendor` | `Vendor`, `VendorContact`, `Rfq`, `VendorQuote`, `VendorScore` |
| Notification | `com.eprocure.notification` | `Notification`, `NotificationTemplate` |
| Analytics | `com.eprocure.analytics` | Dashboard, KPI, report views |
| Admin | `com.eprocure.admin` | System config, audit/config views |

---

## 3. Status Names

### Purchase Request Status

Use enum values exactly:

```text
DRAFT
SUBMITTED
PENDING_APPROVAL
CHANGES_REQUESTED
APPROVED
REJECTED
CONVERTED_TO_PO
CANCELLED
CLOSED
```

### Purchase Request Priority

```text
NORMAL
URGENT
EMERGENCY
```

### User Status

```text
ACTIVE
INACTIVE
LOCKED
PENDING_VERIFY
```

### Delegation Status / Scope

```text
ACTIVE
EXPIRED
REVOKED

ALL
OWN_TEAM
```

### Approval Status / Action

```text
RUNNING
COMPLETED
CANCELLED

PENDING
APPROVED
REJECTED
ESCALATED
SKIPPED
FORWARDED

APPROVE
REJECT
REQUEST_CHANGES
FORWARD
```

---

## 4. Permission Code Vocabulary

Permission code là nguồn phân quyền trong code. Không dùng role name trong `@PreAuthorize`.

| Area | Permission Codes |
|---|---|
| PR | `PR_CREATE`, `PR_VIEW_OWN`, `PR_VIEW_DEPARTMENT`, `PR_VIEW_ALL`, `PR_EDIT_OWN_DRAFT`, `PR_CANCEL_OWN`, `PR_APPROVE_L1`, `PR_APPROVE_L2`, `PR_APPROVE_L3`, `PR_APPROVE_FINANCE`, `PR_APPROVE_EMERGENCY`, `PR_REQUEST_CHANGES`, `PR_FORWARD` |
| PO | `PO_CREATE`, `PO_VIEW_OWN`, `PO_VIEW_ALL`, `PO_EDIT`, `PO_SEND_TO_VENDOR`, `PO_CANCEL` |
| RFQ | `RFQ_CREATE`, `RFQ_VIEW`, `RFQ_EVALUATE`, `RFQ_AWARD` |
| GR / Inventory | `GR_CREATE`, `GR_VIEW`, `GR_ISSUE_OUT` |
| Invoice / Payment | `INVOICE_CREATE`, `INVOICE_VIEW`, `INVOICE_MATCH`, `INVOICE_APPROVE`, `PAYMENT_CONFIRM` |
| Budget | `BUDGET_VIEW_OWN_DEPT`, `BUDGET_VIEW_ALL`, `BUDGET_OVERRIDE`, `BUDGET_TRANSFER_APPROVE` |
| Vendor | `VENDOR_CREATE`, `VENDOR_VIEW`, `VENDOR_EDIT`, `VENDOR_APPROVE` |
| Report | `REPORT_VIEW`, `REPORT_EXPORT` |
| Admin | `ADMIN_USER_VIEW`, `ADMIN_USER_MANAGE`, `ADMIN_ROLE_MANAGE`, `ADMIN_APPROVAL_RULE`, `ADMIN_CATALOG_MANAGE`, `ADMIN_DELEGATION_MANAGE`, `ADMIN_DEPARTMENT_MANAGE`, `SYSTEM_CONFIG`, `SYSTEM_AUDIT_VIEW` |

---

## 5. Event Vocabulary

| Event | Topic | Meaning |
|---|---|---|
| `PrSubmittedEvent` | `procurement.pr.submitted` | PR được submit |
| `PrApprovedEvent` | `procurement.pr.approved` | PR được duyệt hoàn toàn |
| `PrRejectedEvent` | `procurement.pr.rejected` | PR bị từ chối |
| `PrChangesRequestedEvent` | `procurement.pr.changes-requested` | Approver yêu cầu bổ sung |
| `ApprovalStepAssignedEvent` | `approval.step.assigned` | Task được gán cho approver |
| `ApprovalEscalatedEvent` | `approval.escalated` | Approval bị escalate |
| `SlaBreachedEvent` | `approval.sla.breached` | Quá SLA |
| `BudgetExceededWarningEvent` | `finance.budget.warning` | Ngân sách cảnh báo/vượt |
| `PoIssuedEvent` | `procurement.po.issued` | PO phát hành |
| `GrCreatedEvent` | `inventory.gr.created` | GR hoàn tất |
| `InvoiceMatchedEvent` | `finance.invoice.matched` | Invoice 3-way match OK |
| `EmergencyAbuseDetectedEvent` | `procurement.emergency.abuse` | Emergency PR vượt ngưỡng |

---

## 6. Naming Guidance

- Java class dùng English code term: `PurchaseRequest`, `CreatePurchaseRequestUseCase`, `SubmitPurchaseRequestCommand`.
- Route dùng lowercase plural noun: `/api/v1/purchase-requests`, `/api/v1/goods-receipts`.
- Action endpoint dùng verb cuối path: `/submit`, `/cancel`, `/approve`, `/reject`, `/request-changes`, `/revoke`.
- Enum values luôn `UPPER_SNAKE_CASE`.
- Translation key dùng context rõ: `pr.status.draft`, `approval.action.approve`, `admin.user.status.locked`.
- Test method dùng `should_{expected_behavior}_when_{condition}`.
