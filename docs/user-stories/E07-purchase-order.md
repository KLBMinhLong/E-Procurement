# E07 Purchase Order

## Scope

E07 closes the procurement execution handoff:

```text
Approved PR -> RFQ award or direct/manual PO -> DRAFT PO -> send to vendor -> issued PO event -> GR/Invoice downstream
```

The RFQ-award path already exists in `finance-service`. The remaining gap is the direct/manual PO path from an approved PR, plus the internal source contracts that let Finance create a PO without reading another service database.

## User Stories

### E07-US-001 Create PO From RFQ Award

As a purchasing officer, I want an awarded RFQ to create a draft PO automatically so that the awarded vendor and quote lines become the PO source of truth.

Acceptance:
- `finance-service` consumes `procurement.rfq.awarded`.
- Consumer is idempotent by event id through `finance.event_processing_log`.
- A duplicate award for the same `rfqId` returns the existing PO instead of creating another PO.
- PO starts in `DRAFT`.
- PO line items copy `prLineItemId`, awarded quote line price, quantity, currency, delivery days, and warranty.
- After the PO is persisted, Finance records a durable PR-conversion callback outbox row so PR can be marked `CONVERTED_TO_PO` after commit with retry.

### E07-US-002 Create Manual PO From Approved PR

As a purchasing officer, I want to create a PO directly from an approved PR so that low-risk purchases do not require a full RFQ flow.

Acceptance:
- `POST /api/v1/purchase-orders` requires `PO_CREATE` and `Idempotency-Key`.
- Request contains `prId`, `vendorId`, `deliveryAddress`, optional `deliveryDeadline`, optional `paymentTerms`, and optional `notes`.
- Frontend does not send vendor name/email/tax code or line price snapshots; Finance must fetch trusted source data from service-to-service contracts.
- Finance calls PR source contract `GET /internal/purchase-requests/{id}/po-source`.
- Finance calls Vendor source contract `GET /internal/vendors/{id}/po-source`.
- PR source must be `APPROVED`; otherwise manual PO creation fails.
- Vendor source must be `APPROVED` and on AVL; otherwise manual PO creation fails.
- MVP creates one PO containing all PR line items. Partial line selection, split quantities, and multi-vendor split PO are out of scope until a dedicated PR-line conversion ledger exists.
- Manual PO line prices use the approved PR line `unitPrice`/`totalPrice`.
- Created PO starts in `DRAFT` and can be edited before send through the existing draft edit endpoint.
- After PO is persisted, Finance records a durable callback outbox row and dispatches `PATCH /internal/purchase-requests/{id}/converted-to-po` with `Idempotency-Key`.

### E07-US-003 View PO Worklist And Detail

As purchasing and finance users, I want to list and inspect POs so that I can track ordering status and downstream readiness.

Acceptance:
- `GET /api/v1/purchase-orders` requires `PO_VIEW_OWN` or `PO_VIEW_ALL`.
- `PO_VIEW_OWN` sees POs assigned to the current purchasing officer.
- `PO_VIEW_ALL` can filter by status, vendor, and date range.
- `GET /api/v1/purchase-orders/{id}` requires `PO_VIEW_OWN` or `PO_VIEW_ALL`.
- Detail includes PR snapshot, vendor snapshot, purchasing officer snapshot, line items, total, currency, delivery, payment terms, `prConversionStatus`, and PO lifecycle timestamps.

### E07-US-004 Edit Draft PO Before Send

As a purchasing officer, I want to edit delivery and payment details while a PO is still draft so that vendor-facing data is correct before issuance.

Acceptance:
- `PATCH /api/v1/purchase-orders/{id}` requires `PO_EDIT` and `Idempotency-Key`.
- Only `DRAFT` PO can be edited.
- Editable fields: `deliveryAddress`, `deliveryDeadline`, `paymentTerms`.
- Line items, vendor snapshot, PR id, RFQ id, and totals are immutable in this slice.

### E07-US-005 Send Or Cancel PO

As a purchasing officer, I want to send a PO to the vendor or cancel it before fulfillment starts so that the procurement flow remains auditable.

Acceptance:
- `POST /api/v1/purchase-orders/{id}/send` requires `PO_SEND_TO_VENDOR` and `Idempotency-Key`.
- Send requires a non-empty delivery address and vendor email.
- Send is allowed only after the PR-conversion callback for this PO is delivered.
- Send moves PO to `SENT_TO_VENDOR`, sets `issuedAt`/`sentToVendorAt`, publishes `procurement.po.issued`, and publishes `notification.email.send`.
- `PATCH /api/v1/purchase-orders/{id}/cancel` requires `PO_CANCEL` and `Idempotency-Key`.
- Cancel is allowed before receiving/invoicing/payment starts.
- No HTTP DELETE endpoint is introduced.

## Internal Source Contracts

### E07-UC-001 GetPurchaseRequestPoSourceUseCase

Provider: `purchase-request-service`

API:

