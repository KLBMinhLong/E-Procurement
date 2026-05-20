import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  AttachmentInfo,
  CancelPrRequest,
  CreatePrRequest,
  PrListFilter,
  PurchaseRequestDetail,
  PurchaseRequestSummary,
  UpdatePrRequest
} from '../models/purchase-request.model';

export interface PrListResponse {
  data: PurchaseRequestSummary[];
  meta: PageMeta;
}

export interface CreatePrResponse {
  id: string;
  prNumber: string;
  status: string;
}

export interface SubmitPrResponse {
  id: string;
  prNumber: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class PurchaseRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: PrListFilter): Observable<ApiResponse<PurchaseRequestSummary[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size)
      .set('sort', filter.sort);

    if (filter.status) params = params.set('status', filter.status);
    if (filter.priority) params = params.set('priority', filter.priority);
    if (filter.q) params = params.set('q', filter.q);
    if (filter.from_date) params = params.set('from_date', filter.from_date);
    if (filter.to_date) params = params.set('to_date', filter.to_date);
    if (filter.min_amount) params = params.set('min_amount', filter.min_amount);
    if (filter.max_amount) params = params.set('max_amount', filter.max_amount);
    if (filter.department_id) params = params.set('department_id', filter.department_id);
    if (filter.requester_id) params = params.set('requester_id', filter.requester_id);

    return this.http.get<ApiResponse<PurchaseRequestSummary[]> & { meta: PageMeta }>(
      `${this.baseUrl}/purchase-requests`,
      { params, withCredentials: true }
    );
  }

  getById(id: string): Observable<ApiResponse<PurchaseRequestDetail>> {
    return this.http.get<ApiResponse<PurchaseRequestDetail>>(
      `${this.baseUrl}/purchase-requests/${id}`,
      { withCredentials: true }
    );
  }

  create(request: CreatePrRequest): Observable<ApiResponse<CreatePrResponse>> {
    return this.http.post<ApiResponse<CreatePrResponse>>(
      `${this.baseUrl}/purchase-requests`,
      request,
      { withCredentials: true }
      // Idempotency-Key auto-added by idempotencyInterceptor
    );
  }

  update(id: string, request: UpdatePrRequest): Observable<ApiResponse<PurchaseRequestDetail>> {
    return this.http.put<ApiResponse<PurchaseRequestDetail>>(
      `${this.baseUrl}/purchase-requests/${id}`,
      request,
      { withCredentials: true }
    );
  }

  submit(id: string): Observable<ApiResponse<SubmitPrResponse>> {
    return this.http.patch<ApiResponse<SubmitPrResponse>>(
      `${this.baseUrl}/purchase-requests/${id}/submit`,
      {},
      { withCredentials: true }
    );
  }

  cancel(id: string, request: CancelPrRequest): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/purchase-requests/${id}/cancel`,
      request,
      { withCredentials: true }
    );
  }

  uploadAttachment(file: File, description?: string): Observable<ApiResponse<AttachmentInfo>> {
    const formData = new FormData();
    formData.append('file', file);
    if (description) formData.append('description', description);

    return this.http.post<ApiResponse<AttachmentInfo>>(
      `${this.baseUrl}/purchase-requests/attachments/upload`,
      formData,
      { withCredentials: true }
    );
  }
}
