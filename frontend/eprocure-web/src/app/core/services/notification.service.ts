import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse, PageMeta } from '../models/api-response.model';
import { ApiService } from '../http/api.service';

export interface NotificationItem {
  id: string;
  eventType: string;
  channel: string;
  subject: string | null;
  body: string;
  isRead: boolean;
  readAt: string | null;
  referenceType: string | null;
  referenceId: string | null;
  referenceNumber: string | null;
  actionUrl: string | null;
  createdAt: string;
}

export interface UnreadCountResponse {
  unread: number;
}

export interface MarkAllReadResponse {
  markedCount: number;
}

export type NotificationListResponse = ApiResponse<NotificationItem[]> & { meta: PageMeta };

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly api = inject(ApiService);

  listLatest(size = 5): Observable<NotificationListResponse> {
    return this.api.get<NotificationItem[]>('/notifications', {
      page: 1,
      size,
      sort: 'createdAt,desc'
    }) as Observable<NotificationListResponse>;
  }

  countUnread(): Observable<ApiResponse<UnreadCountResponse>> {
    return this.api.get<UnreadCountResponse>('/notifications/count');
  }

  markRead(id: string): Observable<ApiResponse<NotificationItem>> {
    return this.api.patch<NotificationItem>(`/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<ApiResponse<MarkAllReadResponse>> {
    return this.api.patch<MarkAllReadResponse>('/notifications/read-all', {});
  }
}
