## ADR-010 — Spring Boot Profiles Strategy

**Status:** Accepted  
**Date:** 2025-01  

### Quyết định

| Profile | Mục đích | Đặc điểm |
|---|---|---|
| `application.yml` | Base config | Cấu hình chung mọi môi trường |
| `application-local.yml` | Dev chạy ngoài Docker | Point đến localhost services, encryption OFF |
| `application-dev.yml` | Dev chạy trong Docker | Log DEBUG, tắt tính năng không cần, encryption OFF |
| `application-prod.yml` | Full production | Log INFO/WARN, tất cả tính năng ON, encryption ON |

**Nguyên tắc:**
- KHÔNG hardcode credential trong file yml — dùng environment variable
- Dùng `${ENV_VAR:default_value}` syntax
- Sensitive config: chỉ tồn tại trong prod env vars, không commit lên git