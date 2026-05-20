import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  AdminUserDetail,
  AdminUserSummary,
  ChangeUserStatusRequest,
  CreateUserRequest,
  UpdateUserRequest,
  UserListFilter
} from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: UserListFilter): Observable<ApiResponse<AdminUserSummary[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.q) params = params.set('q', filter.q);
    if (filter.departmentId) params = params.set('departmentId', filter.departmentId);
    if (filter.status) params = params.set('status', filter.status);

    return this.http.get<ApiResponse<AdminUserSummary[]> & { meta: PageMeta }>(
      `${this.baseUrl}/users`,
      { params, withCredentials: true }
    );
  }

  getById(id: string): Observable<ApiResponse<AdminUserDetail>> {
    return this.http.get<ApiResponse<AdminUserDetail>>(
      `${this.baseUrl}/users/${id}`,
      { withCredentials: true }
    );
  }

  create(request: CreateUserRequest): Observable<ApiResponse<AdminUserDetail>> {
    return this.http.post<ApiResponse<AdminUserDetail>>(
      `${this.baseUrl}/users`,
      request,
      { withCredentials: true }
    );
  }

  update(id: string, request: UpdateUserRequest): Observable<ApiResponse<AdminUserDetail>> {
    return this.http.put<ApiResponse<AdminUserDetail>>(
      `${this.baseUrl}/users/${id}`,
      request,
      { withCredentials: true }
    );
  }

  changeStatus(id: string, status: 'ACTIVE' | 'INACTIVE' | 'LOCKED'): Observable<ApiResponse<void>> {
    const request: ChangeUserStatusRequest = { status };
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/users/${id}/status`,
      request,
      { withCredentials: true }
    );
  }
}
