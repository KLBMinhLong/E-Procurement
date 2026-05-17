## ADR-003 — Mã hoá Payload Hỗn hợp RSA+AES

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Cần mã hoá payload request/response để tránh MITM dù đã có HTTPS. Đặc biệt quan trọng với dữ liệu nhạy cảm (password, thông tin tài chính).

### Quyết định
Sử dụng **Hybrid Encryption (RSA-2048 + AES-256-GCM)**:

```
Mỗi Request từ FE:
  aesKey = generateAES256()
  encryptedBody = AES_GCM_encrypt(body, aesKey)
  encryptedKey  = RSA_encrypt(aesKey, BE_PublicKey)
  send: { encryptedBody, encryptedKey }

BE xử lý:
  aesKey = RSA_decrypt(encryptedKey, BE_PrivateKey)
  body   = AES_GCM_decrypt(encryptedBody, aesKey)
```

**Feature flag:** `ENCRYPTION_ENABLED=false` tắt hoàn toàn ở môi trường `local` và `dev`.

**Implementation:** Custom Spring `HandlerInterceptor` để tự động mã hoá/giải mã, trong suốt với business logic.

### Hậu quả
- (+) Defense-in-depth: bảo vệ ngay cả khi TLS bị compromise
- (+) Transparent với developer qua interceptor
- (-) Overhead CPU nhỏ cho RSA encryption mỗi request
- (-) Phức tạp hơn khi debug → toggle được giải quyết bằng env var
