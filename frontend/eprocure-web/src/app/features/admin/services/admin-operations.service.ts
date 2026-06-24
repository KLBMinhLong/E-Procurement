import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  ActiveSession,
  AuditExportJob,
  AuditExportRequest,
  AuditLogEntry,
  AuditLogFilter,
  CatalogCategoryAdmin,
  CatalogCategoryRequest,
  DepartmentAdmin,
  DepartmentAdminRequest,
  EncryptionKeyRotationRequest,
  EncryptionKeyRotationResult,
  ServiceConfig,
  ServiceConfigSummary,
  ServiceConfigUpdateRequest,
  ServiceConfigUpdateResult,
  ServiceRestartRequest,
  ServiceRestartResult,
  SessionListFilter,
  SystemHealthData
} from '../models/admin-operations.model';

type QueryValue = string | number | boolean | null | undefined;

@Injectable({ providedIn: 'root' })
export class AdminOperationsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  // ── System Health ──────────────────────────────────────────────
  getSystemHealth(): Observable<ApiResponse<SystemHealthData>> {
    return this.http.get<ApiResponse<SystemHealthData>>(
      `${this.baseUrl}/admin/health`,
      { withCredentials: true }
    );
  }

  // ── Service Config ─────────────────────────────────────────────
  listServiceConfigs(): Observable<ApiResponse<ServiceConfig[]> & { summary: ServiceConfigSummary }> {
    return this.http.get<ApiResponse<ServiceConfig[]> & { summary: ServiceConfigSummary }>(
      `${this.baseUrl}/admin/config/services`,
      { withCredentials: true }
    );
  }

  getServiceConfig(serviceName: string): Observable<ApiResponse<ServiceConfig>> {
    return this.http.get<ApiResponse<ServiceConfig>>(
      `${this.baseUrl}/admin/config/services/${encodeURIComponent(serviceName)}`,
      { withCredentials: true }
    );
  }

  updateServiceConfig(
    serviceName: string,
    request: ServiceConfigUpdateRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<ServiceConfigUpdateResult>> {
    return this.http.put<ApiResponse<ServiceConfigUpdateResult>>(
      `${this.baseUrl}/admin/config/services/${encodeURIComponent(serviceName)}`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  restartService(
    serviceName: string,
    request: ServiceRestartRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<ServiceRestartResult>> {
    return this.http.post<ApiResponse<ServiceRestartResult>>(
      `${this.baseUrl}/admin/config/services/${encodeURIComponent(serviceName)}/restart`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  rotateEncryptionKey(
    request: EncryptionKeyRotationRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<EncryptionKeyRotationResult>> {
    return this.http.post<ApiResponse<EncryptionKeyRotationResult>>(
      `${this.baseUrl}/admin/config/encryption/rotate-key`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  // ── Audit Log ──────────────────────────────────────────────────
  queryAuditLog(filter: AuditLogFilter): Observable<ApiResponse<AuditLogEntry[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('from_time', filter.from_time)
      .set('to_time', filter.to_time)
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.actor_id) params = params.set('actor_id', filter.actor_id);
    if (filter.entity_type) params = params.set('entity_type', filter.entity_type);
    if (filter.entity_id) params = params.set('entity_id', filter.entity_id);
    if (filter.action) params = params.set('action', filter.action);
    if (filter.service_name) params = params.set('service_name', filter.service_name);
    if (filter.is_success !== undefined) params = params.set('is_success', filter.is_success);

    return this.http.get<ApiResponse<AuditLogEntry[]> & { meta: PageMeta }>(
      `${this.baseUrl}/admin/audit-log`,
      { params, withCredentials: true }
    );
  }

  exportAuditLog(
    request: AuditExportRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<AuditExportJob>> {
    return this.http.post<ApiResponse<AuditExportJob>>(
      `${this.baseUrl}/admin/audit-log/export`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  getAuditExportJob(jobId: string): Observable<ApiResponse<AuditExportJob>> {
    return this.http.get<ApiResponse<AuditExportJob>>(
      `${this.baseUrl}/admin/audit-log/export/${encodeURIComponent(jobId)}`,
      { withCredentials: true }
    );
  }

  downloadAuditExport(jobId: string): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/admin/audit-log/export/${encodeURIComponent(jobId)}/download`,
      { withCredentials: true, responseType: 'blob' }
    );
  }

  // ── Active Sessions ────────────────────────────────────────────
  listActiveSessions(filter: SessionListFilter): Observable<ApiResponse<ActiveSession[]> & { meta: PageMeta }> {
    const params = this.toParams({
      user_id: filter.user_id,
      page: filter.page,
      size: filter.size
    });

    return this.http.get<ApiResponse<ActiveSession[]> & { meta: PageMeta }>(
      `${this.baseUrl}/admin/sessions`,
      { params, withCredentials: true }
    );
  }

  invalidateSession(
    sessionId: string,
    reason: string,
    idempotencyKey: string
  ): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/admin/sessions/${encodeURIComponent(sessionId)}/invalidate`,
      { reason },
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  // ── Catalog Category Admin ─────────────────────────────────────
  listCatalogCategories(includeInactive = false): Observable<ApiResponse<CatalogCategoryAdmin[]>> {
    const params = includeInactive ? new HttpParams().set('include_inactive', true) : undefined;
    return this.http.get<ApiResponse<CatalogCategoryAdmin[]>>(
      `${this.baseUrl}/admin/catalog/categories`,
      { params, withCredentials: true }
    );
  }

  createCatalogCategory(
    request: CatalogCategoryRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<CatalogCategoryAdmin>> {
    return this.http.post<ApiResponse<CatalogCategoryAdmin>>(
      `${this.baseUrl}/admin/catalog/categories`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  updateCatalogCategory(
    code: string,
    request: CatalogCategoryRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<CatalogCategoryAdmin>> {
    return this.http.put<ApiResponse<CatalogCategoryAdmin>>(
      `${this.baseUrl}/admin/catalog/categories/${encodeURIComponent(code)}`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  deactivateCatalogCategory(
    code: string,
    idempotencyKey: string
  ): Observable<ApiResponse<CatalogCategoryAdmin>> {
    return this.http.patch<ApiResponse<CatalogCategoryAdmin>>(
      `${this.baseUrl}/admin/catalog/categories/${encodeURIComponent(code)}/deactivate`,
      {},
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  // ── Department Admin ───────────────────────────────────────────
  createDepartment(
    request: DepartmentAdminRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<DepartmentAdmin>> {
    return this.http.post<ApiResponse<DepartmentAdmin>>(
      `${this.baseUrl}/admin/departments`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  updateDepartment(
    id: string,
    request: DepartmentAdminRequest,
    idempotencyKey: string
  ): Observable<ApiResponse<DepartmentAdmin>> {
    return this.http.put<ApiResponse<DepartmentAdmin>>(
      `${this.baseUrl}/admin/departments/${encodeURIComponent(id)}`,
      request,
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  deactivateDepartment(
    id: string,
    idempotencyKey: string
  ): Observable<ApiResponse<DepartmentAdmin>> {
    return this.http.patch<ApiResponse<DepartmentAdmin>>(
      `${this.baseUrl}/admin/departments/${encodeURIComponent(id)}/deactivate`,
      {},
      {
        withCredentials: true,
        headers: { 'Idempotency-Key': idempotencyKey }
      }
    );
  }

  // ── Helpers ────────────────────────────────────────────────────
  private toParams(params?: Record<string, QueryValue>): HttpParams {
    let httpParams = new HttpParams();
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
