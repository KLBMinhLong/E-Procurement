import { HttpInterceptorFn, HttpParams } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { ENCRYPTION_ENABLED } from './api-tokens';
import { EncryptionService } from './encryption.service';

const STATE_CHANGING_METHODS = new Set(['POST', 'PUT', 'PATCH']);

export const encryptionInterceptor: HttpInterceptorFn = (request, next) => {
  const encryptionEnabled = inject(ENCRYPTION_ENABLED);
  const encryptionService = inject(EncryptionService);

  if (!encryptionEnabled || !shouldEncrypt(request.method, request.body)) {
    return next(request);
  }

  return from(encryptionService.encryptBody(request.body)).pipe(
    switchMap((encryptedBody) => next(request.clone({ body: encryptedBody })))
  );
};

function shouldEncrypt(method: string, body: unknown): boolean {
  if (!STATE_CHANGING_METHODS.has(method) || body === null || body === undefined) {
    return false;
  }

  if (
    body instanceof FormData
    || body instanceof Blob
    || body instanceof ArrayBuffer
    || body instanceof URLSearchParams
    || body instanceof HttpParams
  ) {
    return false;
  }

  if (isEncryptedRequest(body)) {
    return false;
  }

  return true;
}

function isEncryptedRequest(body: unknown): boolean {
  if (!body || typeof body !== 'object') {
    return false;
  }

  const candidate = body as Record<string, unknown>;
  return typeof candidate['encryptedPayload'] === 'string'
    && typeof candidate['encryptedAesKey'] === 'string'
    && typeof candidate['iv'] === 'string'
    && typeof candidate['keyVersion'] === 'string';
}
