import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, interval, of, switchMap, takeWhile } from 'rxjs';

import { AdminOperationsService } from '../../services/admin-operations.service';
import { ToastService } from '../../../../core/services/toast.service';
import { PageMeta } from '../../../../core/models/api-response.model';
import { AuditLogEntry, AuditLogFilter, AuditExportJob } from '../../models/admin-operations.model';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';

// Default date range: last 7 days
const defaultFrom = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
const defaultTo = new Date().toISOString();

@Component({
  selector: 'ep-admin-audit-log',
  standalone: true,
  imports: [
    TranslatePipe,
    DatePipe,
    NgClass,
    EpBreadcrumbComponent,
    EpStatCardComponent,
    EpFilterBarComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpBadgeComponent,
    EpModalComponent,
    EpIconComponent,
    EpButtonComponent
  ],
  templateUrl: './audit-log.component.html',
  styleUrl: './audit-log.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuditLogComponent implements OnInit {
  private readonly opsService = inject(AdminOperationsService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // Filter signals (replace FormBuilder/filterForm)
  readonly fromTime = signal<string>(defaultFrom);
  readonly toTime = signal<string>(defaultTo);
  readonly searchQuery = signal('');
  readonly successFilter = signal<boolean | null>(null);
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);

  // State signals
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);

  readonly logs = signal<AuditLogEntry[]>([]);
  readonly meta = signal<PageMeta | null>(null);

  // Detail modal
  readonly selectedLog = signal<AuditLogEntry | null>(null);
  readonly activeModal = signal<'detail' | null>(null);

  // Export signals
  readonly exportJob = signal<AuditExportJob | null>(null);
  readonly exportPolling = signal(false);

  // Computed signals
  readonly successCount = computed(() =>
    this.logs().filter(log => log.isSuccess === true).length
  );

  readonly failureCount = computed(() =>
    this.logs().filter(log => log.isSuccess === false).length
  );

  readonly isExporting = computed(() => {
    const job = this.exportJob();
    if (!job) return false;
    return job.status !== 'COMPLETED' && job.status !== 'FAILED';
  });

  readonly exportJobStatus = computed(() => this.exportJob()?.status ?? null);

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs(pageIndex = 0) {
    this.isLoading.set(true);
    this.error.set(null);
    this.currentPage.set(pageIndex);

    const filter: AuditLogFilter = {
      from_time: this.fromTime(),
      to_time: this.toTime(),
      page: pageIndex,
      size: this.pageSize()
    };

    const query = this.searchQuery().trim();
    if (query) {
      filter.actor_id = query;
    }

    const success = this.successFilter();
    if (success !== null) {
      filter.is_success = success;
    }

    this.opsService.queryAuditLog(filter)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
        catchError(err => {
          this.error.set(err.error?.message || 'Failed to load audit logs');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.logs.set(res.data);
          this.meta.set(res.meta);
        }
      });
  }

  onPageChange(newPageIndex: number) {
    this.loadLogs(newPageIndex);
  }

  openDetail(log: AuditLogEntry) {
    this.selectedLog.set(log);
    this.activeModal.set('detail');
  }

  closeDetail() {
    this.selectedLog.set(null);
    this.activeModal.set(null);
  }

  resetFilters() {
    this.fromTime.set(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString());
    this.toTime.set(new Date().toISOString());
    this.searchQuery.set('');
    this.successFilter.set(null);
    this.loadLogs(0);
  }

  // --- Export Logic ---
  exportLogs() {
    const request = {
      fromTime: this.fromTime(),
      toTime: this.toTime(),
      actorId: null as string | null,
      entityType: null as string | null,
      action: null as string | null
    };

    const idempotencyKey = crypto.randomUUID();

    this.opsService.exportAuditLog(request, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(err => {
          this.toast.error(err.error?.message || 'Failed to start export job');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.audit.toast.exportStarted');
          this.exportJob.set(res.data);
          if (res.data.status !== 'COMPLETED' && res.data.status !== 'FAILED') {
            this.pollExportJob(res.data.jobId);
          }
        }
      });
  }

  private pollExportJob(jobId: string) {
    this.exportPolling.set(true);

    interval(5000).pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(() => this.opsService.getAuditExportJob(jobId).pipe(
        catchError(() => of(null))
      )),
      takeWhile(res => {
        if (!res?.success) return true;
        const status = res.data.status;
        return status !== 'COMPLETED' && status !== 'FAILED';
      }, true) // inclusive: emit the final terminal state
    ).subscribe(res => {
      if (!res?.success) {
        this.exportPolling.set(false);
        return;
      }

      this.exportJob.set(res.data);
      if (res.data.status === 'COMPLETED' || res.data.status === 'FAILED') {
        this.exportPolling.set(false);
        if (res.data.status === 'COMPLETED') {
          this.toast.success('adminOps.audit.toast.exportCompleted');
        } else {
          this.toast.error('adminOps.audit.toast.exportFailed');
        }
      }
    });
  }

  downloadExport() {
    const job = this.exportJob();
    if (!job || job.status !== 'COMPLETED') return;

    this.opsService.downloadAuditExport(job.jobId).pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => {
        this.toast.error('adminOps.audit.toast.downloadFailed');
        return of(null);
      })
    ).subscribe(blob => {
      if (blob) {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = job.fileName || `audit_export_${job.jobId}.xlsx`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }
    });
  }

  formatJson(obj: unknown): string {
    if (!obj) return '';
    try {
      return JSON.stringify(obj, null, 2);
    } catch {
      return String(obj);
    }
  }
}
