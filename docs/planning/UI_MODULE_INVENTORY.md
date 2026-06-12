# Kế hoạch hoàn thiện UI Module Inventory (E08)

> Mục tiêu: biến Inventory thành module vận hành kho dùng được end-to-end cho người dùng kho, purchasing, finance/admin, đồng bộ với backend `inventory-service`, Angular design system và các invariant của dự án.

## 1. Nguồn đối chiếu đã kiểm

- Backend hiện có trong `services/inventory-service`:
  - `GET /api/v1/warehouses`
  - `GET /api/v1/items/{itemCode}/stock`
  - `GET /api/v1/warehouses/{id}/stock`
  - `GET /api/v1/stock/movements`
  - `POST /api/v1/stock/issue-out`
  - `GET /api/v1/goods-receipts`
  - `POST /api/v1/goods-receipts`
  - `GET /api/v1/goods-receipts/{id}`
  - `POST /api/v1/goods-receipts/{id}/complete`
- Frontend hiện có trong `frontend/eprocure-web/src/app/features/inventory`:
  - `/inventory/goods-receipts`
  - `/inventory/goods-receipts/create`
  - `/inventory/goods-receipts/:id`
  - Services hiện có: `GoodsReceiptService`, `WarehouseService`.
- OpenAPI có khai báo nhưng backend code chưa implement:
  - `GET /api/v1/items`
  - `POST /api/v1/items`
  - `GET /api/v1/items/{itemCode}`
  - `PUT /api/v1/items/{itemCode}`
  - `PUT /api/v1/goods-receipts/{id}`
  - `POST /api/v1/stock/adjustment`
- Permission vocabulary hợp lệ:
  - `GR_VIEW`: xem GR, warehouse stock, stock movements.
  - `GR_CREATE`: tạo/complete GR.
  - `GR_ISSUE_OUT`: cấp phát/xuất kho.
  - `ADMIN_CATALOG_MANAGE`: quản lý catalog/adjustment.
  - Không dùng `STOCK_VIEW` cho route mới nếu chưa thêm permission này vào IAM seed/vocabulary.

## 2. Nguyên tắc thiết kế module

- Đây là operational UI, không phải landing page: dense, scan nhanh, ưu tiên bảng dữ liệu, filter, trạng thái và action rõ ràng.
- Dùng shared UI hiện có: `ep-breadcrumb`, `ep-button`, `ep-badge`, `ep-table` nếu phù hợp, `ep-empty-state`, `ep-skeleton`, `ep-modal`, `ep-form-field`, `ep-amount`.
- Mọi component standalone, `ChangeDetectionStrategy.OnPush`, state bằng signal/computed, subscription có `takeUntilDestroyed(this.destroyRef)`.
- Mọi text hiển thị dùng i18n key trong `assets/i18n/vi.json` và `en.json`.
- SCSS dùng CSS custom properties `var(--...)`; không hardcode màu/hex trong component.
- Mọi HTTP request gửi `withCredentials: true`; mọi POST/PUT/PATCH gửi `Idempotency-Key`.
- Không tạo HTTP DELETE. Deactivate/cancel/complete/adjust là state transition có audit/idempotency.
- FE không tự suy đoán business rule quan trọng: backend vẫn là source of truth cho tồn kho, idempotency, duplicate và quantity validation.

## 3. Information architecture đề xuất

| Route | Quyền | Trạng thái | Ghi chú |
|---|---|---|---|
| `/inventory` | any of `GR_VIEW`, `GR_CREATE`, `GR_ISSUE_OUT`, `ADMIN_CATALOG_MANAGE` | Cần chỉnh | Redirect hoặc shell tab tới GR list |
| `/inventory/goods-receipts` | `GR_VIEW` | Đã có, cần polish | Danh sách GR + filter |
| `/inventory/goods-receipts/create` | `GR_CREATE` | Đã có, cần polish | Tạo DRAFT GR từ PO đã issue |
| `/inventory/goods-receipts/:id` | `GR_VIEW` | Đã có, cần polish | Detail + complete nếu DRAFT |
| `/inventory/stock` | `GR_VIEW` | Cần thêm FE | Tồn kho theo warehouse |
| `/inventory/stock/movements` | `GR_VIEW` | Cần thêm FE | Ledger read-only |
| `/inventory/issue-out` | `GR_ISSUE_OUT` | Cần thêm FE | Xuất kho nội bộ |
| `/inventory/catalog` | `ADMIN_CATALOG_MANAGE` | Cần backend + FE | Catalog item admin |
| `/inventory/catalog/:itemCode` | `ADMIN_CATALOG_MANAGE` hoặc `GR_VIEW` tùy scope | Cần backend + FE | Chi tiết item + stock summary |

