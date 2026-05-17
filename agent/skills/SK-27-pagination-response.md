## SK-27 · Pagination Response

### Trigger
Agent cần trả response phân trang đồng nhất giữa backend và frontend.

### Inputs Required
- items list
- totalCount
- current page + page size

### Rules
```
[R1] totalPages = ceil(totalElements / size)
[R2] page là 1-based (page >= 1)
[R3] Meta fields: page, size, totalElements, totalPages, isFirst, isLast, sort
```

### Template — Backend
```java
public record PageMeta(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast,
    String sort
) {
    public static PageMeta of(long totalElements, int page, int size, String sort) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isFirst = page <= 1;
        boolean isLast = page >= totalPages;
        return new PageMeta(page, size, totalElements, totalPages, isFirst, isLast, sort);
    }
}

// Use with ApiResponse envelope
// ApiResponse.success("OK", dataList, PageMeta.of(total, page, size, sort))
```

### Template — Angular
```typescript
export interface PageMeta {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    isFirst: boolean;
    isLast: boolean;
    sort: string;
}
```

### Checklist
```
[ ] totalPages tính đúng theo totalElements/size
[ ] page là 1-based
[ ] BE/FE dùng cùng meta field names
```
