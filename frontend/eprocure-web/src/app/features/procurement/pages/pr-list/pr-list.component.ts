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
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { PageMeta } from '../../../../core/models/api-response.model';
import { PurchaseRequestService } from '../../services/purchase-request.service';
import { PrListFilter, PrPriority, PrStatus, PurchaseRequestSummary } from '../../models/purchase-request.model';
import { EpPageChangeEvent, EpSortChangeEvent, EpTableColumn } from '../../../../shared/shared.index';

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
    HasPermissionDirective
  ],
  templateUrl: './pr-list.component.html',
  styleUrl: './pr-list.component.scss'
})
export class PrListComponent implements OnInit {
  private readonly prService = inject(PurchaseRequestService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly items = signal<PurchaseRequestSummary[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');
  readonly activeStatus = signal<PrStatus | ''>('');
  readonly activePriority = signal<PrPriority | ''>('');

  readonly filter = computed<PrListFilter>(() => ({
    page: 1,
    size: 20,
    sort: 'createdAt,desc',
    q: this.searchQuery() || undefined,
    status: (this.activeStatus() as PrStatus) || undefined,
    priority: (this.activePriority() as PrPriority) || undefined
  }));

  // ── Table config ──────────────────────────────────────────────────
  readonly columns: EpTableColumn[] = [
    { key: 'prNumber', labelKey: 'pr.list.col.number', sortable: true },
    { key: 'title', labelKey: 'pr.list.col.title' },
    { key: 'priority', labelKey: 'pr.list.col.priority' },
    { key: 'status', labelKey: 'pr.list.col.status' },
    { key: 'totalAmount', labelKey: 'pr.list.col.amount', align: 'right' },
    { key: 'requester', labelKey: 'pr.list.col.requester' },
    { key: 'createdAt', labelKey: 'pr.list.col.createdAt', sortable: true, align: 'right' }
  ];

  readonly statusTone = STATUS_TONE;
  readonly priorityTone = PRIORITY_TONE;

  readonly statuses: PrStatus[] = [
    'DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'CHANGES_REQUESTED',
    'APPROVED', 'REJECTED', 'CANCELLED', 'CLOSED'
  ];

  readonly priorities: PrPriority[] = ['NORMAL', 'URGENT', 'EMERGENCY'];

  // ── Lifecycle ─────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadData();
  }

  // ── Event handlers ────────────────────────────────────────────────
  onSearchChange(q: string): void {
    this.searchQuery.set(q);
    this.loadData();
  }

  onStatusChange(status: string): void {
    this.activeStatus.set(status as PrStatus | '');
    this.loadData();
  }

  onPriorityChange(priority: string): void {
    this.activePriority.set(priority as PrPriority | '');
    this.loadData();
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.loadData({ page: event.page, size: event.size });
  }

  onSortChange(event: EpSortChangeEvent): void {
    this.loadData({ sort: `${event.key},${event.direction}` });
  }

  onRowClick(row: Record<string, unknown>): void {
    this.router.navigate(['/procurement', row['id']]);
  }

  navigateCreate(): void {
    this.router.navigate(['/procurement', 'create']);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('vi-VN');
  }

  // ── Private ───────────────────────────────────────────────────────
  loadData(overrides: Partial<PrListFilter> = {}): void {
    this.isLoading.set(true);
    const f: PrListFilter = { ...this.filter(), ...overrides };

    this.prService
      .list(f)
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
        }
      });
  }
}
