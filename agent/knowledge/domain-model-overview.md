# Domain Model Overview

## Bounded contexts and aggregates
- IAM: User, Role, Department, OrgNode, Delegation
- Procurement: PurchaseRequest, PurchaseOrder, RFQ, Contract
- Approval: ApprovalProcess, ApprovalRule, ApprovalTask
- Finance: Budget, Invoice, Payment, BudgetTransfer
- Inventory: Item, StockEntry, GoodsReceipt, StockMovement
- Vendor: Vendor, VendorContact, VendorScore
- Notification: Notification, NotificationTemplate

## PurchaseRequest (core)
- prNumber format: PR-YYYY-MM-XXXXX
- Status: DRAFT -> SUBMITTED -> PENDING_APPROVAL -> APPROVED/REJECTED
- Line items include quantity, unit price, total price
- Money uses BigDecimal with scale 4
- Soft delete fields present

## ApprovalProcess (core)
- process status: RUNNING, COMPLETED, CANCELLED
- steps include SLA deadline, approver role/id, action
- supports sequential and parallel steps

## User (core)
- password hash uses userId salt
- account locked after 5 failed attempts
- 2FA optional (TOTP)
