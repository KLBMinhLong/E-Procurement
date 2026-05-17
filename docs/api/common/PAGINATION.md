# PAGINATION
## eProcure Enterprise — Quy ước phân trang

---

## 1. REQUEST PARAMETERS

Tất cả endpoint trả về danh sách đều hỗ trợ phân trang qua query params:

```
GET /api/v1/purchase-requests?page=1&size=20&sort=createdAt,desc

page   : int, 1-based, mặc định = 1
size   : int, mặc định = 20, tối đa = 100
sort   : string, format "{field},{direction}", direction = asc | desc
         Có thể sort nhiều field: sort=priority,desc&sort=createdAt,asc
```

### 1.1 Giá trị mặc định theo context

| Endpoint | Default size | Max size | Default sort |
|---|---|---|---|
| Approval inbox | 20 | 50 | `slaDeadline,asc` |
| Purchase requests | 20 | 100 | `createdAt,desc` |
| Vendors | 20 | 100 | `name,asc` |
| Notifications | 30 | 100 | `createdAt,desc` |
| Audit logs | 50 | 200 | `occurredAt,desc` |
| Reports/Analytics | 50 | 500 | `createdAt,desc` |

### 1.2 Sortable Fields

Mỗi service khai báo rõ field nào có thể sort trong OpenAPI spec. Cố gắng sort trên field không được phép → `VAL_003`.

---

## 2. RESPONSE META OBJECT

```jsonc
{
  "success": true,
  "code": "OK",
  "data": [ ... ],
  "meta": {
    "page": 1,              // Trang hiện tại (1-based)
    "size": 20,             // Số phần tử mỗi trang
    "totalElements": 156,   // Tổng số phần tử thỏa filter
    "totalPages": 8,        // Tổng số trang
    "isFirst": true,        // Đang ở trang đầu
    "isLast": false,        // Đang ở trang cuối
    "sort": "createdAt,desc"
  }
}
```

---

## 3. CURSOR-BASED PAGINATION (Notification & Realtime Feed)

Với các feed có update liên tục (notifications, activity stream), dùng cursor thay vì offset để tránh duplicate/skip khi có insert mới.

```
// Request
GET /api/v1/notifications?cursor=eyJpZCI6InV1aWQiLCJjcmVhdGVkQXQiOiIyMDI1In0=&size=30
// cursor = base64({"id":"uuid","createdAt":"2025-..."})

// Response meta khi dùng cursor
"meta": {
  "size": 30,
  "hasMore": true,
  "nextCursor": "eyJpZCI6InV1aWQyIiwiY3JlYXRlZEF0IjoiMjAyNSJ9",
  "prevCursor": null
}
```

---

## 4. EMPTY RESULT

```jsonc
// Không có lỗi, chỉ đơn giản là không có dữ liệu
{
  "success": true,
  "code": "OK",
  "data": [],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "isFirst": true,
    "isLast": true,
    "sort": "createdAt,desc"
  }
}
```

---

## 5. BACKEND IMPLEMENTATION (MyBatis)

```java
// PageRequest Value Object
public record PageRequest(int page, int size, List<SortOrder> sorts) {
    public PageRequest {
        if (page < 1)   throw new ValidationException("VAL_003", "page phải >= 1");
        if (size < 1)   throw new ValidationException("VAL_003", "size phải >= 1");
        if (size > 500) throw new ValidationException("VAL_003", "size tối đa là 500");
    }
    public int getOffset() { return (page - 1) * size; }
}

// Page<T> Result
public record Page<T>(List<T> content, PageMeta meta) {
    public static <T> Page<T> of(List<T> content, long total, PageRequest req) {
        int totalPages = (int) Math.ceil((double) total / req.size());
        return new Page<>(content, new PageMeta(req.page(), req.size(), total,
                totalPages, req.page() == 1, req.page() == totalPages,
                req.sorts().toString()));
    }
}

// MyBatis XML: Dynamic ORDER BY với whitelist
// mapper/PurchaseRequestMapper.xml
<select id="findByFilter" resultMap="prResult">
  SELECT *, COUNT(*) OVER() AS total_count
  FROM pr.purchase_requests
  WHERE is_deleted = false
  <if test="filter.status != null">AND status = #{filter.status}</if>
  <if test="filter.departmentId != null">AND department_id = #{filter.departmentId}</if>
  ORDER BY
  <choose>
    <when test="sort.field == 'createdAt'">created_at</when>
    <when test="sort.field == 'totalAmount'">total_amount</when>
    <when test="sort.field == 'slaDeadline'">sla_deadline</when>
    <otherwise>created_at</otherwise>      <!-- Fallback an toàn -->
  </choose>
  <if test="sort.direction == 'asc'">ASC</if>
  <if test="sort.direction != 'asc'">DESC</if>
  LIMIT #{page.size} OFFSET #{page.offset}
</select>
```

> ⚠️ **KHÔNG dùng dynamic ORDER BY từ string raw của user** — SQL Injection risk.  
> Luôn dùng whitelist mapping như ví dụ trên.

---

## 6. FRONTEND IMPLEMENTATION (Angular)

```typescript
// Shared pagination interface
export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  sort: string;
}

// ep-table component tự xử lý pagination
// Chỉ cần truyền vào meta và emit event khi user chuyển trang
<ep-table
  [data]="purchaseRequests"
  [meta]="meta"
  [columns]="columns"
  (pageChange)="onPageChange($event)"
  (sortChange)="onSortChange($event)">
</ep-table>

// Component handler
onPageChange(event: { page: number; size: number }) {
  this.loadData({ ...this.currentFilter, page: event.page, size: event.size });
}
onSortChange(event: { field: string; direction: 'asc' | 'desc' }) {
  this.currentSort = `${event.field},${event.direction}`;
  this.loadData({ ...this.currentFilter, sort: this.currentSort, page: 1 });
}
```

---

## 7. BREADCRUMB CONVENTION

Mọi trang danh sách đều có breadcrumb theo chuẩn:

```
Trang chủ > [Module] > [Sub-module] > [Action]

Ví dụ:
  Trang chủ > Mua sắm > Yêu cầu mua sắm
  Trang chủ > Mua sắm > Yêu cầu mua sắm > PR-2025-01-00001
  Trang chủ > Phê duyệt > Hộp thư đến > Chi tiết task
  Trang chủ > Quản trị > Người dùng > Chỉnh sửa

// ep-breadcrumb component — tự động đọc từ router state
<ep-breadcrumb></ep-breadcrumb>
```
