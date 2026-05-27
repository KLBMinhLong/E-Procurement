import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  ApprovalInboxFilter,
  ApprovalInboxCount,
  ApprovalTaskSummary,
  ApprovalTaskDetail,
  ApproveTaskRequest,
  RejectTaskRequest,
  RequestChangesRequest,
  ForwardTaskRequest,
  ApprovalProcessDetail
} from '../models/approvals.model';

@Injectable({ providedIn: 'root' })
export class ApprovalsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getInbox(filter: ApprovalInboxFilter): Observable<ApiResponse<ApprovalTaskSummary[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page.toString())
      .set('size', filter.size.toString())
      .set('sort', filter.sort);

    if (filter.priority) params = params.set('priority', filter.priority);
    if (filter.entity_type) params = params.set('entity_type', filter.entity_type);
    if (filter.min_amount) params = params.set('min_amount', filter.min_amount);
    if (filter.is_overdue !== undefined && filter.is_overdue !== null) {
      params = params.set('is_overdue', filter.is_overdue.toString());
    }

    return this.http.get<ApiResponse<ApprovalTaskSummary[]> & { meta: PageMeta }>(
      `${this.baseUrl}/approvals/inbox`,
      { params, withCredentials: true }
    );
  }

  getInboxCount(): Observable<ApiResponse<ApprovalInboxCount>> {
    return this.http.get<ApiResponse<ApprovalInboxCount>>(
      `${this.baseUrl}/approvals/inbox/count`,
      { withCredentials: true }
    );
  }

  getTaskDetail(taskId: string): Observable<ApiResponse<ApprovalTaskDetail>> {
    return this.http.get<ApiResponse<ApprovalTaskDetail>>(
      `${this.baseUrl}/approvals/tasks/${taskId}`,
      { withCredentials: true }
    );
  }

  approveTask(taskId: string, request: ApproveTaskRequest): Observable<ApiResponse<{ nextStep: any; isCompleted: boolean }>> {
    return this.http.patch<ApiResponse<{ nextStep: any; isCompleted: boolean }>>(
      `${this.baseUrl}/approvals/tasks/${taskId}/approve`,
      request,
      { withCredentials: true }
    );
  }

  rejectTask(taskId: string, request: RejectTaskRequest): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/approvals/tasks/${taskId}/reject`,
      request,
      { withCredentials: true }
    );
  }

  requestChanges(taskId: string, request: RequestChangesRequest): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/approvals/tasks/${taskId}/request-changes`,
      request,
      { withCredentials: true }
    );
  }

  forwardTask(taskId: string, request: ForwardTaskRequest): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/approvals/tasks/${taskId}/forward`,
      request,
      { withCredentials: true }
    );
  }

  getProcessDetail(entityType: string, entityId: string): Observable<ApiResponse<ApprovalProcessDetail>> {
    return this.http.get<ApiResponse<ApprovalProcessDetail>>(
      `${this.baseUrl}/approvals/processes/${entityType}/${entityId}`,
      { withCredentials: true }
    );
  }
}
