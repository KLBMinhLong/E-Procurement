import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  ApproveInvoiceRequest,
  ConfirmPaymentRequest,
  CreateInvoiceRequest,
  DisputeInvoiceRequest,
  Invoice,
  InvoiceActionResponse,
  InvoiceListFilter,
  Payment,
  RunInvoiceMatchResponse
} from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: InvoiceListFilter): Observable<ApiResponse<Invoice[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.status) params = params.set('status', filter.status);
    if (filter.vendor_id) params = params.set('vendor_id', filter.vendor_id);
    if (filter.po_id) params = params.set('po_id', filter.po_id);
    if (filter.overdue_only) params = params.set('overdue_only', true);

    return this.http.get<ApiResponse<Invoice[]> & { meta: PageMeta }>(
      `${this.baseUrl}/invoices`,
      { params, withCredentials: true }
    );
  }

  getById(id: string): Observable<ApiResponse<Invoice>> {
    return this.http.get<ApiResponse<Invoice>>(
      `${this.baseUrl}/invoices/${id}`,
      { withCredentials: true }
    );
  }

  create(request: CreateInvoiceRequest): Observable<ApiResponse<Invoice>> {
    return this.http.post<ApiResponse<Invoice>>(
      `${this.baseUrl}/invoices`,
      request,
      { withCredentials: true }
    );
  }

  match(id: string): Observable<ApiResponse<RunInvoiceMatchResponse>> {
    return this.http.post<ApiResponse<RunInvoiceMatchResponse>>(
      `${this.baseUrl}/invoices/${id}/match`,
      {},
      { withCredentials: true }
    );
  }

  approve(id: string, request: ApproveInvoiceRequest): Observable<ApiResponse<InvoiceActionResponse>> {
    return this.http.post<ApiResponse<InvoiceActionResponse>>(
      `${this.baseUrl}/invoices/${id}/approve`,
      request,
      { withCredentials: true }
    );
  }

  dispute(id: string, request: DisputeInvoiceRequest): Observable<ApiResponse<InvoiceActionResponse>> {
    return this.http.post<ApiResponse<InvoiceActionResponse>>(
      `${this.baseUrl}/invoices/${id}/dispute`,
      request,
      { withCredentials: true }
    );
  }

  confirmPayment(id: string, request: ConfirmPaymentRequest): Observable<ApiResponse<Payment>> {
    return this.http.post<ApiResponse<Payment>>(
      `${this.baseUrl}/invoices/${id}/confirm-payment`,
      request,
      { withCredentials: true }
    );
  }
}
