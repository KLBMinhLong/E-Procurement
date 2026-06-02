import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { API_BASE_URL } from './api-tokens';

type QueryValue = string | number | boolean | null | undefined;

type ApiRequestOptions = {
  context?: HttpContext;
};

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  get<T>(path: string, params?: Record<string, QueryValue>, options?: ApiRequestOptions): Observable<ApiResponse<T>> {
    return this.http.get<ApiResponse<T>>(this.url(path), {
      params: this.toParams(params),
      context: options?.context
    });
  }

  post<T>(
    path: string,
    body: unknown,
    idempotencyKey?: string,
    options?: ApiRequestOptions
  ): Observable<ApiResponse<T>> {
    return this.http.post<ApiResponse<T>>(this.url(path), body, {
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined,
      context: options?.context
    });
  }

  put<T>(path: string, body: unknown, idempotencyKey?: string): Observable<ApiResponse<T>> {
    return this.http.put<ApiResponse<T>>(this.url(path), body, {
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined
    });
  }

  patch<T>(path: string, body: unknown, idempotencyKey?: string): Observable<ApiResponse<T>> {
    return this.http.patch<ApiResponse<T>>(this.url(path), body, {
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined
    });
  }

  private url(path: string): string {
    return `${this.baseUrl}${path.startsWith('/') ? path : `/${path}`}`;
  }

  private toParams(params?: Record<string, QueryValue>): HttpParams {
    let httpParams = new HttpParams();

    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return httpParams;
  }
}
