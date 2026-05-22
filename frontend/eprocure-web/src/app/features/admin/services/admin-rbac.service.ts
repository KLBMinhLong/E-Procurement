import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AdminPermission, AdminRole, CreateRolePayload, UpdateRolePayload } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminRbacService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getRoles(): Observable<ApiResponse<AdminRole[]>> {
    return this.http.get<ApiResponse<AdminRole[]>>(
      `${this.baseUrl}/roles`,
      { withCredentials: true }
    );
  }

  getPermissions(): Observable<ApiResponse<AdminPermission[]>> {
    return this.http.get<ApiResponse<any>>(
      `${this.baseUrl}/permissions`,
      { withCredentials: true }
    ).pipe(
      map(response => {
        if (response.success && response.data) {
          const flatPermissions: AdminPermission[] = [];
          for (const [moduleName, permList] of Object.entries(response.data)) {
            if (Array.isArray(permList)) {
              for (const perm of permList) {
                flatPermissions.push({
                  code: perm.code,
                  name: perm.name,
                  description: perm.description || null,
                  module: moduleName
                });
              }
            }
          }
          return {
            ...response,
            data: flatPermissions
          } as ApiResponse<AdminPermission[]>;
        }
        return response;
      })
    );
  }

  getRolePermissions(roleCode: string): Observable<ApiResponse<string[]>> {
    return this.http.get<ApiResponse<string[]>>(
      `${this.baseUrl}/roles/${roleCode}/permissions`,
      { withCredentials: true }
    );
  }

  updateRolePermissions(roleCode: string, permissionCodes: string[]): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${this.baseUrl}/roles/${roleCode}/permissions`,
      { permissions: permissionCodes },
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': crypto.randomUUID() }
      }
    );
  }

  createRole(payload: CreateRolePayload): Observable<ApiResponse<AdminRole>> {
    return this.http.post<ApiResponse<AdminRole>>(
      `${this.baseUrl}/roles`,
      payload,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': crypto.randomUUID() }
      }
    );
  }

  updateRole(currentCode: string, payload: UpdateRolePayload): Observable<ApiResponse<AdminRole>> {
    return this.http.put<ApiResponse<AdminRole>>(
      `${this.baseUrl}/roles/${currentCode}`,
      payload,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': crypto.randomUUID() }
      }
    );
  }
}
