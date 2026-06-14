import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  CreateVendorRequest,
  VendorDetail,
  VendorListFilter,
  VendorSummary
} from '../models/vendor.model';

@Injectable({ providedIn: 'root' })
export class VendorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: VendorListFilter): Observable<ApiResponse<VendorSummary[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size)
      .set('sort', filter.sort);

    if (filter.status) params = params.set('status', filter.status);
    if (filter.q) params = params.set('q', filter.q);
    if (filter.category) params = params.set('category', filter.category);
    if (filter.onAvlOnly) params = params.set('on_avl_only', 'true');

    return this.http.get<ApiResponse<VendorSummary[]> & { meta: PageMeta }>(
      `${this.baseUrl}/vendors`,
      { params, withCredentials: true }
    );
  }

  getById(id: string): Observable<ApiResponse<VendorDetail>> {
    return this.http.get<ApiResponse<VendorDetail>>(
      `${this.baseUrl}/vendors/${id}`,
      { withCredentials: true }
    );
  }

  create(request: CreateVendorRequest): Observable<ApiResponse<VendorDetail>> {
    return this.http.post<ApiResponse<VendorDetail>>(
      `${this.baseUrl}/vendors`,
      request,
      { withCredentials: true }
    );
  }

  approve(id: string): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(
      `${this.baseUrl}/vendors/${id}/approve`,
      {},
      { withCredentials: true }
    );
  }
}
