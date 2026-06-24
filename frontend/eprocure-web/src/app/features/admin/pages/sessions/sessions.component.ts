import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of } from 'rxjs';

import { AdminOperationsService } from '../../services/admin-operations.service';
import { ToastService } from '../../../../core/services/toast.service';
import { PageMeta } from '../../../../core/models/api-response.model';
import { ActiveSession } from '../../models/admin-operations.model';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';

@Component({
  selector: 'ep-admin-sessions',
  standalone: true,
  imports: [
    TranslatePipe,
    DatePipe,
    LowerCasePipe,
    EpBreadcrumbComponent,
    EpStatCardComponent,
    EpFilterBarComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpBadgeComponent,
    EpModalComponent,
    EpFormFieldComponent,
    EpButtonComponent,
    EpIconComponent,
  ],
  templateUrl: './sessions.component.html',
  styleUrl: './sessions.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SessionsComponent implements OnInit {
  private readonly opsService = inject(AdminOperationsService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // State
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  readonly sessions = signal<ActiveSession[]>([]);
  readonly meta = signal<PageMeta | null>(null);

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);

  // Filter signals
  readonly userIdFilter = signal('');
  readonly invalidateReason = signal('');

  // Computed stats
  readonly recentSessionCount = computed(() =>
    this.sessions().filter(s =>
      Date.now() - new Date(s.lastActivity).getTime() < 1_800_000
    ).length
  );

  readonly expiringSoonCount = computed(() =>
    this.sessions().filter(s => {
      const diff = new Date(s.expiresAt).getTime() - Date.now();
      return diff > 0 && diff < 3_600_000;
    }).length
  );

  // Modal State
  readonly activeModal = signal<'invalidate' | null>(null);
  readonly selectedSession = signal<ActiveSession | null>(null);
  readonly modalSubmitting = signal(false);

  ngOnInit() {
    this.loadSessions();
  }

  loadSessions(pageIndex = 0) {
    this.isLoading.set(true);
    this.error.set(null);
    this.currentPage.set(pageIndex);

    const filter = {
      user_id: this.userIdFilter() || undefined,
      page: pageIndex,
      size: this.pageSize()
    };

    this.opsService.listActiveSessions(filter)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
        catchError(err => {
          this.error.set(err.error?.message || 'Failed to load active sessions');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.sessions.set(res.data);
          this.meta.set(res.meta);
        }
      });
  }

  onPageChange(newPageIndex: number) {
    this.loadSessions(newPageIndex);
  }

  resetFilters() {
    this.userIdFilter.set('');
    this.loadSessions(0);
  }

  // --- Invalidate Session Logic ---
  openInvalidateModal(session: ActiveSession) {
    this.selectedSession.set(session);
    this.invalidateReason.set('');
    this.activeModal.set('invalidate');
  }

  closeModal() {
    this.activeModal.set(null);
    this.selectedSession.set(null);
    this.invalidateReason.set('');
  }

  submitInvalidate() {
    const reason = this.invalidateReason().trim();
    if (!reason || !this.selectedSession()) {
      return;
    }

    this.modalSubmitting.set(true);
    const idempotencyKey = crypto.randomUUID();

    this.opsService.invalidateSession(this.selectedSession()!.sessionId, reason, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.modalSubmitting.set(false)),
        catchError(err => {
          this.toast.error(err.error?.message || 'adminOps.sessions.toast.invalidateFailed');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.sessions.toast.invalidateSuccess');
          this.closeModal();
          this.loadSessions(this.currentPage());
        }
      });
  }

  /**
   * Extracts a short browser/device label from a User-Agent string.
   */
  shortAgent(ua: string | null): string {
    if (!ua) return 'Unknown';
    if (/Edg\//i.test(ua)) return 'Edge';
    if (/OPR\//i.test(ua) || /Opera/i.test(ua)) return 'Opera';
    if (/Chrome\//i.test(ua)) return 'Chrome';
    if (/Firefox\//i.test(ua)) return 'Firefox';
    if (/Safari\//i.test(ua)) return 'Safari';
    if (/MSIE|Trident/i.test(ua)) return 'IE';
    if (/Mobile/i.test(ua)) return 'Mobile';
    return 'Other';
  }
}
