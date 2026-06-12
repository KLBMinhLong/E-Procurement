import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { PageMeta } from '../../../../core/models/api-response.model';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import {
  StockMovement,
  StockMovementListFilter,
  StockMovementType,
  Warehouse
} from '../../models/stock.model';
import { StockService } from '../../services/stock.service';
import { WarehouseService } from '../../services/warehouse.service';

const MOVEMENT_TONE: Record<StockMovementType, EpBadgeTone> = {
  RECEIPT_IN: 'success',
  ISSUE_OUT: 'warning',
  ADJUSTMENT: 'info',
  TRANSFER: 'neutral'
};

@Component({
  selector: 'ep-stock-movements',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpSkeletonComponent
  ],
  templateUrl: './stock-movements.html',
  styleUrl: './stock-movements.scss'
})
export class StockMovementsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly stockService = inject(StockService);
  private readonly warehouseService = inject(WarehouseService);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<StockMovement[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly isLoading = signal(false);

  readonly itemCode = signal('');
  readonly warehouseId = signal('');
  readonly movementType = signal<StockMovementType | ''>('');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly page = signal(1);
  readonly size = signal(20);

  readonly movementTypes: StockMovementType[] = ['RECEIPT_IN', 'ISSUE_OUT', 'ADJUSTMENT', 'TRANSFER'];
  readonly movementTone = MOVEMENT_TONE;

  readonly filter = computed<StockMovementListFilter>(() => ({
    item_code: this.itemCode().trim() || undefined,
    warehouse_id: this.warehouseId() || undefined,
    movement_type: (this.movementType() as StockMovementType) || undefined,
    from_date: this.fromDate() || undefined,
    to_date: this.toDate() || undefined,
    page: this.page(),
    size: this.size()
  }));

  readonly hasActiveFilters = computed(() =>
    Boolean(this.itemCode().trim() || this.warehouseId() || this.movementType() || this.fromDate() || this.toDate())
  );

  readonly receiptCount = computed(() => this.items().filter((item) => item.movementType === 'RECEIPT_IN').length);
  readonly issueCount = computed(() => this.items().filter((item) => item.movementType === 'ISSUE_OUT').length);
  readonly adjustmentCount = computed(() => this.items().filter((item) => item.movementType === 'ADJUSTMENT').length);
  readonly transferCount = computed(() => this.items().filter((item) => item.movementType === 'TRANSFER').length);

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.itemCode.set(params.get('item_code') ?? '');
    this.warehouseId.set(params.get('warehouse_id') ?? '');
    this.movementType.set((params.get('movement_type') as StockMovementType | null) ?? '');
    this.loadInitialData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.stockService
      .listMovements(this.filter())
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

  onItemCodeChange(value: string): void {
    this.itemCode.set(value);
    this.resetPageAndLoad();
  }

  onWarehouseChange(warehouseId: string): void {
    this.warehouseId.set(warehouseId);
    this.resetPageAndLoad();
  }

  onMovementTypeChange(value: string): void {
    this.movementType.set(value as StockMovementType | '');
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
    this.itemCode.set('');
    this.warehouseId.set('');
    this.movementType.set('');
    this.fromDate.set('');
    this.toDate.set('');
    this.resetPageAndLoad();
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  navigateStock(): void {
    this.router.navigate(['/inventory/stock'], {
      queryParams: { warehouse_id: this.warehouseId() || undefined }
    });
  }

  openSource(movement: StockMovement): void {
    if (movement.sourceRefType === 'GOODS_RECEIPT' && movement.sourceRefId) {
      this.router.navigate(['/inventory/goods-receipts', movement.sourceRefId]);
    }
  }

  sourceLabel(movement: StockMovement): string {
    if (!movement.sourceRefType && !movement.sourceRefId) {
      return '--';
    }

    return [movement.sourceRefType, movement.sourceRefId].filter(Boolean).join(' / ');
  }

  formatQuantity(movement: StockMovement): string {
    const value = Number(movement.quantity || '0').toLocaleString('vi-VN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4
    });

    return movement.movementType === 'ISSUE_OUT' ? `-${value}` : value;
  }

  formatBalance(value: string | null | undefined): string {
    if (!value) {
      return '0';
    }

    return Number(value).toLocaleString('vi-VN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4
    });
  }

  formatDateTime(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(iso));
  }

  private loadInitialData(): void {
    forkJoin({
      warehouses: this.warehouseService.list()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ warehouses }) => this.warehouses.set(warehouses.data ?? []),
        error: () => this.warehouses.set([])
      });

    this.loadData();
  }

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
