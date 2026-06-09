import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  AwardRfqRequest,
  AwardRfqResponse,
  CreateRfqRequest,
  EvaluateQuoteRequest,
  RfqDetail,
  RfqListFilter,
  SubmitQuoteRequest,
  VendorQuote
} from '../models/vendor.model';

@Injectable({ providedIn: 'root' })
export class RfqService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filter: RfqListFilter): Observable<ApiResponse<RfqDetail[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size)
      .set('sort', filter.sort);

    if (filter.status) params = params.set('status', filter.status);
    if (filter.prId) params = params.set('pr_id', filter.prId);

    return this.http.get<ApiResponse<RfqDetail[]> & { meta: PageMeta }>(
      `${this.baseUrl}/rfq`,
      { params, withCredentials: true }
    );
  }

  getById(id: string): Observable<ApiResponse<RfqDetail>> {
    return this.http.get<ApiResponse<RfqDetail>>(
      `${this.baseUrl}/rfq/${id}`,
      { withCredentials: true }
    );
  }

  create(request: CreateRfqRequest): Observable<ApiResponse<RfqDetail>> {
    return this.http.post<ApiResponse<RfqDetail>>(
      `${this.baseUrl}/rfq`,
      request,
      { withCredentials: true }
    );
  }

  close(id: string): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(
      `${this.baseUrl}/rfq/${id}/close`,
      {},
      { withCredentials: true }
    );
  }

  submitQuote(rfqId: string, request: SubmitQuoteRequest): Observable<ApiResponse<VendorQuote>> {
    return this.http.post<ApiResponse<VendorQuote>>(
      `${this.baseUrl}/rfq/${rfqId}/quotes`,
      request,
      { withCredentials: true }
    );
  }

  evaluateQuote(
    rfqId: string,
    quoteId: string,
    request: EvaluateQuoteRequest
  ): Observable<ApiResponse<VendorQuote>> {
    return this.http.post<ApiResponse<VendorQuote>>(
      `${this.baseUrl}/rfq/${rfqId}/quotes/${quoteId}/evaluate`,
      request,
      { withCredentials: true }
    );
  }

  award(rfqId: string, request: AwardRfqRequest): Observable<ApiResponse<AwardRfqResponse>> {
    return this.http.post<ApiResponse<AwardRfqResponse>>(
      `${this.baseUrl}/rfq/${rfqId}/award`,
      request,
      { withCredentials: true }
    );
  }
}
