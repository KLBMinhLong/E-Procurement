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

import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { PageMeta } from '../../../../core/models/api-response.model';
import { PurchaseRequestService } from '../../services/purchase-request.service';
import { Money, PrListFilter, PrPriority, PrStatus, PurchaseRequestSummary } from '../../models/purchase-request.model';
import { EpPageChangeEvent } from '../../../../shared/shared.index';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  PENDING_APPROVAL: 'warning',
  CHANGES_REQUESTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CONVERTED_TO_PO: 'success',
  CANCELLED: 'neutral',
  CLOSED: 'neutral'
};

const PRIORITY_TONE: Record<string, EpBadgeTone> = {
  NORMAL: 'neutral',
  URGENT: 'warning',
  EMERGENCY: 'danger'
};

type SortKey = 'createdAt' | 'prNumber' | 'needByDate' | 'totalAmount';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'ep-pr-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpAmountComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpButtonComponent,
    EpFilterBarComponent,
    EpBreadcrumbComponent,
    EpIconComponent,
    HasPermissionDirective
  ],
  templateUrl: './pr-list.component.html',
  styleUrl: './pr-list.component.scss'
})
export class PrListComponent implements OnInit {
  private readonly prService = inject(PurchaseRequestService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<PurchaseRequestSummary[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');
  readonly activeStatus = signal<PrStatus | ''>('');
  readonly activePriority = signal<PrPriority | ''>('');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly minAmount = signal('');
  readonly maxAmount = signal('');
  readonly page = signal(1);
  readonly size = signal(20);
  readonly sortKey = signal<SortKey>('createdAt');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly filter = computed<PrListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    sort: `${this.sortKey()},${this.sortDirection()}`,
    q: this.searchQuery().trim() || undefined,
    status: (this.activeStatus() as PrStatus) || undefined,
    priority: (this.activePriority() as PrPriority) || undefined,
    from_date: this.fromDate() || undefined,
    to_date: this.toDate() || undefined,
    min_amount: this.minAmount().trim() || undefined,
    max_amount: this.maxAmount().trim() || undefined
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(
      this.searchQuery().trim() ||
      this.activeStatus() ||
      this.activePriority() ||
      this.fromDate() ||
      this.toDate() ||
      this.minAmount().trim() ||
      this.maxAmount().trim()
    )
  );

  readonly visibleTotalAmount = computed(() =>
    this.items().reduce((sum, item) => sum + this.amountNumber(item.totalAmount), 0).toFixed(4)
  );
  readonly draftCount = computed(() => this.items().filter((item) => item.status === 'DRAFT').length);
  readonly pendingCount = computed(() =>
    this.items().filter((item) => ['SUBMITTED', 'PENDING_APPROVAL', 'CHANGES_REQUESTED'].includes(item.status)).length
  );
  readonly completedCount = computed(() =>
    this.items().filter((item) => ['APPROVED', 'CONVERTED_TO_PO', 'CLOSED'].includes(item.status)).length
  );

  readonly statusTone = STATUS_TONE;
  readonly priorityTone = PRIORITY_TONE;

  readonly statuses: PrStatus[] = [
    'DRAFT',
    'SUBMITTED',
    'PENDING_APPROVAL',
    'CHANGES_REQUESTED',
    'APPROVED',
    'REJECTED',
    'CANCELLED',
    'CLOSED'
  ];

  readonly priorities: PrPriority[] = ['NORMAL', 'URGENT', 'EMERGENCY'];

  ngOnInit(): void {
    this.loadData();
  }

  onSearchChange(query: string): void {
    this.searchQuery.set(query);
    this.resetPageAndLoad();
  }

  onStatusChange(status: string): void {
    this.activeStatus.set(status as PrStatus | '');
    this.resetPageAndLoad();
  }

  onPriorityChange(priority: string): void {
    this.activePriority.set(priority as PrPriority | '');
    this.resetPageAndLoad();
  }

  onDateFilterChange(field: 'from' | 'to', value: string): void {
    if (field === 'from') {
      this.fromDate.set(value);
    } else {
      this.toDate.set(value);
    }
    this.resetPageAndLoad();
  }

  onAmountFilterChange(field: 'min' | 'max', value: string): void {
    const normalized = value.replace(/[^\d.]/g, '');
    if (field === 'min') {
      this.minAmount.set(normalized);
    } else {
      this.maxAmount.set(normalized);
    }
    this.resetPageAndLoad();
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.activeStatus.set('');
    this.activePriority.set('');
    this.fromDate.set('');
    this.toDate.set('');
    this.minAmount.set('');
    this.maxAmount.set('');
    this.resetPageAndLoad();
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  onSort(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((direction) => (direction === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortKey.set(key);
      this.sortDirection.set(key === 'createdAt' ? 'desc' : 'asc');
    }
    this.resetPageAndLoad();
  }

  sortIcon(key: SortKey): string {
    if (this.sortKey() !== key) {
      return 'chevron-down';
    }
    return this.sortDirection() === 'asc' ? 'chevron-up' : 'chevron-down';
  }

  onRowClick(id: string): void {
    this.router.navigate(['/procurement', id]);
  }

  navigateCreate(): void {
    this.router.navigate(['/procurement', 'create']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '--';
    }
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso));
  }

  requesterLabel(pr: PurchaseRequestSummary): string {
    return pr.requester?.fullName || this.shortId(pr.requesterId);
  }

  departmentLabel(pr: PurchaseRequestSummary): string {
    return pr.requester?.department || this.shortId(pr.departmentId);
  }

  shortId(value: string | null | undefined): string {
    if (!value) {
      return '--';
    }
    if (value.length <= 12) {
      return value;
    }
    return `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  loadData(): void {
    this.isLoading.set(true);

    this.prService
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

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }

  private amountNumber(value: Money | string | number | null | undefined): number {
    const raw = typeof value === 'object' && value !== null ? value.amount : value;
    const parsed = Number(raw ?? 0);
    return Number.isFinite(parsed) ? parsed : 0;
  }
}
