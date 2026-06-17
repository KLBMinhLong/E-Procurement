import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, interval, of, switchMap, takeWhile } from 'rxjs';

import { AdminOperationsService } from '../../services/admin-operations.service';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { ToastService } from '../../../../core/services/toast.service';
import { PageMeta } from '../../../../core/models/api-response.model';
import { AuditLogEntry, AuditLogFilter, AuditExportJob } from '../../models/admin-operations.model';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, TranslateModule, EpIconComponent, FormsModule, ReactiveFormsModule],
  templateUrl: './audit-log.component.html',
  styleUrl: './audit-log.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuditLogComponent implements OnInit {
  private readonly opsService = inject(AdminOperationsService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  // State
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  
  readonly logs = signal<AuditLogEntry[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  
  readonly selectedLog = signal<AuditLogEntry | null>(null);
  
  readonly exportJob = signal<AuditExportJob | null>(null);
  readonly exportPolling = signal(false);

  // Filters Form
  readonly filterForm = this.fb.group({
    from_time: ['', Validators.required],
    to_time: ['', Validators.required],
    actor_id: [''],
    entity_type: [''],
    entity_id: [''],
    action: [''],
    service_name: [''],
    is_success: [null as boolean | null]
  });

  readonly currentPage = signal(0);
  readonly pageSize = signal(20);

  ngOnInit() {
    // Set default dates: Last 7 days
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 7);

    this.filterForm.patchValue({
      from_time: from.toISOString().slice(0, 16), // YYYY-MM-DDTHH:mm
      to_time: to.toISOString().slice(0, 16)
    });

    this.loadLogs();
  }

  loadLogs(pageIndex = 0) {
    if (this.filterForm.invalid) {
      this.filterForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.currentPage.set(pageIndex);

    const fv = this.filterForm.getRawValue();
    const filter: AuditLogFilter = {
      from_time: new Date(fv.from_time!).toISOString(),
      to_time: new Date(fv.to_time!).toISOString(),
      page: pageIndex,
      size: this.pageSize()
    };

    if (fv.actor_id) filter.actor_id = fv.actor_id;
    if (fv.entity_type) filter.entity_type = fv.entity_type;
    if (fv.entity_id) filter.entity_id = fv.entity_id;
    if (fv.action) filter.action = fv.action;
    if (fv.service_name) filter.service_name = fv.service_name;
    if (fv.is_success !== null && fv.is_success !== undefined) {
      // Form might return string 'true'/'false' depending on select setup, convert it
      filter.is_success = String(fv.is_success) === 'true';
    }

    this.opsService.queryAuditLog(filter)
      .pipe(
        finalize(() => this.loading.set(false)),
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
  }

  closeDetail() {
    this.selectedLog.set(null);
  }

  // --- Export Logic ---
  exportLogs() {
    if (this.filterForm.invalid) {
      this.toast.error('adminOps.audit.validation.datesRequired');
      return;
    }

    const fv = this.filterForm.getRawValue();
    const request = {
      fromTime: new Date(fv.from_time!).toISOString(),
      toTime: new Date(fv.to_time!).toISOString(),
      actorId: fv.actor_id || null,
      entityType: fv.entity_type || null,
      action: fv.action || null
    };

    const idempotencyKey = crypto.randomUUID();

    this.opsService.exportAuditLog(request, idempotencyKey)
      .pipe(
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
        if (!res?.success) return true; // Keep polling if minor error? Or maybe stop. Let's stop if error.
        const status = res.data.status;
        return status !== 'COMPLETED' && status !== 'FAILED';
      }, true) // inclusive to emit the final state
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
      catchError(err => {
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

  resetFilters() {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 7);

    this.filterForm.reset({
      from_time: from.toISOString().slice(0, 16),
      to_time: to.toISOString().slice(0, 16)
    });
    this.loadLogs(0);
  }
}
