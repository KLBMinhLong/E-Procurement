import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AdminDepartment, CreateDepartmentRequest, UpdateDepartmentRequest } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminOrgService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getDepartments(): Observable<ApiResponse<AdminDepartment[]>> {
    return this.http.get<ApiResponse<AdminDepartment[]>>(
      `${this.baseUrl}/org/departments`,
      { withCredentials: true }
    );
  }

  createDepartment(request: CreateDepartmentRequest): Observable<ApiResponse<AdminDepartment>> {
    return this.http.post<ApiResponse<AdminDepartment>>(
      `${this.baseUrl}/org/departments`,
      request,
      { withCredentials: true }
    );
  }

  updateDepartment(id: string, request: UpdateDepartmentRequest): Observable<ApiResponse<AdminDepartment>> {
    return this.http.put<ApiResponse<AdminDepartment>>(
      `${this.baseUrl}/org/departments/${id}`,
      request,
      { withCredentials: true }
    );
  }
}
