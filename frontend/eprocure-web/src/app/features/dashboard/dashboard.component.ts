import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/services/toast.service';
import { EpAmountComponent } from '../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../shared/components/ep-skeleton/ep-skeleton.component';
import {
  ChartDataPoint,
  CycleTimeKpi,
  DepartmentSpend,
  ExecutiveDashboard,
  KpiCard,
  KpiStatus,
  ManagerDashboard,
  MonthlyTrend,
  PurchasingDashboard,
  ReportFormat,
  ReportJob,
  ReportJobStatus,
  ReportType,
  RequesterDashboard,
  SlaComplianceKpi,
  TopVendor,
  analyticsMoney
} from './analytics.model';
import { AnalyticsService } from './analytics.service';

type DashboardTab = 'executive' | 'manager' | 'purchasing' | 'requester' | 'reports';

const KPI_TONE: Record<KpiStatus, EpBadgeTone> = {
  GOOD: 'success',
  WARNING: 'warning',
  CRITICAL: 'danger'
};

const JOB_TONE: Record<ReportJobStatus, EpBadgeTone> = {
  QUEUED: 'warning',
  PROCESSING: 'info',
  COMPLETED: 'success',
  FAILED: 'danger'
};

@Component({
  selector: 'ep-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpSkeletonComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly permissionService = inject(PermissionService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly executiveDashboard = signal<ExecutiveDashboard | null>(null);
  readonly managerDashboard = signal<ManagerDashboard | null>(null);
  readonly purchasingDashboard = signal<PurchasingDashboard | null>(null);
  readonly requesterDashboard = signal<RequesterDashboard | null>(null);
  readonly cycleTimeKpi = signal<CycleTimeKpi | null>(null);
  readonly slaComplianceKpi = signal<SlaComplianceKpi | null>(null);
  readonly reportJobs = signal<ReportJob[]>([]);
  readonly activeTab = signal<DashboardTab>('executive');
  readonly isLoadingDashboard = signal(false);
  readonly isLoadingKpi = signal(false);
  readonly isExporting = signal(false);
  readonly refreshingJobId = signal<string | null>(null);
  readonly downloadingJobId = signal<string | null>(null);

  readonly kpiTone = KPI_TONE;
  readonly jobTone = JOB_TONE;
  readonly currentYear = new Date().getFullYear();
  readonly tabs = computed(() => [
    { id: 'executive' as const, labelKey: 'dashboard.tabs.executive', permissions: ['REPORT_VIEW'] },
    { id: 'manager' as const, labelKey: 'dashboard.tabs.manager', permissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
    { id: 'purchasing' as const, labelKey: 'dashboard.tabs.purchasing', permissions: ['PO_VIEW_ALL'] },
    { id: 'requester' as const, labelKey: 'dashboard.tabs.requester', permissions: ['PR_VIEW_OWN'] },
    { id: 'reports' as const, labelKey: 'dashboard.tabs.reports', permissions: ['REPORT_EXPORT'] }
  ].filter((tab) => this.permissionService.hasAnyPermission(tab.permissions)));
  readonly canViewReports = computed(() => this.permissionService.hasPermission('REPORT_EXPORT'));
  readonly canViewExecutive = computed(() => this.permissionService.hasPermission('REPORT_VIEW'));
  readonly visibleKpis = computed(() => this.executiveDashboard()?.kpis ?? []);
  readonly maxDepartmentSpend = computed(() => this.maxOf(this.executiveDashboard()?.spendByDepartment ?? [], 'spent'));
  readonly maxCategorySpend = computed(() => this.maxPoint(this.executiveDashboard()?.spendByCategory ?? []));
  readonly maxMonthlySpend = computed(() => this.maxMonthly(this.executiveDashboard()?.monthlyTrend ?? []));
  readonly maxPriorityAvgValue = computed(() => this.maxPriorityAvgHours(this.cycleTimeKpi()?.byPriority ?? []));
  readonly totalPurchasingPipeline = computed(() => {
    const pipeline = this.purchasingDashboard()?.poPipeline;
    return pipeline ? pipeline.draft + pipeline.pendingApproval + pipeline.sentToVendor + pipeline.partiallyReceived : 0;
  });

  readonly filterForm = new FormGroup({
    fiscalYear: new FormControl(this.currentYear, { nonNullable: true, validators: [Validators.required] }),
    quarter: new FormControl<number | null>(null),
    fromDate: new FormControl(this.startOfYear(), { nonNullable: true, validators: [Validators.required] }),
    toDate: new FormControl(this.today(), { nonNullable: true, validators: [Validators.required] }),
    departmentId: new FormControl('', { nonNullable: true })
  });

  readonly exportForm = new FormGroup({
    reportType: new FormControl<ReportType>('SPENDING_BY_DEPARTMENT', { nonNullable: true, validators: [Validators.required] }),
    format: new FormControl<ReportFormat>('PDF', { nonNullable: true, validators: [Validators.required] }),
    fromDate: new FormControl(this.startOfYear(), { nonNullable: true }),
    toDate: new FormControl(this.today(), { nonNullable: true }),
    fiscalYear: new FormControl(this.currentYear, { nonNullable: true }),
    quarter: new FormControl<number | null>(null),
    vendorId: new FormControl('', { nonNullable: true }),
    categoryCode: new FormControl('', { nonNullable: true })
  });

  readonly reportTypes: ReportType[] = [
    'SPENDING_BY_DEPARTMENT',
    'BUDGET_VS_PLAN',
    'VENDOR_SCORECARD',
    'CYCLE_TIME_ANALYSIS',
    'SLA_COMPLIANCE',
    'THREE_WAY_MATCH',
    'INVENTORY_PENDING',
    'RFQ_SAVINGS',
    'MAVERICK_SPENDING',
    'AUDIT_TRAIL',
    'PR_SUMMARY',
    'PO_SUMMARY'
  ];
  readonly reportFormats: ReportFormat[] = ['PDF', 'EXCEL'];
  readonly quarters = [1, 2, 3, 4];

  ngOnInit(): void {
    const firstTab = this.tabs()[0]?.id ?? 'requester';
    this.activeTab.set(firstTab);
    this.loadDashboards();
    this.loadKpis();
  }

  setTab(tab: DashboardTab): void {
    this.activeTab.set(tab);
  }

  reload(): void {
    this.loadDashboards();
    this.loadKpis();
  }

  applyFilters(): void {
    this.filterForm.markAllAsTouched();
    if (this.filterForm.invalid) {
      return;
    }
    this.loadDashboards();
    this.loadKpis();
  }

  exportReport(): void {
    this.exportForm.markAllAsTouched();
    if (this.exportForm.invalid || this.isExporting()) {
      return;
    }
    const raw = this.exportForm.getRawValue();
    this.isExporting.set(true);
    this.analyticsService.exportReport({
      reportType: raw.reportType,
      format: raw.format,
      filters: {
        fromDate: raw.fromDate || null,
        toDate: raw.toDate || null,
        fiscalYear: raw.fiscalYear || null,
        quarter: raw.quarter || null,
        vendorId: raw.vendorId.trim() || null,
        categoryCode: raw.categoryCode.trim() || null
      }
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isExporting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.upsertJob(res.data);
          this.toastService.successKey('dashboard.reports.toast.created');
        }
      });
  }

  refreshJob(job: ReportJob): void {
    this.refreshingJobId.set(job.jobId);
    this.analyticsService.getReportJob(job.jobId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.refreshingJobId.set(null))
      )
      .subscribe({
        next: (res) => this.upsertJob(res.data)
      });
  }

  downloadJob(job: ReportJob): void {
    if (job.status !== 'COMPLETED') {
      return;
    }
    this.downloadingJobId.set(job.jobId);
    this.analyticsService.downloadReport(job.jobId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.downloadingJobId.set(null))
      )
      .subscribe({
        next: (blob) => this.saveBlob(blob, this.reportFileName(job)),
        error: () => this.toastService.errorKey('dashboard.reports.toast.downloadFailed')
      });
  }

  kpiValue(card: KpiCard): string {
    return card.unit ? `${card.value} ${card.unit}` : card.value;
  }

  statusTone(status: KpiStatus | null | undefined): EpBadgeTone {
    return status ? KPI_TONE[status] : 'neutral';
  }

  jobStatusTone(status: ReportJobStatus): EpBadgeTone {
    return JOB_TONE[status] ?? 'neutral';
  }

  money(value: string | number | null | undefined) {
    return analyticsMoney(value);
  }

  barWidth(value: string | number | null | undefined, max: number): string {
    if (max <= 0) {
      return '0%';
    }
    const percent = Math.max(0, Math.min(100, (Number(value ?? 0) / max) * 100));
    return `${percent}%`;
  }

  utilizationWidth(value: string | number | null | undefined): string {
    const percent = Math.max(0, Math.min(100, Number(value ?? 0)));
    return `${percent}%`;
  }

  percent(value: string | number | null | undefined): string {
    return `${Number(value ?? 0).toFixed(1)}%`;
  }

  hours(value: string | number | null | undefined): string {
    return `${Number(value ?? 0).toFixed(1)}h`;
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }

  formatDateTime(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(iso));
  }

  private loadDashboards(): void {
    const raw = this.filterForm.getRawValue();
    let pending = 0;
    const start = () => {
      pending += 1;
      this.isLoadingDashboard.set(true);
    };
    const done = () => {
      pending -= 1;
      if (pending <= 0) {
        this.isLoadingDashboard.set(false);
      }
    };

    if (this.canViewExecutive()) {
      start();
      this.analyticsService.getExecutiveDashboard({
        fiscal_year: raw.fiscalYear,
        quarter: raw.quarter ?? undefined
      })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(done)
        )
        .subscribe({
          next: (res) => this.executiveDashboard.set(res.data)
        });
    } else {
      this.executiveDashboard.set(null);
    }

    if (this.permissionService.hasAnyPermission(['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'])) {
      start();
      this.analyticsService.getManagerDashboard()
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(done)
        )
        .subscribe({
          next: (res) => this.managerDashboard.set(res.data)
        });
    } else {
      this.managerDashboard.set(null);
    }

    if (this.permissionService.hasPermission('PO_VIEW_ALL')) {
      start();
      this.analyticsService.getPurchasingDashboard()
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(done)
        )
        .subscribe({
          next: (res) => this.purchasingDashboard.set(res.data)
        });
    } else {
      this.purchasingDashboard.set(null);
    }

    if (this.permissionService.hasPermission('PR_VIEW_OWN')) {
      start();
      this.analyticsService.getRequesterDashboard()
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(done)
        )
        .subscribe({
          next: (res) => this.requesterDashboard.set(res.data)
        });
    } else {
      this.requesterDashboard.set(null);
    }

    if (pending === 0) {
      this.isLoadingDashboard.set(false);
      return;
    }
  }

  private loadKpis(): void {
    if (!this.canViewExecutive()) {
      return;
    }
    const raw = this.filterForm.getRawValue();
    this.isLoadingKpi.set(true);
    forkJoin({
      cycle: this.analyticsService.getCycleTimeKpi({
        from_date: raw.fromDate,
        to_date: raw.toDate,
        department_id: raw.departmentId.trim() || undefined
      }),
      sla: this.analyticsService.getSlaComplianceKpi({
        from_date: raw.fromDate,
        to_date: raw.toDate
      })
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingKpi.set(false))
      )
      .subscribe({
        next: ({ cycle, sla }) => {
          this.cycleTimeKpi.set(cycle.data);
          this.slaComplianceKpi.set(sla.data);
        }
      });
  }

  private upsertJob(job: ReportJob): void {
    this.reportJobs.update((jobs) => [
      job,
      ...jobs.filter((item) => item.jobId !== job.jobId)
    ].slice(0, 8));
  }

  private saveBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private reportFileName(job: ReportJob): string {
    const extension = job.format === 'PDF' ? 'pdf' : 'xlsx';
    return `${job.reportType.toLowerCase()}-${job.jobId.slice(0, 8)}.${extension}`;
  }

  private maxOf(items: DepartmentSpend[], key: 'spent' | 'budget'): number {
    return Math.max(0, ...items.map((item) => Number(item[key] ?? 0)));
  }

  private maxPoint(items: ChartDataPoint[]): number {
    return Math.max(0, ...items.map((item) => Number(item.value ?? 0)));
  }

  private maxMonthly(items: MonthlyTrend[]): number {
    return Math.max(0, ...items.flatMap((item) => [Number(item.spent ?? 0), Number(item.budget ?? 0)]));
  }

  private maxPriorityAvgHours(items: { avgHours: string | number }[]): number {
    return Math.max(0, ...items.map((item) => Number(item.avgHours ?? 0)));
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private startOfYear(): string {
    return `${this.currentYear}-01-01`;
  }
}