Ghi chú permission: route cha trong `app.routes.ts` nên bỏ `STOCK_VIEW` hoặc thêm permission thật vào IAM trước. Theo backend hiện tại, stock read đang được bảo vệ bằng `GR_VIEW`.

## 4. Phân hệ Goods Receipt (GR)

### 4.1 GR List

- Route: `/inventory/goods-receipts`
- API: `GET /api/v1/goods-receipts`
- Permission: `GR_VIEW`
- Filter:
  - `status`: `DRAFT`, `PARTIAL`, `COMPLETE`, `DISCREPANCY`
  - `warehouse_id`
  - `po_id` nếu UI có search/quick filter theo PO
  - `from_date`, `to_date`
  - `page`, `size`
- UI:
  - Bảng GR: `grNumber`, `po.poNumber`, `warehouse.name`, `receivedAt`, `status`, tổng line/accepted/rejected nếu FE tính từ response.
  - Badge status theo tone: DRAFT neutral, PARTIAL warning, COMPLETE success, DISCREPANCY danger.
  - CTA tạo GR chỉ hiện với `GR_CREATE`.
  - Empty/loading/error state dùng component shared.
- Acceptance:
  - Filter reset về page 1.
  - Page index bắt đầu từ 1.
  - Không hardcode label/status text trong template.

### 4.2 Create GR

- Route: `/inventory/goods-receipts/create`
- API:
  - `GET /api/v1/warehouses`
  - `GET /api/v1/purchase-orders` từ finance-service, FE lọc client-side `SENT_TO_VENDOR | PARTIALLY_RECEIVED` cho đến khi có API filter riêng.
  - `POST /api/v1/goods-receipts`
- Permission: `GR_CREATE`
- Form:
  - Warehouse UUID bắt buộc, không dùng text/code thay cho `warehouseId`.
  - PO bắt buộc.
  - Line items render từ PO line snapshots: item name, ordered quantity, unit, unit price.
  - User nhập `receivedQuantity`, `rejectedQuantity`, `rejectionReason`, `lotNumber`.
  - `rejectionReason` bắt buộc khi `rejectedQuantity > 0`.
  - Chỉ submit các line có `receivedQuantity > 0` hoặc `rejectedQuantity > 0`.
- Backend behavior:
  - POST tạo DRAFT GR, chưa tăng tồn kho.
  - Backend validate PO snapshot, warehouse active, line thuộc PO, duplicate line và tolerance.
- Gap cần sửa FE hiện tại:
  - Create page đang chưa expose `rejectedQuantity`, `rejectionReason`, `lotNumber`.
  - Cần giữ quantity là string trong request để khớp money/quantity precision convention.

### 4.3 GR Detail & Complete

- Route: `/inventory/goods-receipts/:id`
- API:
  - `GET /api/v1/goods-receipts/{id}`
  - `POST /api/v1/goods-receipts/{id}/complete`
- Permission:
  - View: `GR_VIEW`
  - Complete: `GR_CREATE`
- UI:
  - Header: GR number, PO number, warehouse, keeper, receivedAt, status.
  - Line table: item, ordered, received, rejected, lot, rejection reason.
  - Complete button chỉ hiện khi status `DRAFT` và có `GR_CREATE`.
  - Sau complete hiển thị response summary: `grStatus`, `movementsCreated`, `updatedStocks`.
- Backend behavior:
  - Complete chỉ cho DRAFT.
  - Tạo `RECEIPT_IN` movement, update `stock_entries`, publish `inventory.gr.created` cho finance 3-way match.
  - Nếu không resolve được active catalog item thì fail `INV_001`.
- Gap backend:
  - `PUT /api/v1/goods-receipts/{id}` có trong OpenAPI nhưng chưa có controller/use case. Không bật UI edit draft trước khi implement backend.

## 5. Phân hệ Stock Dashboard

### 5.1 Warehouse Stock

