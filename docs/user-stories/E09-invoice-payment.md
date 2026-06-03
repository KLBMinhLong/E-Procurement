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
- Match checks PO amount/lines and GR received quantities.
- Result can be `MATCHED`, `MISMATCHED`, or `PARTIAL`.
- Matched invoices publish `finance.invoice.matched`.

### E09-US-005 Approve, Dispute, And Pay Invoice

As finance, I want to approve, dispute, and confirm payment for invoices so that payment state and budget spend are auditable.

Acceptance:
- Approve requires `INVOICE_APPROVE`.
- Dispute requires a reason.
- Confirm payment requires `PAYMENT_CONFIRM`, payment date, and payment reference.
- Paid invoices update finance spend ledger.

## Next Coding Slices

1. Invoice create/list/detail foundation.
2. 3-way match read model and `inventory.gr.created` consumption.
3. Invoice approve/dispute actions.
4. Payment confirmation and budget spent ledger update.
