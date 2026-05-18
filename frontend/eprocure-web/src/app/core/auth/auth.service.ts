import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { LoginRequest, PublicKeyResponse, UserContext } from '../models/user-context.model';
import { ApiService } from '../http/api.service';
import { ENCRYPTION_ENABLED } from '../http/api-tokens';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly encryptionEnabled = inject(ENCRYPTION_ENABLED);

  readonly currentUser = signal<UserContext | null>(null);
  readonly isHydrating = signal(false);

  loadPublicKey(): Observable<ApiResponse<PublicKeyResponse>> {
    return this.api.get<PublicKeyResponse>('/auth/public-key');
  }

  login(request: LoginRequest): Observable<ApiResponse<UserContext>> {
    const payload = this.encryptionEnabled ? this.toEncryptedPlaceholder(request) : request;

    return this.api.post<UserContext>('/auth/login', payload).pipe(
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

  clearSession(): void {
    this.currentUser.set(null);
  }

  private toEncryptedPlaceholder(request: LoginRequest): LoginRequest {
    return request;
  }
}
