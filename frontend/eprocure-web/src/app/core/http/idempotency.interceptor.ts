import { HttpInterceptorFn } from '@angular/common/http';

const STATE_CHANGING_METHODS = new Set(['POST', 'PUT', 'PATCH']);

export const idempotencyInterceptor: HttpInterceptorFn = (request, next) => {
  if (!STATE_CHANGING_METHODS.has(request.method) || request.headers.has('Idempotency-Key')) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        'Idempotency-Key': crypto.randomUUID()
      }
    })
  );
};
