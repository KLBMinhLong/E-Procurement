import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AdminPermission, AdminRole } from '../models/admin.model';

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
    return this.http.get<ApiResponse<AdminPermission[]>>(
      `${this.baseUrl}/permissions`,
      { withCredentials: true }
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
      { permissionCodes },
      { withCredentials: true }
    );
  }
}
