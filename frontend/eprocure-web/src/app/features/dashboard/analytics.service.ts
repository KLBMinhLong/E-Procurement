import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/http/api-tokens';
import { ApiResponse } from '../../core/models/api-response.model';
import {
  CycleTimeKpi,
  ExecutiveDashboard,
  ExecutiveDashboardFilter,
  ExportReportRequest,
  KpiFilter,
  ManagerDashboard,
  PurchasingDashboard,
  ReportJob,
  RequesterDashboard,
  SlaComplianceKpi
} from './analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getExecutiveDashboard(filter: ExecutiveDashboardFilter): Observable<ApiResponse<ExecutiveDashboard>> {
    let params = new HttpParams();
    if (filter.fiscal_year) params = params.set('fiscal_year', filter.fiscal_year);
    if (filter.quarter) params = params.set('quarter', filter.quarter);
    return this.http.get<ApiResponse<ExecutiveDashboard>>(
      `${this.baseUrl}/dashboard/executive`,
      { params, withCredentials: true }
    );
  }

  getManagerDashboard(): Observable<ApiResponse<ManagerDashboard>> {
    return this.http.get<ApiResponse<ManagerDashboard>>(
      `${this.baseUrl}/dashboard/manager`,
      { withCredentials: true }
    );
  }

  getPurchasingDashboard(): Observable<ApiResponse<PurchasingDashboard>> {
    return this.http.get<ApiResponse<PurchasingDashboard>>(
      `${this.baseUrl}/dashboard/purchasing`,
      { withCredentials: true }
    );
  }

  getRequesterDashboard(): Observable<ApiResponse<RequesterDashboard>> {
    return this.http.get<ApiResponse<RequesterDashboard>>(
      `${this.baseUrl}/dashboard/requester`,
      { withCredentials: true }
    );
  }

  getCycleTimeKpi(filter: KpiFilter): Observable<ApiResponse<CycleTimeKpi>> {
    let params = new HttpParams()
      .set('from_date', filter.from_date)
      .set('to_date', filter.to_date);
    if (filter.department_id) params = params.set('department_id', filter.department_id);
    return this.http.get<ApiResponse<CycleTimeKpi>>(
      `${this.baseUrl}/kpi/cycle-time`,
      { params, withCredentials: true }
    );
  }

  getSlaComplianceKpi(filter: KpiFilter): Observable<ApiResponse<SlaComplianceKpi>> {
    const params = new HttpParams()
      .set('from_date', filter.from_date)
      .set('to_date', filter.to_date);
    return this.http.get<ApiResponse<SlaComplianceKpi>>(
      `${this.baseUrl}/kpi/sla-compliance`,
      { params, withCredentials: true }
    );
  }

  exportReport(request: ExportReportRequest): Observable<ApiResponse<ReportJob>> {
    return this.http.post<ApiResponse<ReportJob>>(
      `${this.baseUrl}/reports/export`,
      request,
      { withCredentials: true }
    );
  }

  getReportJob(jobId: string): Observable<ApiResponse<ReportJob>> {
    return this.http.get<ApiResponse<ReportJob>>(
      `${this.baseUrl}/reports/jobs/${jobId}`,
      { withCredentials: true }
    );
  }

  downloadReport(jobId: string): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/reports/jobs/${jobId}/download`,
      { withCredentials: true, responseType: 'blob' }
    );
  }
}
