## SK-20 · Angular Guard & Interceptor

### Trigger
Agent implement guard/interceptor cho auth, permission, idempotency.

### Inputs Required
- Permission codes
- Idempotency header policy
- Auth cookie strategy

### Rules
```
[R1] Auth interceptor luôn set withCredentials
[R2] Idempotency-Key chỉ thêm cho POST/PUT/PATCH
[R3] Idempotency-Key phải ổn định theo ý định hành động (không tạo mới mỗi request)
[R4] Permission guard dùng permission code và redirect khi deny
```

### Template — Auth Interceptor
```typescript
// core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Cookie được gửi tự động (withCredentials)
  const authReq = req.clone({ withCredentials: true });
  return next(authReq);
};

// core/interceptors/idempotency.interceptor.ts
export const idempotencyInterceptor: HttpInterceptorFn = (req, next) => {
  if (!['POST', 'PUT', 'PATCH'].includes(req.method)) return next(req);

  // Neu request da co Idempotency-Key thi giu nguyen
  if (req.headers.has('Idempotency-Key')) {
    return next(req);
  }

  // Lay key tu context (set theo y dinh hanh dong)
  const contextKey = IdempotencyContext.get();
  if (!contextKey) {
    return next(req);
  }

  const idemReq = req.clone({
    setHeaders: { 'Idempotency-Key': contextKey }
  });
  return next(idemReq);
};

// Example context helper (pseudo)
export class IdempotencyContext {
  private static key: string | null = null;

  static init(): string {
    this.key = crypto.randomUUID();
    return this.key;
  }

  static get(): string | null {
    return this.key;
  }

  static clear(): void {
    this.key = null;
  }
}
```

### Template — Permission Guard
```typescript
// core/guards/permission.guard.ts
export const permissionGuard = (requiredPermission: string): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.hasPermission(requiredPermission)) {
      return true;
    }

    router.navigate(['/forbidden']);
    return false;
  };
};

// Dùng trong route config:
{
  path: 'purchase-requests/new',
  component: CreatePurchaseRequestComponent,
  canActivate: [permissionGuard('PR_CREATE')]
}
```

### Checklist
```
[ ] withCredentials bật trong auth interceptor
[ ] Idempotency-Key chỉ áp dụng cho mutating requests
[ ] Guard kiểm tra permission code
```
