## ADR-013 — Timezone và Số liệu tài chính

**Status:** Accepted  
**Date:** 2025-01  

### Quyết định

**Timezone:**
- DB: lưu tất cả timestamp dạng UTC (`TIMESTAMPTZ`)
- Application: `TZ=Asia/Ho_Chi_Minh` trong Docker env
- API response: trả về UTC ISO-8601, FE convert sang local timezone
- SLA calculation: dùng business calendar library, tính giờ làm việc 8:00–17:30 Thứ 2–Thứ 6

**Số liệu tài chính:**
- DB: lưu dạng `NUMERIC(19,4)` — không dùng FLOAT/DOUBLE
- Java: dùng `BigDecimal` — không dùng `double`
- API: trả về dạng string `"123456789.0000"` để tránh rounding error
- Display: FE dùng `ep-amount` component với font-mono