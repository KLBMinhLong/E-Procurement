import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ApiErrorResponse } from '../models/api-response.model';
import { ToastService } from '../services/toast.service';
import { EncryptionService } from './encryption.service';
import { BYPASS_ERROR_INTERCEPTOR_TOKEN } from './http-context-tokens';

import { AuthService } from '../auth/auth.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const toast = inject(ToastService);
  const encryptionService = inject(EncryptionService);
  const authService = inject(AuthService);
  const bypassErrorInterceptor = request.context.get(BYPASS_ERROR_INTERCEPTOR_TOKEN);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      // Skip redirect handling nếu request có flag bypassErrorInterceptor
      if (bypassErrorInterceptor === true) {
        return throwError(() => error);
      }

      const body = error.error as ApiErrorResponse | undefined;
      const code = body?.code;
      const isPasswordChange = request.url.includes('/users/me/password');
 
      if (error.status === 423 || code === 'IAM_002') {
        // Account bị khóa → đá về trang login với thông báo riêng
        authService.clearSession();
        router.navigate(['/login'], { queryParams: { reason: 'locked' } });
      } else if ((error.status === 401 || code === 'IAM_003') && !isPasswordChange) {
        authService.clearSession();
        router.navigate(['/login']);
      } else if (error.status === 403 || code === 'IAM_004') {
        router.navigate(['/forbidden']);
      } else if (code === 'GW_001') {
        toast.warningKey('error.rateLimit');
      } else if (code === 'SYS_003') {
        encryptionService.clearPublicKeyCache();
        toast.error('error.encryptionKeyExpired');
      } else if (isPasswordChange) {
        // Let profile.component.ts handle the toast for password change
      } else {
        toast.error(body?.message ?? 'error.generic');
      }

      return throwError(() => error);
    })
  );
};
