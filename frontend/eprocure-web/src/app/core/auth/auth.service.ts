import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { ForgotPasswordRequest, LoginRequest, PublicKeyResponse, UserContext } from '../models/user-context.model';
import { ApiService } from '../http/api.service';
import { API_BASE_URL } from '../http/api-tokens';
import { EncryptionService } from '../http/encryption.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly encryptionService = inject(EncryptionService);

  readonly currentUser = signal<UserContext | null>(null);
  readonly isHydrating = signal(false);

  loadPublicKey(): Observable<ApiResponse<PublicKeyResponse>> {
    return this.api.get<PublicKeyResponse>('/auth/public-key');
  }

  login(request: LoginRequest): Observable<ApiResponse<UserContext>> {
    return this.api.post<UserContext>('/auth/login', request).pipe(
      tap((response) => this.currentUser.set(response.data))
    );
  }

  hydrateUserContext(): Observable<ApiResponse<UserContext>> {
    this.isHydrating.set(true);

    return this.api.get<UserContext>('/users/me').pipe(
      tap({
        next: (response) => {
          this.currentUser.set(response.data);
          this.isHydrating.set(false);
        },
        error: () => {
          this.currentUser.set(null);
          this.isHydrating.set(false);
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
  }

  refreshPublicKey(): Promise<PublicKeyResponse> {
    this.encryptionService.clearPublicKeyCache();
    return this.encryptionService.loadPublicKey();
  }
}