- Route: `/inventory/stock`
- API:
  - `GET /api/v1/warehouses`
  - `GET /api/v1/warehouses/{id}/stock?below_reorder=&page=&size=`
- Permission: `GR_VIEW`
- UI:
  - Warehouse selector bắt buộc hoặc default warehouse đầu tiên.
  - Toggle `below_reorder` để xem mặt hàng dưới ngưỡng.
  - KPI cards nhỏ: total SKU, below reorder count, total quantity rows, last updated mới nhất.
  - Bảng stock: itemCode, itemName, quantityOnHand, unit, reorderPoint, isBelowReorder, lastUpdated.
  - Row action:
    - Xem movement của item.
    - Issue out nếu có `GR_ISSUE_OUT`.
    - Adjustment nếu có `ADMIN_CATALOG_MANAGE` và backend đã có API.
- Gap FE hiện tại:
  - `StockEntry` model đang dùng `reorderLevel`; backend response là `reorderPoint`, `isBelowReorder`, `lastUpdated`. Cần sửa model trước khi build UI.

### 5.2 Item Stock

- API: `GET /api/v1/items/{itemCode}/stock?warehouse_id=`
- Permission: `GR_VIEW`
- UI usage:
  - Dùng trong item detail/catalog detail.
  - Dùng để kiểm tra nhanh khi issue-out.
  - Hiển thị tồn kho item trên tất cả kho, không sửa trực tiếp tại đây.

### 5.3 Stock Movements Ledger

- Route: `/inventory/stock/movements`
- API: `GET /api/v1/stock/movements`
- Permission: `GR_VIEW`
- Filter:
  - `item_code`
  - `warehouse_id`
  - `movement_type`: `RECEIPT_IN`, `ISSUE_OUT`, `ADJUSTMENT`, `TRANSFER`
  - `from_date`, `to_date`
  - `page`, `size`
- UI:
  - Read-only ledger, không có edit/delete.
  - Quantity hiển thị signed: `ISSUE_OUT` là số âm.
  - Source ref link nếu sourceRefType có route tương ứng:
    - `GOODS_RECEIPT` -> `/inventory/goods-receipts/{sourceRefId}`
    - `STOCK_ISSUE_OUT` -> tạm thời hiển thị ID, route detail sẽ là follow-up nếu backend có.
  - `performedBy.fullName` hiển thị theo response; backend hiện có có thể fallback UUID string cho đến khi IAM enrichment xong.

## 6. Phân hệ Issue Out

- Route: `/inventory/issue-out`
- API:
  - `GET /api/v1/warehouses`
  - `GET /api/v1/warehouses/{id}/stock`
  - `POST /api/v1/stock/issue-out`
- Permission: `GR_ISSUE_OUT`
- Form:
  - Chọn warehouse.
  - Chọn recipient. Nếu chưa có employee lookup phù hợp permission warehouse, MVP có thể nhập UUID có validate format; bản hoàn chỉnh cần IAM people picker/department member API.
  - Optional `prId`.
  - Dynamic line items: itemCode, available quantity, quantity, unit.
  - Client-side chặn quantity > available để UX tốt hơn, backend vẫn validate atomic và rollback nếu thiếu tồn.
  - Notes tối đa theo backend request.
- Request:
  - `warehouseId`, `recipientId`, `prId?`, `items[]`, `notes?`
  - `quantity` gửi dạng string/decimal safe, không dùng floating point cho tính toán nghiệp vụ.
- Response:
  - Hiển thị movements vừa tạo, balanceAfter mới, link sang ledger filter theo source.
- Acceptance:
  - Double click/retry không tạo duplicate do có `Idempotency-Key`.
  - Lỗi `INV_003` tồn kho không đủ phải hiển thị message thân thiện từ backend.

## 7. Phân hệ Catalog Management

### 7.1 Phạm vi nghiệp vụ

- Catalog item là master data của inventory-service, khác với PR catalog cũ trong `pr.catalog_items`.
- Inventory completion cần active item để resolve `itemCode`; catalog UI là bắt buộc nếu muốn vận hành dữ liệu thật, không chỉ seed.
- Không quản lý bằng cách ghi trực tiếp DB hoặc cross-service DB.

### 7.2 Backend cần implement trước UI

