import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  AdminUserDetail,
  AdminUserSummary,
  AssignRolesRequest,
  ChangeUserStatusRequest,
  CreateUserRequest,
  UpdateUserRequest,
  UserListFilter
} from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  private mapUser<T>(user: any): T {
    if (!user) return user;
    return {
      ...user,
      departmentId: user.department?.id || null,
      departmentName: user.department?.name || null
    } as T;
  }

  list(filter: UserListFilter): Observable<ApiResponse<AdminUserSummary[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.q) params = params.set('q', filter.q);
    if (filter.departmentId) params = params.set('department_id', filter.departmentId);
    if (filter.status) params = params.set('status', filter.status);

    return this.http.get<ApiResponse<AdminUserSummary[]> & { meta: PageMeta }>(
      `${this.baseUrl}/users`,
      { params, withCredentials: true }
    ).pipe(
      map(response => {
        if (response.success && Array.isArray(response.data)) {
          return {
            ...response,
            data: response.data.map(user => this.mapUser<AdminUserSummary>(user))
          };
        }
        return response;
      })
    );
  }

  getById(id: string): Observable<ApiResponse<AdminUserDetail>> {
    return this.http.get<ApiResponse<AdminUserDetail>>(
      `${this.baseUrl}/users/${id}`,
      { withCredentials: true }
    ).pipe(
      map(response => {
        if (response.success && response.data) {
          return {
            ...response,
            data: this.mapUser<AdminUserDetail>(response.data)
          };
        }
        return response;
      })
    );
  }

  create(request: CreateUserRequest): Observable<ApiResponse<AdminUserDetail>> {
    return this.http.post<ApiResponse<AdminUserDetail>>(
      `${this.baseUrl}/users`,
      request,
      { withCredentials: true }
    ).pipe(
      map(response => {
        if (response.success && response.data) {
          return {
            ...response,
            data: this.mapUser<AdminUserDetail>(response.data)
          };
        }
        return response;
      })
    );
  }

  update(id: string, request: UpdateUserRequest): Observable<ApiResponse<AdminUserDetail>> {
    return this.http.put<ApiResponse<AdminUserDetail>>(
      `${this.baseUrl}/users/${id}`,
      request,
      { withCredentials: true }
    ).pipe(
      map(response => {
        if (response.success && response.data) {
          return {
            ...response,
            data: this.mapUser<AdminUserDetail>(response.data)
          };
        }
        return response;
      })
    );
  }

  assignRoles(id: string, roles: string[]): Observable<ApiResponse<void>> {
    const request: AssignRolesRequest = { roles };
    return this.http.post<ApiResponse<void>>(
      `${this.baseUrl}/users/${id}/roles`,
      request,
      { withCredentials: true }
    );
  }

  changeStatus(id: string, status: 'ACTIVE' | 'INACTIVE' | 'LOCKED', reason?: string): Observable<ApiResponse<void>> {
    const request: ChangeUserStatusRequest = { status, reason: reason || null };
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/users/${id}/status`,
      request,
      { withCredentials: true }
    );
  }
}
