## ADR-002 — Custom Opaque Token thay vì JWT Session

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Keycloak mặc định phát hành JWT chứa claims về user. Yêu cầu: 1 phiên đăng nhập duy nhất, token có thể thu hồi, không expose thông tin user trong token.

### Quyết định
Không dùng Keycloak JWT trực tiếp với Frontend. Thay vào đó:

1. **Keycloak** chỉ làm nhiệm vụ **xác thực credential** (verify username/password qua Custom Provider)
2. **IAM Service** sau khi nhận OK từ Keycloak → sinh **opaque token** (UUID + hash ngẫu nhiên)
3. Token được lưu trong **Redis** (primary) và **DB** (backup/audit)
4. Mọi request từ FE đều dùng opaque token này
5. IAM Service verify token bằng Redis lookup, không dùng JWT verification

```java
// Token format (không phải JWT)
String token = UUID.randomUUID().toString().replace("-","") 
             + SecureRandom.alphanumeric(32);
// Lưu: Redis key = "session:{token}", value = userId + roles + metadata
```

### Hậu quả
- (+) Hoàn toàn kiểm soát session: thu hồi ngay lập tức, 1 phiên/user
- (+) Token không chứa thông tin → không thể decode → bảo mật cao hơn
- (+) Dễ implement "forced logout" khi đăng nhập từ thiết bị khác
- (-) Mỗi request phải Redis lookup (giải quyết bằng cache hot path)
- (-) Stateful → phức tạp hơn stateless JWT khi scale
- **Giải pháp:** Redis cluster, TTL sliding window, connection pool tối ưu