- Domain/application/infrastructure/presentation theo Clean Architecture:
  - `Item` domain model.
  - `ItemRepository`.
  - Use cases: `SearchItemsUseCase`, `GetItemDetailUseCase`, `CreateItemUseCase`, `UpdateItemUseCase`.
  - MyBatis mapper query có `is_deleted = false`, order whitelist, pagination.
  - Controller:
    - `GET /api/v1/items`
    - `GET /api/v1/items/{itemCode}`
    - `POST /api/v1/items`
    - `PUT /api/v1/items/{itemCode}`
- Permission:
  - Read/search có thể là authenticated/`GR_VIEW` nếu dùng cho warehouse flow.
  - Create/update dùng `ADMIN_CATALOG_MANAGE`.
- Idempotency:
  - POST/PUT bắt buộc `Idempotency-Key`.
- Data:
  - Money/price là `BigDecimal`/`NUMERIC(19,4)` và string trong JSON.
  - Deactivate không DELETE; OpenAPI hiện có dùng `PUT` với `isActive`.
- Tests:
  - Duplicate itemCode -> `INV_007`.
  - Get missing item -> `INV_001`.
  - Search filters `q`, `category_code`, `is_active`, `below_reorder`.
  - Update inactive/price/reorderPoint.

### 7.3 UI sau khi backend có

- Route: `/inventory/catalog`
- Permission: `ADMIN_CATALOG_MANAGE` cho admin CRUD; read-only có thể mở bằng `GR_VIEW` nếu backend cho phép.
- List:
  - Search q, category, active/inactive, below reorder.
  - Columns: itemCode, name, categoryCode, unit, unitPrice, reorderPoint, isActive, stock summary.
- Detail:
  - Item metadata.
  - Stock summary all warehouses từ `stockSummary` hoặc `GET /items/{itemCode}/stock`.
  - Movement quick link filter theo item.
- Create/Edit:
  - Fields: itemCode, name, description, categoryCode, unit, unitPrice.amount/currency, preferredVendorId, reorderPoint, isActive.
  - Validate itemCode max length theo OpenAPI, price/reorderPoint non-negative.
  - Không hardcode vendor list; preferredVendorId picker chỉ làm khi vendor lookup phù hợp permission đã có.

## 8. Stock Adjustment / Kiểm kê

- OpenAPI có `POST /api/v1/stock/adjustment`, backend code chưa implement.
- Không bật UI trước khi có backend use case vì đây là mutation tài chính/kho có rủi ro.
- Backend cần implement:
  - `AdjustStockUseCase` với `@Transactional`.
  - Validate active warehouse/item, unit consistency, reason min length.
  - Lock/update stock atomically.
  - Tạo `ADJUSTMENT` movement signed theo delta và `balanceAfter`.
  - Idempotency-Key required.
  - Permission `ADMIN_CATALOG_MANAGE`.
  - Unit tests cho increase, decrease, duplicate idempotency, missing item/warehouse, negative invalid.
- UI:
  - Modal từ `/inventory/stock`.
  - Form: warehouse, itemCode, currentQuantity read-only, newQuantity, reason.
  - Sau success reload stock và link sang movement ledger.

## 9. Backend/OpenAPI drift cần xử lý

- `docs/api/inventory-service.openapi.yaml` đang khai báo nhiều endpoint chưa có trong code. Khi bắt đầu implement:
  - Nếu quyết định làm đầy đủ module: implement các endpoint còn thiếu theo spec và test.
  - Nếu tạm thời chưa làm: ghi rõ trong planning/tracker là "planned, not implemented"; không để FE gọi endpoint chưa tồn tại.
- `docs/api/common/API_SPECIFICATION.md` có ví dụ `POST /stock/issue-out` đặt `recipientId` trong từng item, nhưng backend hiện có dùng `recipientId` ở request header-level. Khi chạm API docs, cần đồng bộ lại theo backend/OpenAPI chính.
- Frontend route/sidebar đang nhắc `STOCK_VIEW`; permission này không nằm trong glossary/IAM seed hiện tại. Cần bỏ hoặc thêm permission thật theo đúng quy trình IAM.

## 10. Roadmap thực hiện từng bước

### Slice 0: Contract cleanup nhỏ trước khi thêm UI

