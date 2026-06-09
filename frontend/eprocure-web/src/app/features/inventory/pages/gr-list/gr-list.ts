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
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import { GoodsReceiptDetail, GoodsReceiptStatus } from '../../models/goods-receipt.model';
import { GoodsReceiptService, GoodsReceiptListFilter } from '../../services/goods-receipt.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  DRAFT: 'neutral',
  PARTIAL: 'warning',
  COMPLETE: 'success',
  DISCREPANCY: 'danger'
};

type SortKey = 'receivedAt' | 'createdAt' | 'grNumber' | 'poNumber' | 'status';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'ep-gr-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFilterBarComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './gr-list.html',
  styleUrl: './gr-list.scss'
})
export class GrList implements OnInit {
  private readonly router = inject(Router);
  private readonly grService = inject(GoodsReceiptService);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<GoodsReceiptDetail[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);

  readonly poId = signal('');
  readonly activeStatus = signal<GoodsReceiptStatus | ''>('');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly page = signal(1);
  readonly size = signal(20);
  readonly sortKey = signal<SortKey>('createdAt');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly statusTone = STATUS_TONE;
  readonly statuses: GoodsReceiptStatus[] = [
    'DRAFT',
    'PARTIAL',
    'COMPLETE',
    'DISCREPANCY'
  ];

  readonly filter = computed<GoodsReceiptListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    sort: `${this.sortKey()},${this.sortDirection()}`,
    status: (this.activeStatus() as GoodsReceiptStatus) || undefined,
    po_id: this.poId().trim() || undefined,
    from_date: this.fromDate() || undefined,
    to_date: this.toDate() || undefined
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(this.activeStatus() || this.poId().trim() || this.fromDate() || this.toDate())
  );

  readonly draftCount = computed(() => this.items().filter((item) => item.status === 'DRAFT').length);
  readonly partialCount = computed(() => this.items().filter((item) => item.status === 'PARTIAL').length);
  readonly completeCount = computed(() => this.items().filter((item) => item.status === 'COMPLETE').length);
  readonly discrepancyCount = computed(() => this.items().filter((item) => item.status === 'DISCREPANCY').length);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.grService
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

  onStatusChange(status: string): void {
    this.activeStatus.set(status as GoodsReceiptStatus | '');
    this.resetPageAndLoad();
  }

  onPoSearch(value: string): void {
    this.poId.set(value);
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

  clearFilters(): void {
    this.activeStatus.set('');
    this.poId.set('');
    this.fromDate.set('');
    this.toDate.set('');
    this.resetPageAndLoad();
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

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  navigateCreate(): void {
    this.router.navigate(['/inventory/goods-receipts/create']);
  }

  navigateDetail(id: string): void {
    this.router.navigate(['/inventory/goods-receipts', id]);
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

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
