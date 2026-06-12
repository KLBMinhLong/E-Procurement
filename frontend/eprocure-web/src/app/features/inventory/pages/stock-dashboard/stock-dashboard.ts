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
import { finalize } from 'rxjs';

import { PageMeta } from '../../../../core/models/api-response.model';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpPageChangeEvent } from '../../../../shared/shared.index';
import { StockEntry, Warehouse, WarehouseStockFilter } from '../../models/stock.model';
import { StockService } from '../../services/stock.service';
import { WarehouseService } from '../../services/warehouse.service';

@Component({
  selector: 'ep-stock-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './stock-dashboard.html',
  styleUrl: './stock-dashboard.scss'
})
export class StockDashboardComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly stockService = inject(StockService);
  private readonly warehouseService = inject(WarehouseService);
  private readonly destroyRef = inject(DestroyRef);

  readonly warehouses = signal<Warehouse[]>([]);
  readonly items = signal<StockEntry[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);

  readonly warehouseId = signal('');
  readonly belowReorder = signal(false);
  readonly page = signal(1);
  readonly size = signal(20);

  readonly selectedWarehouse = computed(() =>
    this.warehouses().find((warehouse) => warehouse.id === this.warehouseId()) ?? null
  );

  readonly filter = computed<WarehouseStockFilter | null>(() => {
    const warehouseId = this.warehouseId();
    if (!warehouseId) {
      return null;
    }

    return {
      warehouseId,
      below_reorder: this.belowReorder() || undefined,
      page: this.page(),
      size: this.size()
    };
  });

  readonly hasActiveFilters = computed(() => Boolean(this.belowReorder()));
  readonly skuCount = computed(() => new Set(this.items().map((item) => item.itemCode)).size);
  readonly belowReorderCount = computed(() => this.items().filter((item) => item.isBelowReorder).length);
  readonly quantityRows = computed(() => this.items().length);
  readonly lastUpdated = computed(() => {
    const timestamps = this.items()
      .map((item) => item.lastUpdated)
      .filter(Boolean)
      .map((value) => new Date(value).getTime())
      .filter((value) => Number.isFinite(value));

    if (timestamps.length === 0) {
      return '--';
    }

    return this.formatDateTime(new Date(Math.max(...timestamps)).toISOString());
  });

  ngOnInit(): void {
    this.warehouseId.set(this.route.snapshot.queryParamMap.get('warehouse_id') ?? '');
    this.loadWarehouses();
  }

  loadData(): void {
    const filter = this.filter();
    if (!filter) {
      this.items.set([]);
      this.meta.set(null);
      return;
    }

    this.isLoading.set(true);
    this.stockService
      .listWarehouseStock(filter)
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

  onWarehouseChange(warehouseId: string): void {
    this.warehouseId.set(warehouseId);
    this.resetPageAndLoad();
  }

  onBelowReorderChange(checked: boolean): void {
    this.belowReorder.set(checked);
    this.resetPageAndLoad();
  }

  clearFilters(): void {
    this.belowReorder.set(false);
    this.resetPageAndLoad();
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  navigateMovements(item?: StockEntry): void {
    this.router.navigate(['/inventory/stock/movements'], {
      queryParams: {
        item_code: item?.itemCode || undefined,
        warehouse_id: item?.warehouseId || this.warehouseId() || undefined
      }
    });
  }

  navigateIssueOut(item?: StockEntry): void {
    this.router.navigate(['/inventory/issue-out'], {
      queryParams: {
        item_code: item?.itemCode || undefined,
        warehouse_id: item?.warehouseId || this.warehouseId() || undefined
      }
    });
  }

  formatQuantity(value: string | null | undefined): string {
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

  private loadWarehouses(): void {
    this.warehouseService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const warehouses = res.data ?? [];
          this.warehouses.set(warehouses);
          if (!this.warehouseId() && warehouses.length > 0) {
            this.warehouseId.set(warehouses[0].id);
          }
          this.loadData();
        },
        error: () => {
          this.warehouses.set([]);
          this.items.set([]);
          this.meta.set(null);
        }
      });
  }

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }
}
