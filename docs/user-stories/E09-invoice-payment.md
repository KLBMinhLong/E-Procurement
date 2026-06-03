# E09 Invoice & Payment

## Scope

E09 completes the finance tail of the procurement flow:

```
PO issued -> Goods received -> Invoice captured -> 3-way match -> approve invoice -> confirm payment
```

This backlog starts with invoice entry/list/detail foundation before adding 3-way match, approval, dispute, and payment confirmation.

## User Stories

### E09-US-001 Capture Vendor Invoice

As an accountant, I want to enter a vendor invoice against an issued PO so that the system can later match it against PO and GR data.

Acceptance:
- `POST /api/v1/invoices` requires `INVOICE_CREATE` and `Idempotency-Key`.
- Request must include invoice number, vendor id, PO id, invoice date, due date, and at least one line.
- PO must exist and vendor must match the PO vendor.
- Each line must include `poLineItemId` from the selected PO so matching can compare invoice, PO, and GR quantities by the same line key.
- Created invoice starts in `PENDING_MATCH`.
- Money uses string JSON, `BigDecimal` Java, `NUMERIC(19,4)` DB.

### E09-US-002 View Invoice Worklist

As an accountant, I want to list and filter invoices so that I can find invoices pending match or overdue payment.

Acceptance:
- `GET /api/v1/invoices` requires `INVOICE_VIEW`.
- Filters: status, vendor id, PO id, overdue only.
- Response includes pagination metadata and invoice detail projection.

### E09-US-003 View Invoice Detail

As an accountant, I want to open a single invoice detail so that I can review line totals and match status before taking action.

Acceptance:
- `GET /api/v1/invoices/{id}` requires `INVOICE_VIEW`.
- Missing invoice returns `FIN_007`.
- Detail includes vendor snapshot, PO snapshot, line items, totals, dates, status, and nullable match result.

### E09-US-004 Run 3-Way Match

As an accountant, I want to match invoice lines against PO and GR records so that mismatches can be reviewed before payment.

Acceptance:
- `POST /api/v1/invoices/{id}/match` requires `INVOICE_MATCH` and `Idempotency-Key`.
- Match checks invoice subtotal, unit price, and quantity against PO lines.
- Match checks invoice quantities against finance-side GR snapshots consumed from `inventory.gr.created`.
- Result can be `MATCHED`, `MISMATCHED`, or `PARTIAL`.
- Matched invoices publish `finance.invoice.matched`.

### E09-US-005 Approve, Dispute, And Pay Invoice

As finance, I want to approve, dispute, and confirm payment for invoices so that payment state and budget spend are auditable.

Acceptance:
- Approve requires `INVOICE_APPROVE`.
- Approve is allowed only after 3-way match status is `MATCHED`.
- Dispute requires a reason and is allowed only when invoice status is `MISMATCHED`.
- Confirm payment requires `PAYMENT_CONFIRM`, payment date, and payment reference.
- Confirm payment is allowed only for `APPROVED` invoices and records a `finance.payments` row.
- The MVP payment action requires paid amount to equal invoice total amount before marking the invoice `PAID`.
- Budget spent ledger update remains deferred until PO/invoice carries a reliable budget reference.

## Next Coding Slices

1. Invoice create/list/detail foundation.
2. 3-way match and `finance.invoice.matched` publication.
3. Invoice approve/dispute/payment actions.
4. Budget spent ledger link once the PO/invoice budget reference contract is available.