- [x] Sửa FE `StockEntry` model: `reorderPoint`, `isBelowReorder`, `lastUpdated`.
- [x] Thêm `StockService` cho warehouse stock, item stock, movements, issue-out.
- [x] Chuẩn hóa route/sidebar permission Inventory: dùng permission thật (`GR_VIEW`, `GR_CREATE`, `GR_ISSUE_OUT`, `ADMIN_CATALOG_MANAGE`), không đưa thêm `STOCK_VIEW` nếu chưa seed.
- [x] Bổ sung i18n route/nav keys cho các trang stock, movements, issue-out, catalog.
- [x] Verify: `npm run build`.

### Slice 1: Stock Dashboard + Movements UI

- [x] Thêm routes `/inventory/stock` và `/inventory/stock/movements`.
- [x] Xây warehouse stock page dùng API đã có.
- [x] Xây immutable movement ledger dùng API đã có.
- [x] Link từ stock row sang movement filter.
- [x] Verify: `npm run build`; nếu có backend touched thì `mvn -pl services/inventory-service test`.

### Slice 2: Issue Out UI

- [x] Thêm route `/inventory/issue-out`.
- [x] Dùng warehouse stock làm item selector và available quantity guard.
- [x] POST `/stock/issue-out` với `Idempotency-Key`.
- [x] Hiển thị movements response và reload stock/movement.
- [x] Verify: `npm run build`.

### Slice 3: Polish GR hiện có

- [x] Create GR: thêm rejected quantity, rejection reason, lot number.
- [x] GR detail: hiển thị complete response summary và link stock/movement.
- [x] GR list: thêm `po_id` quick filter nếu cần.
- [x] Không thêm draft edit UI cho đến khi backend `PUT /goods-receipts/{id}` tồn tại.
- [x] Verify: `npm run build`.

### Slice 4: Catalog backend

- [x] Implement Item domain/repository/use cases/controller theo mục 7.
- [x] Đồng bộ OpenAPI nếu response/request khác spec.
- [x] Unit tests backend.
- [x] Verify: `mvn -pl services/inventory-service test`, `git diff --check`.

### Slice 5: Catalog UI

- [x] Thêm `/inventory/catalog` và `/inventory/catalog/:itemCode`.
- [x] Thêm `CatalogService`, models, list/detail/create/edit modal/page.
- [x] Permission gate `ADMIN_CATALOG_MANAGE` cho mutation, read route dùng `GR_VIEW` theo backend.
- [x] Verify: `npm run build`, i18n JSON parse.

### Slice 6: GR draft edit backend + UI

- Implement backend `PUT /goods-receipts/{id}` chỉ cho DRAFT.
- Thêm UI edit line item trong detail nếu DRAFT.
- Idempotency và tests bắt buộc.
- Verify: `mvn -pl services/inventory-service test`, `npm run build`.

### Slice 7: Stock Adjustment backend + UI

- Implement backend `POST /stock/adjustment`.
- Thêm modal adjustment từ stock dashboard.
- Verify: `mvn -pl services/inventory-service test`, `npm run build`.

### Slice 8: E2E/runtime hardening

- Cập nhật smoke path nếu thêm issue-out/catalog/adjustment vào user flow.
- Chạy tối thiểu:
  - `mvn -pl services/inventory-service test`
  - `npm run build` trong `frontend/eprocure-web`
  - `git diff --check`
- Nếu cần verify runtime: chạy smoke qua gateway với user có `WAREHOUSE`/`SUPER_ADMIN`, không test protected route khi chưa có authenticated session.

## 11. Definition of Done cho module Inventory

- Warehouse user có thể:
  - Tạo GR từ PO đã issue.
  - Complete GR và thấy stock tăng.
  - Xem stock theo warehouse và movement ledger.
  - Issue out stock nội bộ và thấy stock giảm.
- Admin/catalog manager có thể:
  - Tạo/cập nhật/deactivate item master không dùng DB tay.
  - Điều chỉnh tồn kho có reason, movement ledger và audit/idempotency.
- Finance flow không bị phá:
  - Complete GR vẫn publish `inventory.gr.created` để invoice 3-way match.
- Security/convention:
  - Không HTTP DELETE.
  - Mutating endpoint có `Idempotency-Key`.
  - Permission code đúng vocabulary.
  - FE không hardcode text/màu.
  - Build/test tối thiểu pass theo slice.
