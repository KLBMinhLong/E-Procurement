## SK-10 · Encryption Interceptor

### Trigger
Agent implement hoặc modify encryption/decryption của request/response.

### Inputs Required
- RSA/AES key source
- Env toggle name
- Paths cần skip

### Rules
```
[R1] Mã hoá là tùy chọn — toggle bằng ENCRYPTION_ENABLED env var
[R2] Chỉ active ở prod — dev/local tắt mặc định
[R3] Mô hình: RSA + AES hybrid (mỗi request một AES key mới)
[R4] FE gửi: { encryptedPayload, encryptedAesKey }
[R5] BE trả: { encryptedResponse, encryptedAesKey } (dùng FE public key)
[R6] RSA key exchange khi FE init session (GET /api/v1/auth/public-key)
[R7] Interceptor implement HandlerInterceptor — không sửa controller
```

### Template
```java
@Component
@ConditionalOnProperty(name = "app.encryption.enabled", havingValue = "true")
public class EncryptionInterceptor implements HandlerInterceptor {

    private static final Logger log = LogManager.getLogger(EncryptionInterceptor.class);

    private final RsaEncryptionService rsaService;
    private final AesEncryptionService aesService;
    private final FrontendKeyStore feKeyStore;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (shouldSkip(request)) return true;

        try {
            String body = readBody(request);
            if (body.isBlank()) return true;

            EncryptedRequest encrypted = objectMapper.readValue(body, EncryptedRequest.class);
            String aesKey = rsaService.decrypt(encrypted.getEncryptedAesKey());
            String plainBody = aesService.decrypt(encrypted.getEncryptedPayload(), aesKey);

            // Wrap request với plain body để controller đọc được
            RequestWrapper wrappedRequest = new RequestWrapper(request, plainBody.getBytes());
            // Store wrapped request in attribute
            request.setAttribute("wrappedRequest", wrappedRequest);
            return true;
        } catch (Exception e) {
            log.warn("[ENCRYPTION] Decrypt failed | path={} | error={}", request.getRequestURI(), e.getMessage());
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid encrypted payload");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        // Encrypt response body using FE's public key
        // Implementation: wrap HttpServletResponse với ContentCachingResponseWrapper
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/public-key") ||
               path.startsWith("/actuator/") ||
               request.getMethod().equals("GET");
    }
}
```

### Checklist
```
[ ] Toggle bằng env var, dev/local tắt mặc định
[ ] GET/actuator/crypto paths được skip
[ ] Interceptor không sửa controller
```
