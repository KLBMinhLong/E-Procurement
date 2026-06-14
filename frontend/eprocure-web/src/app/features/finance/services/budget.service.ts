import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  BudgetDashboard,
  BudgetListFilter,
  BudgetOverrideRequest,
  BudgetOverrideResult,
  BudgetTransferRequest,
  BudgetTransferResult
} from '../models/budget.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: BudgetListFilter): Observable<ApiResponse<BudgetDashboard[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.sort) params = params.set('sort', filter.sort);
    if (filter.department_id) params = params.set('department_id', filter.department_id);
    if (filter.fiscal_year) params = params.set('fiscal_year', filter.fiscal_year);
    if (filter.quarter) params = params.set('quarter', filter.quarter);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.gl_account_code) params = params.set('gl_account_code', filter.gl_account_code);

    return this.http.get<ApiResponse<BudgetDashboard[]> & { meta: PageMeta }>(
      `${this.baseUrl}/budgets`,
      { params, withCredentials: true }
    );
  }

  getDashboard(id: string): Observable<ApiResponse<BudgetDashboard>> {
    return this.http.get<ApiResponse<BudgetDashboard>>(
      `${this.baseUrl}/budgets/${id}/dashboard`,
      { withCredentials: true }
    );
  }

  override(id: string, request: BudgetOverrideRequest): Observable<ApiResponse<BudgetOverrideResult>> {
    return this.http.patch<ApiResponse<BudgetOverrideResult>>(
      `${this.baseUrl}/budgets/${id}/override-approval`,
      request,
      { withCredentials: true }
    );
  }

  transfer(id: string, request: BudgetTransferRequest): Observable<ApiResponse<BudgetTransferResult>> {
    return this.http.patch<ApiResponse<BudgetTransferResult>>(
      `${this.baseUrl}/budgets/${id}/transfer`,
      request,
      { withCredentials: true }
    );
  }
}
