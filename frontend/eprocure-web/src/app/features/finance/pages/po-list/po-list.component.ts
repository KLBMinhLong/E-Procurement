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
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import {
  PurchaseOrder,
  PurchaseOrderListFilter,
  PurchaseOrderStatus,
  purchaseOrderMoney
} from '../../models/purchase-order.model';
import { PurchaseOrderService } from '../../services/purchase-order.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  DRAFT: 'neutral',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  SENT_TO_VENDOR: 'info',
  PARTIALLY_RECEIVED: 'warning',
  FULLY_RECEIVED: 'success',
  INVOICED: 'info',
  PAID: 'success',
  CLOSED: 'neutral',
  CANCELLED: 'danger'
};

type SortKey = 'createdAt' | 'poNumber' | 'vendorName' | 'totalAmount' | 'status';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'ep-po-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFilterBarComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './po-list.component.html',
  styleUrl: './po-list.component.scss'
})
export class PoListComponent implements OnInit {
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<PurchaseOrder[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly vendorId = signal('');
  readonly activeStatus = signal<PurchaseOrderStatus | ''>('');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly page = signal(1);
  readonly size = signal(20);
  readonly sortKey = signal<SortKey>('createdAt');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly statusTone = STATUS_TONE;
  readonly statuses: PurchaseOrderStatus[] = [
    'DRAFT',
    'SENT_TO_VENDOR',
    'PARTIALLY_RECEIVED',
    'FULLY_RECEIVED',
    'INVOICED',
    'PAID',
    'CANCELLED',
    'CLOSED'
  ];

  readonly filter = computed<PurchaseOrderListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    sort: `${this.sortKey()},${this.sortDirection()}`,
    status: (this.activeStatus() as PurchaseOrderStatus) || undefined,
    vendor_id: this.vendorId().trim() || undefined,
    from_date: this.fromDate() || undefined,
    to_date: this.toDate() || undefined
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(this.activeStatus() || this.vendorId().trim() || this.fromDate() || this.toDate())
  );

  readonly draftCount = computed(() => this.items().filter((item) => item.status === 'DRAFT').length);
  readonly issuedCount = computed(() =>
    this.items().filter((item) => ['SENT_TO_VENDOR', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED'].includes(item.status)).length
  );
  readonly callbackPendingCount = computed(() =>
    this.items().filter((item) => item.prConversionStatus && item.prConversionStatus !== 'DELIVERED').length
  );
  readonly visibleTotalAmount = computed(() =>
    this.items().reduce((sum, item) => sum + Number(item.totalAmount ?? 0), 0).toFixed(4)
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.purchaseOrderService
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
    this.activeStatus.set(status as PurchaseOrderStatus | '');
    this.resetPageAndLoad();
  }

  onVendorSearch(value: string): void {
    this.vendorId.set(value);
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
    this.vendorId.set('');
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
    this.router.navigate(['/finance', 'purchase-orders', 'create']);
  }

  navigateDetail(poId: string): void {
    this.router.navigate(['/finance', 'purchase-orders', poId]);
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

  amount(po: PurchaseOrder) {
    return purchaseOrderMoney(po);
  }

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
