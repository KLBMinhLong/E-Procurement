import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { PageMeta } from '../../../../core/models/api-response.model';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import {
  BudgetDashboard,
  BudgetHealthTone,
  BudgetListFilter,
  BudgetStatus,
  budgetHealthTone,
  budgetMoney
} from '../../models/budget.model';
import { BudgetService } from '../../services/budget.service';

const STATUS_TONE: Record<BudgetStatus, EpBadgeTone> = {
  PLANNING: 'neutral',
  SUBMITTED: 'warning',
  APPROVED: 'info',
  ACTIVE: 'success',
  CLOSED: 'neutral'
};

const HEALTH_TONE: Record<BudgetHealthTone, EpBadgeTone> = {
  HEALTHY: 'success',
  WARNING: 'warning',
  EXCEEDED: 'danger'
};

type QuarterFilter = 1 | 2 | 3 | 4 | '';
type SortKey = 'fiscalYear' | 'allocated' | 'available' | 'status';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'ep-budget-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpSkeletonComponent
  ],
  templateUrl: './budget-list.component.html',
  styleUrl: './budget-list.component.scss'
})
export class BudgetListComponent implements OnInit {
  private readonly budgetService = inject(BudgetService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<BudgetDashboard[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly fiscalYear = signal<number | ''>(new Date().getFullYear());
  readonly quarter = signal<QuarterFilter>('');
  readonly departmentId = signal('');
  readonly glAccountCode = signal('');
  readonly activeStatus = signal<BudgetStatus | ''>('');
  readonly page = signal(1);
  readonly size = signal(20);
  readonly sortKey = signal<SortKey>('fiscalYear');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly statusTone = STATUS_TONE;
  readonly healthTone = HEALTH_TONE;
  readonly quarters = [1, 2, 3, 4] as const;
  readonly statuses: BudgetStatus[] = ['PLANNING', 'SUBMITTED', 'APPROVED', 'ACTIVE', 'CLOSED'];

  readonly filter = computed<BudgetListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    sort: `${this.sortKey()},${this.sortDirection()}`,
    department_id: this.departmentId().trim() || undefined,
    fiscal_year: this.fiscalYear() || undefined,
    quarter: this.quarter() || undefined,
    status: (this.activeStatus() as BudgetStatus) || undefined,
    gl_account_code: this.glAccountCode().trim() || undefined
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(
      this.fiscalYear() ||
        this.quarter() ||
        this.departmentId().trim() ||
        this.glAccountCode().trim() ||
        this.activeStatus()
    )
  );

  readonly visibleAllocated = computed(() => this.sumMoney('allocated'));
  readonly visibleAvailable = computed(() => this.sumMoney('available'));
  readonly warningCount = computed(() => this.items().filter((item) => this.health(item) === 'WARNING').length);
  readonly exceededCount = computed(() => this.items().filter((item) => this.health(item) === 'EXCEEDED').length);
  readonly riskCount = computed(() => this.warningCount() + this.exceededCount());

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.budgetService
      .list(this.filter())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.items.set(res.data ?? []);
          this.meta.set(res.meta ?? null);
        },
        error: () => {
          this.items.set([]);
          this.meta.set(null);
        }
      });
  }

  onFiscalYearChange(value: string): void {
    this.fiscalYear.set(value ? Number(value) : '');
    this.resetPageAndLoad();
  }

  onQuarterChange(value: string): void {
    this.quarter.set(value ? (Number(value) as QuarterFilter) : '');
    this.resetPageAndLoad();
  }

  onDepartmentChange(value: string): void {
    this.departmentId.set(value);
    this.resetPageAndLoad();
  }

  onGlAccountChange(value: string): void {
    this.glAccountCode.set(value);
    this.resetPageAndLoad();
  }

  onStatusChange(status: string): void {
    this.activeStatus.set(status as BudgetStatus | '');
    this.resetPageAndLoad();
  }

  clearFilters(): void {
    this.fiscalYear.set('');
    this.quarter.set('');
    this.departmentId.set('');
    this.glAccountCode.set('');
    this.activeStatus.set('');
    this.resetPageAndLoad();
  }

  onSort(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((direction) => (direction === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortKey.set(key);
      this.sortDirection.set(key === 'fiscalYear' || key === 'allocated' ? 'desc' : 'asc');
    }
    this.resetPageAndLoad();
  }

  sortIcon(key: SortKey): string {
    if (this.sortKey() !== key) {
      return 'chevron-down';
    }
    return this.sortDirection() === 'asc' ? 'chevron-up' : 'chevron-down';
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  navigateDetail(id: string): void {
    this.router.navigate(['/finance', 'budgets', id]);
  }

  amount(budget: BudgetDashboard, field: 'allocated' | 'committed' | 'spent' | 'available') {
    return budgetMoney(budget, field);
  }

  health(budget: BudgetDashboard): BudgetHealthTone {
    return budgetHealthTone(budget);
  }

  periodLabelKey(budget: BudgetDashboard): string {
    return budget.quarter ? 'finance.budget.list.period.quarter' : 'finance.budget.list.period.allYear';
  }

  utilizationPercent(budget: BudgetDashboard): number {
    const allocated = Number(budget.allocated ?? 0);
    if (allocated <= 0) {
      return 0;
    }
    const used = Number(budget.committed ?? 0) + Number(budget.spent ?? 0);
    return Math.round((used / allocated) * 100);
  }

  utilizationWidth(budget: BudgetDashboard): string {
    return `${Math.min(this.utilizationPercent(budget), 100)}%`;
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }

  shortId(value: string | null | undefined): string {
    if (!value) {
      return '--';
    }
    return value.length <= 12 ? value : `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  private sumMoney(field: 'allocated' | 'available'): string {
    return this.items()
      .reduce((sum, item) => sum + Number(item[field] ?? 0), 0)
      .toFixed(4);
  }

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
