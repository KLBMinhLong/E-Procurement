import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { finalize, forkJoin, interval } from 'rxjs';

import { AdminDepartment } from '../admin/models/admin.model';
import { AdminOrgService } from '../admin/services/admin-org.service';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/services/toast.service';
import { EpAmountComponent } from '../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../shared/components/ep-badge/ep-badge.component';
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
import { DashboardControlBarComponent } from './dashboard-control-bar.component';
import { DashboardDepartmentOption, DashboardTab, DashboardTabItem } from './dashboard-ui.model';

interface DashboardChartTheme {
  accent: string;
  accentSubtle: string;
  info: string;
  infoSubtle: string;
  success: string;
  warning: string;
  danger: string;
  neutralSubtle: string;
  surface: string;
  border: string;
  textMuted: string;
  textPrimary: string;
}

interface DashboardQuickAction {
  icon: string;
  labelKey: string;
  descriptionKey: string;
  permissions: string[];
  route?: string;
  reportType?: ReportType;
}

const DASHBOARD_QUICK_ACTIONS: DashboardQuickAction[] = [
  {
    icon: 'shopping-cart',
    labelKey: 'dashboard.quick.createPr',
    descriptionKey: 'dashboard.quick.createPrHint',
    permissions: ['PR_CREATE'],
    route: '/procurement/create'
  },
  {
    icon: 'inbox',
    labelKey: 'dashboard.quick.approvalInbox',
    descriptionKey: 'dashboard.quick.approvalInboxHint',
    permissions: ['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'PR_APPROVE_FINANCE', 'PR_APPROVE_EMERGENCY'],
    route: '/approvals'
  },
  {
    icon: 'receipt-text',
    labelKey: 'dashboard.quick.createPo',
    descriptionKey: 'dashboard.quick.createPoHint',
    permissions: ['PO_CREATE'],
    route: '/finance/purchase-orders/create'
  },
  {
    icon: 'wallet-cards',
    labelKey: 'dashboard.quick.viewBudgets',
    descriptionKey: 'dashboard.quick.viewBudgetsHint',
    permissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'],
    route: '/finance/budgets'
  },
  {
    icon: 'send',
    labelKey: 'dashboard.quick.viewRfqs',
    descriptionKey: 'dashboard.quick.viewRfqsHint',
    permissions: ['RFQ_VIEW'],
    route: '/vendors/rfq'
  },
  {
    icon: 'package-check',
    labelKey: 'dashboard.quick.createGr',
    descriptionKey: 'dashboard.quick.createGrHint',
    permissions: ['GR_CREATE'],
    route: '/inventory/goods-receipts/create'
  },
  {
    icon: 'file-check-2',
    labelKey: 'dashboard.quick.createInvoice',
    descriptionKey: 'dashboard.quick.createInvoiceHint',
    permissions: ['INVOICE_CREATE'],
    route: '/finance/invoices/create'
  },
  {
    icon: 'file-down',
    labelKey: 'dashboard.quick.reports',
    descriptionKey: 'dashboard.quick.reportsHint',
    permissions: ['REPORT_EXPORT'],
    reportType: 'SPENDING_BY_DEPARTMENT'
  }
];

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

const DEFAULT_CHART_THEME: DashboardChartTheme = {
  accent: '#f59e0b',
  accentSubtle: 'rgba(245, 158, 11, 0.12)',
  info: '#38bdf8',
  infoSubtle: 'rgba(56, 189, 248, 0.12)',
  success: '#22c55e',
  warning: '#f59e0b',
  danger: '#ef4444',
  neutralSubtle: 'rgba(148, 163, 184, 0.12)',
  surface: '#0f172a',
  border: 'rgba(148, 163, 184, 0.18)',
  textMuted: '#94a3b8',
  textPrimary: '#f8fafc'
};

const AUTO_REFRESH_MS = 5 * 60 * 1000;
const FRESHNESS_TICK_MS = 60 * 1000;
const DATA_STALE_MS = 5 * 60 * 1000;
const REPORT_JOB_POLL_MS = 5000;
const REPORT_JOBS_STORAGE_KEY = 'eprocure.dashboard.reportJobs';

@Component({
  selector: 'ep-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpSkeletonComponent,
    DashboardControlBarComponent,
    RouterLink,
    BaseChartDirective
  ],
  providers: [provideCharts(withDefaultRegisterables())],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly analyticsService = inject(AnalyticsService);
  private readonly adminOrgService = inject(AdminOrgService);
  private readonly permissionService = inject(PermissionService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);
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
  readonly isPollingReportJobs = signal(false);
  readonly refreshingJobId = signal<string | null>(null);
  readonly downloadingJobId = signal<string | null>(null);
  readonly chartTheme = signal<DashboardChartTheme>(DEFAULT_CHART_THEME);
  readonly departmentOptions = signal<DashboardDepartmentOption[]>([]);
  readonly isLoadingDepartments = signal(false);
  readonly departmentsLoadFailed = signal(false);
  readonly lastDashboardUpdatedAt = signal<string | null>(null);
  readonly freshnessClock = signal(Date.now());
  private readonly chartTextVersion = signal(0);

  readonly kpiTone = KPI_TONE;
  readonly jobTone = JOB_TONE;
  readonly currentYear = new Date().getFullYear();
  readonly tabs = computed<DashboardTabItem[]>(() => [
    { id: 'executive' as const, labelKey: 'dashboard.tabs.executive', permissions: ['REPORT_VIEW'] },
    { id: 'manager' as const, labelKey: 'dashboard.tabs.manager', permissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
    { id: 'purchasing' as const, labelKey: 'dashboard.tabs.purchasing', permissions: ['PO_VIEW_ALL'] },
    { id: 'requester' as const, labelKey: 'dashboard.tabs.requester', permissions: ['PR_VIEW_OWN'] },
    { id: 'reports' as const, labelKey: 'dashboard.tabs.reports', permissions: ['REPORT_EXPORT'] }
  ].filter((tab) => this.permissionService.hasAnyPermission(tab.permissions)));
  readonly quickActions = computed(() => DASHBOARD_QUICK_ACTIONS.filter((action) => this.permissionService.hasAnyPermission(action.permissions)));
  readonly isRefreshing = computed(() => this.isLoadingDashboard() || this.isLoadingKpi());
  readonly activeReportJobCount = computed(() => this.reportJobs().filter((job) => this.isReportJobInProgress(job.status)).length);
  readonly canViewReports = computed(() => this.permissionService.hasPermission('REPORT_EXPORT'));
  readonly canViewExecutive = computed(() => this.permissionService.hasPermission('REPORT_VIEW'));
  readonly lastUpdatedLabel = computed(() => {
    const value = this.lastDashboardUpdatedAt();
    return value ? this.formatDateTime(value) : null;
  });
  readonly cachedAtLabel = computed(() => {
    const cachedAt = this.executiveDashboard()?.cachedAt;
    return cachedAt ? this.formatDateTime(cachedAt) : null;
  });
  readonly dataStale = computed(() => {
    this.freshnessClock();
    const source = this.executiveDashboard()?.cachedAt ?? this.lastDashboardUpdatedAt();
    if (!source) {
      return false;
    }
    return Date.now() - new Date(source).getTime() > DATA_STALE_MS;
  });
  readonly visibleKpis = computed(() => this.executiveDashboard()?.kpis ?? []);
  readonly maxDepartmentSpend = computed(() => this.maxOf(this.executiveDashboard()?.spendByDepartment ?? [], 'spent'));
  readonly maxCategorySpend = computed(() => this.maxPoint(this.executiveDashboard()?.spendByCategory ?? []));
  readonly maxMonthlySpend = computed(() => this.maxMonthly(this.executiveDashboard()?.monthlyTrend ?? []));
  readonly maxPriorityAvgValue = computed(() => this.maxPriorityAvgHours(this.cycleTimeKpi()?.byPriority ?? []));
  readonly totalPurchasingPipeline = computed(() => {
    const pipeline = this.purchasingDashboard()?.poPipeline;
    return pipeline ? pipeline.draft + pipeline.pendingApproval + pipeline.sentToVendor + pipeline.partiallyReceived : 0;
  });
  readonly monthlyTrendChartData = computed<ChartData<'bar', number[], string>>(() => {
    const theme = this.chartTheme();
    const trend = this.executiveDashboard()?.monthlyTrend ?? [];
    return {
      labels: trend.map((month) => month.month),
      datasets: [
        {
          label: this.translate('dashboard.chart.spent'),
          data: trend.map((month) => this.numeric(month.spent)),
          backgroundColor: theme.accent,
          borderColor: theme.accent,
          borderRadius: 6,
          maxBarThickness: 28
        },
        {
          label: this.translate('dashboard.chart.budget'),
          data: trend.map((month) => this.numeric(month.budget)),
          backgroundColor: theme.infoSubtle,
          borderColor: theme.info,
          borderRadius: 6,
          borderWidth: 1,
          maxBarThickness: 28
        }
      ]
    };
  });
  readonly categorySpendChartData = computed<ChartData<'doughnut', number[], string>>(() => {
    const theme = this.chartTheme();
    const palette = this.chartPalette(theme);
    const categories = this.executiveDashboard()?.spendByCategory ?? [];
    return {
      labels: categories.map((category) => category.label),
      datasets: [
        {
          data: categories.map((category) => this.numeric(category.value)),
          backgroundColor: categories.map((_, index) => palette[index % palette.length]),
          borderColor: theme.surface,
          borderWidth: 2,
          hoverOffset: 4
        }
      ]
    };
  });
  readonly approvalSlaGaugeChartData = computed<ChartData<'doughnut', number[], string>>(() => {
    const theme = this.chartTheme();
    const sla = this.executiveDashboard()?.approvalSla;
    const onTime = this.clampPercent(this.numeric(sla?.onTimePercent));
    return {
      labels: [
        this.translate('dashboard.executive.onTime'),
        this.translate('dashboard.chart.late')
      ],
      datasets: [
        {
          data: [onTime, 100 - onTime],
          backgroundColor: [
            this.slaColor(onTime, theme),
            theme.neutralSubtle
          ],
          borderColor: theme.surface,
          borderWidth: 2
        }
      ]
    };
  });
  readonly cycleTimeTrendChartData = computed<ChartData<'line', number[], string>>(() => {
    const theme = this.chartTheme();
    const trend = this.cycleTimeKpi()?.trend ?? [];
    return {
      labels: trend.map((point) => point.week),
      datasets: [
        {
          label: this.translate('dashboard.chart.avgHours'),
          data: trend.map((point) => this.numeric(point.avgHours)),
          borderColor: theme.accent,
          backgroundColor: theme.accentSubtle,
          fill: true,
          pointBackgroundColor: theme.accent,
          pointBorderColor: theme.surface,
          tension: 0.35
        }
      ]
    };
  });
  readonly priorityCycleTimeChartData = computed<ChartData<'bar', number[], string>>(() => {
    const theme = this.chartTheme();
    const priorities = this.cycleTimeKpi()?.byPriority ?? [];
    return {
      labels: priorities.map((priority) => priority.priority),
      datasets: [
        {
          label: this.translate('dashboard.chart.avgHours'),
          data: priorities.map((priority) => this.numeric(priority.avgHours)),
          backgroundColor: theme.info,
          borderColor: theme.info,
          borderRadius: 6,
          maxBarThickness: 28
        }
      ]
    };
  });
  readonly slaRoleComplianceChartData = computed<ChartData<'bar', number[], string>>(() => {
    const theme = this.chartTheme();
    const roles = this.slaComplianceKpi()?.byApproverRole ?? [];
    return {
      labels: roles.map((role) => role.role),
      datasets: [
        {
          label: this.translate('dashboard.kpi.compliance'),
          data: roles.map((role) => this.numeric(role.compliancePct)),
          backgroundColor: theme.success,
          borderColor: theme.success,
          borderRadius: 6,
          maxBarThickness: 28
        }
      ]
    };
  });
  readonly requesterStatusChartData = computed<ChartData<'doughnut', number[], string>>(() => {
    const theme = this.chartTheme();
    const stats = this.requesterDashboard()?.myPrStats;
    return {
      labels: [
        this.translate('pr.status.DRAFT'),
        this.translate('pr.status.PENDING_APPROVAL'),
        this.translate('pr.status.CHANGES_REQUESTED'),
        this.translate('pr.status.APPROVED'),
        this.translate('pr.status.REJECTED')
      ],
      datasets: [
        {
          data: stats
            ? [stats.draft, stats.pendingApproval, stats.changesRequested, stats.approved, stats.rejected]
            : [],
          backgroundColor: [
            theme.textMuted,
            theme.warning,
            theme.info,
            theme.success,
            theme.danger
          ],
          borderColor: theme.surface,
          borderWidth: 2
        }
      ]
    };
  });
  readonly monthlyTrendChartOptions = computed<ChartOptions<'bar'>>(() => this.currencyBarOptions(this.chartTheme()));
  readonly categorySpendChartOptions = computed<ChartOptions<'doughnut'>>(() => this.doughnutOptions(this.chartTheme()));
  readonly approvalSlaGaugeChartOptions = computed<ChartOptions<'doughnut'>>(() => this.gaugeOptions(this.chartTheme()));
  readonly cycleTimeTrendChartOptions = computed<ChartOptions<'line'>>(() => this.hoursLineOptions(this.chartTheme()));
  readonly priorityCycleTimeChartOptions = computed<ChartOptions<'bar'>>(() => this.hoursBarOptions(this.chartTheme()));
  readonly slaRoleComplianceChartOptions = computed<ChartOptions<'bar'>>(() => this.percentBarOptions(this.chartTheme()));
  readonly requesterStatusChartOptions = computed<ChartOptions<'doughnut'>>(() => this.doughnutOptions(this.chartTheme()));

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
    this.resolveChartTheme();
    this.translateService.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.chartTextVersion.update((version) => version + 1));

    this.initializeTabFromUrl();
    this.bindTabQueryParam();
    this.restoreReportJobs();
    this.loadDepartments();
    this.startFreshnessClock();
    this.startAutoRefresh();
    this.startReportJobPolling();
    this.reload();
  }

  setTab(tab: DashboardTab): void {
    if (this.activeTab() === tab) {
      return;
    }
    this.activeTab.set(tab);
    this.syncTabQueryParam(tab);
  }

  openReportPreset(reportType: ReportType): void {
    this.syncReportFormWithFilters();
    this.exportForm.controls.reportType.setValue(reportType);
    this.setTab('reports');
  }

  openReportsForCurrentTab(): void {
    this.openReportPreset(this.reportPresetForTab(this.activeTab()));
  }

  reload(): void {
    if (this.isRefreshing()) {
      return;
    }
    this.loadDashboards();
    this.loadKpis();
  }

  applyFilters(): void {
    this.filterForm.markAllAsTouched();
    if (this.filterForm.invalid || this.isRefreshing()) {
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
    if (this.isReportDownloadDisabled(job)) {
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

  isReportDownloadDisabled(job: ReportJob): boolean {
    return job.status !== 'COMPLETED';
  }

  reportExpiryLabel(job: ReportJob): string {
    return job.expiresAt ? this.formatDateTime(job.expiresAt) : this.notAvailableLabel();
  }

  kpiValue(card: KpiCard): string {
    return card.unit ? `${card.value} ${card.unit}` : card.value;
  }

  kpiIcon(card: KpiCard): string {
    const label = card.label.toLowerCase();
    if (label.includes('spend') || label.includes('budget') || label.includes('amount')) {
      return 'wallet-cards';
    }
    if (label.includes('pr') || label.includes('request')) {
      return 'shopping-cart';
    }
    if (label.includes('po') || label.includes('purchase order')) {
      return 'receipt-text';
    }
    if (label.includes('invoice') || label.includes('match')) {
      return 'file-check-2';
    }
    if (label.includes('sla') || label.includes('cycle') || label.includes('approval')) {
      return 'timer';
    }
    if (label.includes('vendor') || label.includes('supplier')) {
      return 'factory';
    }
    return 'chart-no-axes-combined';
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
    const percent = Math.max(0, Math.min(100, (this.numeric(value) / max) * 100));
    return `${percent}%`;
  }

  utilizationWidth(value: string | number | null | undefined): string {
    const percent = this.clampPercent(this.numeric(value));
    return `${percent}%`;
  }

  percent(value: string | number | null | undefined): string {
    return `${this.numeric(value).toFixed(1)}%`;
  }

  hours(value: string | number | null | undefined): string {
    return `${this.numeric(value).toFixed(1)}h`;
  }

  hasChartValues(values: Array<string | number | null | undefined>): boolean {
    return values.some((value) => this.numeric(value) > 0);
  }

  hasMonthlyTrend(dashboard: ExecutiveDashboard): boolean {
    return dashboard.monthlyTrend.length > 0
      && this.hasChartValues(dashboard.monthlyTrend.flatMap((month) => [month.spent, month.budget]));
  }

  hasCategorySpend(dashboard: ExecutiveDashboard): boolean {
    return dashboard.spendByCategory.length > 0
      && this.hasChartValues(dashboard.spendByCategory.map((point) => point.value));
  }

  hasCycleTrend(kpi: CycleTimeKpi): boolean {
    return kpi.trend.length > 0
      && this.hasChartValues(kpi.trend.map((point) => point.avgHours));
  }

  hasPriorityCycleTime(kpi: CycleTimeKpi): boolean {
    return kpi.byPriority.length > 0
      && this.hasChartValues(kpi.byPriority.map((point) => point.avgHours));
  }

  hasRoleCompliance(kpi: SlaComplianceKpi): boolean {
    return kpi.byApproverRole.length > 0
      && this.hasChartValues(kpi.byApproverRole.map((role) => role.compliancePct));
  }

  hasWorstApprovers(kpi: SlaComplianceKpi): boolean {
    return kpi.worstApprovers.length > 0;
  }

  hasRequesterStatusData(dashboard: RequesterDashboard): boolean {
    return this.totalRequesterRequests(dashboard) > 0;
  }

  totalRequesterRequests(dashboard: RequesterDashboard): number {
    const stats = dashboard.myPrStats;
    return stats.draft
      + stats.pendingApproval
      + stats.changesRequested
      + stats.approved
      + stats.rejected;
  }

  can(permission: string): boolean {
    return this.permissionService.hasPermission(permission);
  }

  canAny(permissions: string[]): boolean {
    return this.permissionService.hasAnyPermission(permissions);
  }

  budgetHealthTone(value: string | number | null | undefined): EpBadgeTone {
    const availablePct = this.numeric(value);
    if (availablePct >= 30) {
      return 'success';
    }
    if (availablePct >= 15) {
      return 'warning';
    }
    return 'danger';
  }

  budgetHealthLabelKey(value: string | number | null | undefined): string {
    const availablePct = this.numeric(value);
    if (availablePct >= 30) {
      return 'dashboard.budgetHealth.good';
    }
    if (availablePct >= 15) {
      return 'dashboard.budgetHealth.watch';
    }
    return 'dashboard.budgetHealth.risk';
  }

  isReportJobInProgress(status: ReportJobStatus): boolean {
    return status === 'QUEUED' || status === 'PROCESSING';
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return this.notAvailableLabel();
    }
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }

  formatDateTime(iso: string | null | undefined): string {
    if (!iso) {
      return this.notAvailableLabel();
    }
    return new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(new Date(iso));
  }

  notAvailableLabel(): string {
    return this.translate('dashboard.empty.notAvailable');
  }

  private initializeTabFromUrl(): void {
    const rawTab = this.route.snapshot.queryParamMap.get('tab');
    const tab = this.resolveVisibleTab(rawTab);
    this.activeTab.set(tab);
    if (rawTab !== tab) {
      this.syncTabQueryParam(tab, true);
    }
  }

  private bindTabQueryParam(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const rawTab = params.get('tab');
        const tab = this.resolveVisibleTab(rawTab);
        if (this.activeTab() !== tab) {
          this.activeTab.set(tab);
        }
        if (rawTab !== tab) {
          this.syncTabQueryParam(tab, true);
        }
      });
  }

  private syncTabQueryParam(tab: DashboardTab, replaceUrl = false): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl
    });
  }

  private resolveVisibleTab(value: string | null): DashboardTab {
    const visibleTabs = this.tabs();
    if (this.isDashboardTab(value) && visibleTabs.some((tab) => tab.id === value)) {
      return value;
    }
    return visibleTabs[0]?.id ?? 'requester';
  }

  private isDashboardTab(value: string | null): value is DashboardTab {
    return value === 'executive'
      || value === 'manager'
      || value === 'purchasing'
      || value === 'requester'
      || value === 'reports';
  }

  private loadDepartments(): void {
    this.isLoadingDepartments.set(true);
    this.departmentsLoadFailed.set(false);
    this.adminOrgService.getDepartments()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingDepartments.set(false))
      )
      .subscribe({
        next: (res) => {
          const options = this.flattenDepartmentOptions(res.data ?? []);
          const selectedDepartmentId = this.filterForm.controls.departmentId.value.trim();
          if (selectedDepartmentId && !options.some((option) => option.id === selectedDepartmentId)) {
            options.unshift({ id: selectedDepartmentId, label: selectedDepartmentId });
          }
          this.departmentOptions.set(options);
        },
        error: () => {
          this.departmentOptions.set([]);
          this.departmentsLoadFailed.set(true);
        }
      });
  }

  private flattenDepartmentOptions(departments: AdminDepartment[]): DashboardDepartmentOption[] {
    const seen = new Set<string>();
    const options: DashboardDepartmentOption[] = [];
    const visit = (items: AdminDepartment[], depth: number) => {
      items.forEach((department) => {
        if (seen.has(department.id)) {
          return;
        }
        seen.add(department.id);
        const prefix = depth > 0 ? `${'--'.repeat(depth)} ` : '';
        options.push({
          id: department.id,
          label: `${prefix}${department.name} (${department.code})`
        });
        if (department.children?.length) {
          visit(department.children, depth + 1);
        }
      });
    };
    visit(departments, 0);
    return options;
  }

  private startFreshnessClock(): void {
    interval(FRESHNESS_TICK_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.freshnessClock.set(Date.now()));
  }

  private startAutoRefresh(): void {
    interval(AUTO_REFRESH_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (typeof document !== 'undefined' && document.hidden) {
          return;
        }
        this.reload();
      });
  }

  private startReportJobPolling(): void {
    interval(REPORT_JOB_POLL_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.pollReportJobs());
  }

  private pollReportJobs(): void {
    if (this.isPollingReportJobs()) {
      return;
    }
    const activeJobs = this.reportJobs().filter((job) => this.isReportJobInProgress(job.status));
    if (activeJobs.length === 0) {
      return;
    }
    this.isPollingReportJobs.set(true);
    forkJoin(activeJobs.map((job) => this.analyticsService.getReportJob(job.jobId)))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isPollingReportJobs.set(false))
      )
      .subscribe({
        next: (responses) => responses.forEach((res) => this.upsertJob(res.data))
      });
  }

  private markDashboardLoaded(): void {
    const now = new Date().toISOString();
    this.lastDashboardUpdatedAt.set(now);
    this.freshnessClock.set(Date.now());
  }

  private resolveChartTheme(): void {
    if (typeof document === 'undefined') {
      return;
    }
    const style = getComputedStyle(document.documentElement);
    this.chartTheme.set({
      accent: this.cssVar(style, '--color-accent', DEFAULT_CHART_THEME.accent),
      accentSubtle: this.cssVar(style, '--color-accent-subtle', DEFAULT_CHART_THEME.accentSubtle),
      info: this.cssVar(style, '--color-info', DEFAULT_CHART_THEME.info),
      infoSubtle: this.cssVar(style, '--color-info-subtle', DEFAULT_CHART_THEME.infoSubtle),
      success: this.cssVar(style, '--color-success', DEFAULT_CHART_THEME.success),
      warning: this.cssVar(style, '--color-warning', DEFAULT_CHART_THEME.warning),
      danger: this.cssVar(style, '--color-danger', DEFAULT_CHART_THEME.danger),
      neutralSubtle: this.cssVar(style, '--color-neutral-subtle', DEFAULT_CHART_THEME.neutralSubtle),
      surface: this.cssVar(style, '--color-surface', DEFAULT_CHART_THEME.surface),
      border: this.cssVar(style, '--color-border-subtle', DEFAULT_CHART_THEME.border),
      textMuted: this.cssVar(style, '--color-text-muted', DEFAULT_CHART_THEME.textMuted),
      textPrimary: this.cssVar(style, '--color-text-primary', DEFAULT_CHART_THEME.textPrimary)
    });
  }

  private currencyBarOptions(theme: DashboardChartTheme): ChartOptions<'bar'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: theme.textMuted, boxHeight: 10, boxWidth: 10 }
        },
        tooltip: {
          callbacks: {
            label: (context: TooltipItem<'bar'>) =>
              `${context.dataset.label}: ${this.formatCompactNumber(this.tooltipNumber(context))}`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: theme.textMuted }
        },
        y: {
          beginAtZero: true,
          grid: { color: theme.border },
          ticks: {
            color: theme.textMuted,
            callback: (value) => this.formatCompactNumber(this.numeric(value))
          }
        }
      }
    };
  }

  private hoursBarOptions(theme: DashboardChartTheme): ChartOptions<'bar'> {
    return {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (context: TooltipItem<'bar'>) =>
              `${context.dataset.label}: ${this.hours(this.tooltipNumber(context))}`
          }
        }
      },
      scales: {
        x: {
          beginAtZero: true,
          grid: { color: theme.border },
          ticks: { color: theme.textMuted }
        },
        y: {
          grid: { display: false },
          ticks: { color: theme.textMuted }
        }
      }
    };
  }

  private percentBarOptions(theme: DashboardChartTheme): ChartOptions<'bar'> {
    return {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (context: TooltipItem<'bar'>) =>
              `${context.dataset.label}: ${this.percent(this.tooltipNumber(context))}`
          }
        }
      },
      scales: {
        x: {
          beginAtZero: true,
          max: 100,
          grid: { color: theme.border },
          ticks: {
            color: theme.textMuted,
            callback: (value) => this.percent(this.numeric(value))
          }
        },
        y: {
          grid: { display: false },
          ticks: { color: theme.textMuted }
        }
      }
    };
  }

  private hoursLineOptions(theme: DashboardChartTheme): ChartOptions<'line'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: theme.textMuted, boxHeight: 10, boxWidth: 10 }
        },
        tooltip: {
          callbacks: {
            label: (context: TooltipItem<'line'>) =>
              `${context.dataset.label}: ${this.hours(this.tooltipNumber(context))}`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: theme.textMuted }
        },
        y: {
          beginAtZero: true,
          grid: { color: theme.border },
          ticks: { color: theme.textMuted }
        }
      }
    };
  }

  private doughnutOptions(theme: DashboardChartTheme): ChartOptions<'doughnut'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '62%',
      plugins: {
        legend: {
          position: 'right',
          labels: { color: theme.textMuted, boxHeight: 10, boxWidth: 10 }
        },
        tooltip: {
          callbacks: {
            label: (context: TooltipItem<'doughnut'>) =>
              `${context.label}: ${this.formatCompactNumber(this.tooltipNumber(context))}`
          }
        }
      }
    };
  }

  private gaugeOptions(theme: DashboardChartTheme): ChartOptions<'doughnut'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '72%',
      rotation: -90,
      circumference: 180,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (context: TooltipItem<'doughnut'>) =>
              `${context.label}: ${this.percent(this.tooltipNumber(context))}`
          }
        }
      }
    };
  }

  private translate(key: string): string {
    this.chartTextVersion();
    return this.translateService.instant(key);
  }

  private chartPalette(theme: DashboardChartTheme): string[] {
    return [theme.accent, theme.info, theme.success, theme.warning, theme.danger, theme.textMuted];
  }

  private slaColor(onTimePercent: number, theme: DashboardChartTheme): string {
    if (onTimePercent >= 85) {
      return theme.success;
    }
    if (onTimePercent >= 70) {
      return theme.warning;
    }
    return theme.danger;
  }

  private clampPercent(value: number): number {
    return Math.max(0, Math.min(100, value));
  }

  private numeric(value: string | number | null | undefined): number {
    const normalized = String(value ?? '0').replace(/,/g, '');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private tooltipNumber(context: TooltipItem<'bar'> | TooltipItem<'line'> | TooltipItem<'doughnut'>): number {
    const parsed = context.parsed as number | { x?: number; y?: number };
    if (typeof parsed === 'number') {
      return parsed;
    }
    return parsed.y ?? parsed.x ?? 0;
  }

  private formatCompactNumber(value: string | number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN', {
      notation: 'compact',
      maximumFractionDigits: 1
    }).format(this.numeric(value));
  }

  private cssVar(style: CSSStyleDeclaration, name: string, fallback: string): string {
    return style.getPropertyValue(name).trim() || fallback;
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
        this.markDashboardLoaded();
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
      this.lastDashboardUpdatedAt.set(null);
      return;
    }
  }

  private loadKpis(): void {
    if (!this.canViewExecutive()) {
      this.cycleTimeKpi.set(null);
      this.slaComplianceKpi.set(null);
      this.isLoadingKpi.set(false);
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
    this.persistReportJobs();
  }

  private restoreReportJobs(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }
    try {
      const raw = sessionStorage.getItem(REPORT_JOBS_STORAGE_KEY);
      if (!raw) {
        return;
      }
      const jobs = JSON.parse(raw) as ReportJob[];
      if (Array.isArray(jobs)) {
        this.reportJobs.set(jobs.slice(0, 8));
      }
    } catch {
      sessionStorage.removeItem(REPORT_JOBS_STORAGE_KEY);
    }
  }

  private persistReportJobs(): void {
    if (typeof sessionStorage === 'undefined') {
      return;
    }
    sessionStorage.setItem(REPORT_JOBS_STORAGE_KEY, JSON.stringify(this.reportJobs()));
  }

  private syncReportFormWithFilters(): void {
    const raw = this.filterForm.getRawValue();
    this.exportForm.patchValue({
      fromDate: raw.fromDate,
      toDate: raw.toDate,
      fiscalYear: raw.fiscalYear,
      quarter: raw.quarter
    });
  }

  private reportPresetForTab(tab: DashboardTab): ReportType {
    if (tab === 'manager') {
      return 'BUDGET_VS_PLAN';
    }
    if (tab === 'purchasing') {
      return 'PO_SUMMARY';
    }
    if (tab === 'requester') {
      return 'PR_SUMMARY';
    }
    return 'SPENDING_BY_DEPARTMENT';
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
    return Math.max(0, ...items.map((item) => this.numeric(item[key])));
  }

  private maxPoint(items: ChartDataPoint[]): number {
    return Math.max(0, ...items.map((item) => this.numeric(item.value)));
  }

  private maxMonthly(items: MonthlyTrend[]): number {
    return Math.max(0, ...items.flatMap((item) => [this.numeric(item.spent), this.numeric(item.budget)]));
  }

  private maxPriorityAvgHours(items: { avgHours: string | number }[]): number {
    return Math.max(0, ...items.map((item) => this.numeric(item.avgHours)));
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private startOfYear(): string {
    return `${this.currentYear}-01-01`;
  }
}
