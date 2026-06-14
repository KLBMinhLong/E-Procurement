import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { BYPASS_ERROR_INTERCEPTOR_TOKEN } from '../../../core/http/http-context-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  GoodsReceiptCreateCommand,
  GoodsReceiptDetail,
  GoodsReceiptUpdateCommand
} from '../models/goods-receipt.model';
import { CompleteGrResponse } from '../models/stock.model';

export interface GoodsReceiptListFilter {
  page: number;
  size: number;
  status?: string;
  po_id?: string;
  warehouse_id?: string;
  from_date?: string;
  to_date?: string;
}

@Injectable({ providedIn: 'root' })
export class GoodsReceiptService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: GoodsReceiptListFilter, context?: HttpContext): Observable<ApiResponse<GoodsReceiptDetail[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.status) params = params.set('status', filter.status);
    if (filter.po_id) params = params.set('po_id', filter.po_id);
    if (filter.warehouse_id) params = params.set('warehouse_id', filter.warehouse_id);
    if (filter.from_date) params = params.set('from_date', filter.from_date);
    if (filter.to_date) params = params.set('to_date', filter.to_date);

    return this.http.get<ApiResponse<GoodsReceiptDetail[]> & { meta: PageMeta }>(
      `${this.baseUrl}/goods-receipts`,
      { params, context, withCredentials: true }
    );
  }

  listCompletedByPoId(poId: string): Observable<ApiResponse<GoodsReceiptDetail[]> & { meta: PageMeta }> {
    const context = new HttpContext().set(BYPASS_ERROR_INTERCEPTOR_TOKEN, true);
    return this.list({
      page: 1,
      size: 50,
      status: 'COMPLETE',
      po_id: poId
    }, context);
  }

  getById(id: string): Observable<ApiResponse<GoodsReceiptDetail>> {
    return this.http.get<ApiResponse<GoodsReceiptDetail>>(
      `${this.baseUrl}/goods-receipts/${id}`,
      { withCredentials: true }
    );
  }

  create(command: GoodsReceiptCreateCommand, idempotencyKey: string): Observable<ApiResponse<GoodsReceiptDetail>> {
    return this.http.post<ApiResponse<GoodsReceiptDetail>>(
      `${this.baseUrl}/goods-receipts`,
      command,
      {
        headers: { 'Idempotency-Key': idempotencyKey },
        withCredentials: true
      }
    );
  }

  update(id: string, command: GoodsReceiptUpdateCommand, idempotencyKey: string): Observable<ApiResponse<GoodsReceiptDetail>> {
    return this.http.put<ApiResponse<GoodsReceiptDetail>>(
      `${this.baseUrl}/goods-receipts/${id}`,
      command,
      {
        headers: { 'Idempotency-Key': idempotencyKey },
        withCredentials: true
      }
    );
  }

  complete(id: string, idempotencyKey: string): Observable<ApiResponse<CompleteGrResponse>> {
    return this.http.post<ApiResponse<CompleteGrResponse>>(
      `${this.baseUrl}/goods-receipts/${id}/complete`,
      {},
      {
        headers: { 'Idempotency-Key': idempotencyKey },
        withCredentials: true
      }
    );
  }
}