```text
GET /internal/purchase-requests/{id}/po-source
Headers: X-Internal-Api-Key
```

Rules:
- Load PR by id with `is_deleted = false`.
- Return `PR_001` if missing.
- Return `PR_003` if status is not `APPROVED`.
- Return immutable source data: PR header, requester, department, fiscal year, need-by date, total amount, and all line items.
- Line source must include `prLineItemId`, `lineNumber`, optional `itemCode`, item name, description, category, quantity, unit, unit price, total price, currency, preferred vendor id, specifications, GL account, and catalog flag.

### E07-UC-002 MarkPurchaseRequestConvertedToPoUseCase

Provider: `purchase-request-service`

API:

```text
PATCH /internal/purchase-requests/{id}/converted-to-po
Headers: X-Internal-Api-Key, Idempotency-Key
Body: { "poId": "uuid", "poNumber": "PO-2026-000001" }
```

Rules:
- Valid transition is `APPROVED -> CONVERTED_TO_PO`.
- If request is replayed with the same idempotency key, return the cached converted view.
- If PR is already `CONVERTED_TO_PO`, return current converted view only for idempotent replay; otherwise reject with `PR_003`.
- The callback does not create or mutate PO data; Finance remains the PO owner.

### E07-UC-003 GetVendorPoSourceUseCase

Provider: `vendor-service`

API:

```text
GET /internal/vendors/{id}/po-source
Headers: X-Internal-Api-Key
```

Rules:
- Load vendor by id with `is_deleted = false`.
- Return `VND_001` if missing.
- Return `VND_008` if vendor status is not `APPROVED` or vendor is not on AVL.
- Return vendor snapshot: id, vendor code, name, tax code, primary email, phone, categories, primary contact, status, and AVL flag.

## Manual PO Use Case

### E07-UC-004 CreateManualPurchaseOrderUseCase

Provider: `finance-service`

Main flow:
1. Verify `Idempotency-Key`.
2. Resolve current user as purchasing officer.
3. Fetch PR PO source from `purchase-request-service`.
4. Fetch vendor PO source from `vendor-service`.
5. Check no active manual PO already exists for the same `prId`.
6. Generate `poNumber`.
7. Create `finance.purchase_orders` header with `rfqId = null`, trusted vendor snapshot, delivery data, officer snapshot, and `sourceEventId = "manual-po:{idempotencyKey}"`.
8. Create `finance.po_line_items` from every PR source line.
9. Create `finance.po_pr_conversion_callbacks` outbox row with the same idempotency key.
10. Return `201` with `PurchaseOrderDetail` after the local transaction commits.
11. A scheduled/after-commit dispatcher calls the PR converted callback and marks the outbox row `DELIVERED` on success.

Error flow:
- PR missing: propagate or map source error to a 404 response.
- PR not `APPROVED`: fail with `FIN_017`.
- Vendor not approved/on AVL: fail with `FIN_019`.
- Active manual PO already exists for the PR: fail with `FIN_018` or return idempotency replay if the same request key created it.
- PR converted callback fails after PO insert: keep PO in `DRAFT`, keep callback outbox retryable, and block `send` until the callback is `DELIVERED`.

## Error Codes To Implement

| Code | HTTP | Meaning |
|---|---|---|
| `FIN_017` | 422 | Purchase request is not eligible for manual PO creation |
| `FIN_018` | 409 | Active purchase order already exists for the purchase request |
| `FIN_019` | 422 | Vendor is not eligible for PO creation |

## Database Impact For Manual PO

Required next migration in `finance-service`:
- Add a partial unique index for active manual PO by PR:

```sql
CREATE UNIQUE INDEX ux_purchase_orders_manual_pr_active
    ON finance.purchase_orders (pr_id)
    WHERE rfq_id IS NULL AND is_deleted = FALSE;
```

- Add a durable callback outbox for PR conversion:

```sql
CREATE TABLE finance.po_pr_conversion_callbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id UUID NOT NULL REFERENCES finance.purchase_orders(id),
    pr_id UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ NULL,
    delivered_at TIMESTAMPTZ NULL,
    last_error TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_po_pr_conversion_callbacks_status CHECK (status IN (
        'PENDING', 'DELIVERED', 'FAILED_RETRYABLE', 'FAILED_EXHAUSTED'
    ))
);

CREATE UNIQUE INDEX ux_po_pr_conversion_callbacks_po
    ON finance.po_pr_conversion_callbacks (po_id);
```

No PR-line partial quantity ledger is added in the MVP. Add a separate ledger before supporting partial/split PO.

## Next Coding Slices

1. Add PR `po-source` and `converted-to-po` internal endpoints plus tests.
2. Add Vendor `po-source` internal endpoint plus tests.
3. Add Finance manual PO migration, duplicate guard, callback outbox, dispatcher, and tests.
4. Add Finance manual PO create use case/controller/adapters/tests.
5. Wire PR converted callback outbox after RFQ-award PO creation if not already handled.
6. Add frontend manual PO creation UI after backend contracts are green.
