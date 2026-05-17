# Security Encryption (RSA + AES)

## Decision
- Use hybrid encryption: RSA-2048 + AES-256-GCM
- Implemented via Spring HandlerInterceptor
- Feature flag: ENCRYPTION_ENABLED=false in local/dev

## Request flow
1) FE generates AES-256-GCM key
2) FE encrypts payload with AES (iv required)
3) FE encrypts AES key with BE RSA public key
4) FE sends encryptedPayload + encryptedAesKey + iv + keyVersion

## Response flow
1) BE generates a new AES key per response
2) BE encrypts response payload with AES
3) BE encrypts AES key with FE RSA public key
4) Response returns encryptedResponse + encryptedAesKey + iv

## Public key endpoint
GET /api/v1/auth/public-key
- Response includes publicKey, keyVersion, algorithm
