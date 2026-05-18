import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ApiErrorResponse } from '../models/api-response.model';
import { ToastService } from '../services/toast.service';
import { EncryptionService } from './encryption.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const toast = inject(ToastService);
  const encryptionService = inject(EncryptionService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const body = error.error as ApiErrorResponse | undefined;
      const code = body?.code;

      if (error.status === 401 || code === 'IAM_003' || code === 'IAM_005') {
        router.navigate(['/login']);
      } else if (error.status === 403 || code === 'IAM_004') {
        router.navigate(['/forbidden']);
      } else if (code === 'GW_001') {
        toast.warningKey('error.rateLimit');
      } else if (code === 'SYS_003') {
        encryptionService.clearPublicKeyCache();
        toast.error('error.encryptionKeyExpired');
      } else {
        toast.error(body?.message ?? 'error.generic');
      }

      return throwError(() => error);
    })
  );
};
