import { inject, Injectable, signal } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { ChangePasswordRequest, ForgotPasswordRequest, LoginRequest, PublicKeyResponse, ResetPasswordRequest, UserContext, LoginResponse, UserSummaryView } from '../models/user-context.model';
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

  login(request: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    this.isFullyHydrated.set(false);
    return this.api.post<LoginResponse>('/auth/login', request).pipe(
      tap((response) => {
        if (!response.data.requiresTwoFactor) {
          // Temporarily set a dummy UserContext until authGuard hydrates it
          this.currentUser.set({
            id: response.data.userId,
            fullName: response.data.fullName,
            avatarUrl: response.data.avatarUrl,
            employeeCode: '',
            username: request.username,
            email: '',
            phone: null,
            department: null,
            roles: [],
            permissions: [],
            twoFactorEnabled: false,
            lastLoginAt: null
          });
        }
      })
    );
  }

  verifyTwoFactor(code: string): Observable<ApiResponse<UserSummaryView>> {
    return this.api.post<UserSummaryView>('/auth/two-factor/verify', { code }).pipe(
      tap((response) => {
        this.currentUser.set({
          id: response.data.id,
          fullName: response.data.fullName,
          avatarUrl: response.data.avatarUrl,
          employeeCode: response.data.employeeCode,
          username: response.data.username,
          email: response.data.email,
          phone: response.data.phone,
          department: response.data.department,
          roles: response.data.roles,
          permissions: [],
          twoFactorEnabled: true,
          lastLoginAt: null
        });
      })
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

  updateProfile(request: { fullName: string; phone: string; avatarUrl: string | null }): Observable<ApiResponse<UserContext>> {
    return this.api.put<UserContext>('/users/me', request).pipe(
      tap((response) => this.currentUser.set(response.data))
    );
  }

  changePassword(request: ChangePasswordRequest): Observable<ApiResponse<null>> {
    return this.api.put<null>('/users/me/password', request);
  }

  logout(): Observable<ApiResponse<null>> {
    return this.api.post<null>('/auth/logout', {}).pipe(
      tap(() => this.clearSession())
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<null>> {
    return this.api.post<null>('/auth/forgot-password', request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<null>> {
    return this.api.post<null>('/auth/reset-password', request, undefined, {
      context: BYPASS_ERROR_INTERCEPTOR
    });
  }

  enableTwoFactor(): Observable<ApiResponse<{ secret: string; qrCodeUrl: string; manualEntryKey: string }>> {
    return this.api.put<{ secret: string; qrCodeUrl: string; manualEntryKey: string }>('/users/me/two-factor/enable', {});
  }

  confirmTwoFactor(code: string): Observable<ApiResponse<{ backupCodes: string[] }>> {
    return this.api.put<{ backupCodes: string[] }>('/users/me/two-factor/confirm', { code });
  }

  disableTwoFactor(): Observable<ApiResponse<null>> {
    return this.api.put<null>('/users/me/two-factor/disable', {});
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
