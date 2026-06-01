import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  NotificationTemplate,
  NotificationTemplateFilter,
  NotificationTemplatePreview,
  UpdateNotificationTemplateRequest
} from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminNotificationTemplateService {
  private readonly api = inject(ApiService);
  private readonly basePath = '/notification-templates';

  listTemplates(filter: NotificationTemplateFilter = {}): Observable<ApiResponse<NotificationTemplate[]>> {
    return this.api.get<NotificationTemplate[]>(this.basePath, {
      channel: filter.channel || undefined,
      event_type: filter.eventType || undefined,
      language: filter.language || undefined
    });
  }

  updateTemplate(
    code: string,
    payload: UpdateNotificationTemplateRequest,
    idempotencyKey = crypto.randomUUID()
  ): Observable<ApiResponse<NotificationTemplate>> {
    return this.api.put<NotificationTemplate>(
      `${this.basePath}/${encodeURIComponent(code)}`,
      payload,
      idempotencyKey
    );
  }

  previewTemplate(
    code: string,
    sampleData: Record<string, unknown>,
    idempotencyKey = crypto.randomUUID()
  ): Observable<ApiResponse<NotificationTemplatePreview>> {
    return this.api.post<NotificationTemplatePreview>(
      `${this.basePath}/${encodeURIComponent(code)}/preview`,
      sampleData,
      idempotencyKey
    );
  }
}
