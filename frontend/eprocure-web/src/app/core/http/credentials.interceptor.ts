import { HttpInterceptorFn } from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (request, next) => {
  const requestId = request.headers.get('X-Request-Id') ?? crypto.randomUUID();

  return next(
    request.clone({
      withCredentials: true,
      setHeaders: {
        'X-Request-Id': requestId
      }
    })
  );
};
