## ADR-012 — Idempotency cho Mọi Mutating Request

**Status:** Accepted  
**Date:** 2025-01  

### Quyết định

Tất cả POST/PUT request phải hỗ trợ idempotency qua `Idempotency-Key` header:

```
Header: Idempotency-Key: <uuid-v4>

Flow:
1. FE gửi request với Idempotency-Key
2. BE check Redis: key "idempotent:{key}" tồn tại không?
   - Tồn tại → trả về response đã cache (HTTP 200 + cached result)
   - Không tồn tại → xử lý, lưu result vào Redis (TTL 24h), trả về kết quả
```

**Lý do:** Tránh duplicate khi network retry hoặc user double-click.