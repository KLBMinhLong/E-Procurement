import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  CancelPurchaseOrderRequest,
  CreatePurchaseOrderRequest,
  PurchaseOrder,
  PurchaseOrderListFilter,
  SendPurchaseOrderRequest,
  UpdatePurchaseOrderDraftRequest
} from '../models/purchase-order.model';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: PurchaseOrderListFilter): Observable<ApiResponse<PurchaseOrder[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size)
      .set('sort', filter.sort);

    if (filter.status) params = params.set('status', filter.status);
    if (filter.vendor_id) params = params.set('vendor_id', filter.vendor_id);
    if (filter.from_date) params = params.set('from_date', filter.from_date);
    if (filter.to_date) params = params.set('to_date', filter.to_date);

    return this.http.get<ApiResponse<PurchaseOrder[]> & { meta: PageMeta }>(
      `${this.baseUrl}/purchase-orders`,
      { params, withCredentials: true }
    );
  }

  getById(id: string): Observable<ApiResponse<PurchaseOrder>> {
    return this.http.get<ApiResponse<PurchaseOrder>>(
      `${this.baseUrl}/purchase-orders/${id}`,
      { withCredentials: true }
    );
  }

  create(request: CreatePurchaseOrderRequest): Observable<ApiResponse<PurchaseOrder>> {
    return this.http.post<ApiResponse<PurchaseOrder>>(
      `${this.baseUrl}/purchase-orders`,
      request,
      { withCredentials: true }
    );
  }

  updateDraft(id: string, request: UpdatePurchaseOrderDraftRequest): Observable<ApiResponse<PurchaseOrder>> {
    return this.http.patch<ApiResponse<PurchaseOrder>>(
      `${this.baseUrl}/purchase-orders/${id}`,
      request,
      { withCredentials: true }
    );
  }

  send(id: string, request: SendPurchaseOrderRequest): Observable<ApiResponse<PurchaseOrder>> {
    return this.http.post<ApiResponse<PurchaseOrder>>(
      `${this.baseUrl}/purchase-orders/${id}/send`,
      request,
      { withCredentials: true }
    );
  }

  cancel(id: string, request: CancelPurchaseOrderRequest): Observable<ApiResponse<PurchaseOrder>> {
    return this.http.patch<ApiResponse<PurchaseOrder>>(
      `${this.baseUrl}/purchase-orders/${id}/cancel`,
      request,
      { withCredentials: true }
    );
  }
}
