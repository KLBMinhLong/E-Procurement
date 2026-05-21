import { inject, Injectable, signal } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { ForgotPasswordRequest, LoginRequest, PublicKeyResponse, UserContext } from '../models/user-context.model';
import { ApiService } from '../http/api.service';
import { API_BASE_URL } from '../http/api-tokens';
import { EncryptionService } from '../http/encryption.service';
import { BYPASS_ERROR_INTERCEPTOR_TOKEN } from '../http/http-context-tokens';

export const BYPASS_ERROR_INTERCEPTOR = new HttpContext().set(BYPASS_ERROR_INTERCEPTOR_TOKEN, true);

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly encryptionService = inject(EncryptionService);

  readonly currentUser = signal<UserContext | null>(null);
  readonly isHydrating = signal(false);
  readonly isFullyHydrated = signal(false);

  loadPublicKey(): Observable<ApiResponse<PublicKeyResponse>> {
    return this.api.get<PublicKeyResponse>('/auth/public-key');
  }

  login(request: LoginRequest): Observable<ApiResponse<UserContext>> {
    this.isFullyHydrated.set(false);
    return this.api.post<UserContext>('/auth/login', request).pipe(
      tap((response) => this.currentUser.set(response.data))
    );
  }

  hydrateUserContext(): Observable<ApiResponse<UserContext>> {
    this.isHydrating.set(true);

    return this.api.get<UserContext>('/users/me', undefined, {
      context: BYPASS_ERROR_INTERCEPTOR
    }).pipe(
      tap({
        next: (response) => {
          this.currentUser.set(response.data);
          this.isHydrating.set(false);
          this.isFullyHydrated.set(true);
        },
        error: () => {
          this.currentUser.set(null);
          this.isHydrating.set(false);
          this.isFullyHydrated.set(false);
        }
      })
    );
  }

  logout(): Observable<ApiResponse<null>> {
    return this.api.post<null>('/auth/logout', {}).pipe(
      tap(() => this.clearSession())
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<null>> {
    return this.api.post<null>('/auth/forgot-password', request);
  }

  googleLoginUrl(): string {
    return `${this.apiBaseUrl}/auth/oauth/google`;
  }

  clearSession(): void {
    this.currentUser.set(null);
    this.isFullyHydrated.set(false);
  }

  refreshPublicKey(): Promise<PublicKeyResponse> {
    this.encryptionService.clearPublicKeyCache();
    return this.encryptionService.loadPublicKey();
  }
}
