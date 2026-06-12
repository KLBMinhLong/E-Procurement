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
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { PageMeta } from '../../../../core/models/api-response.model';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { ToastService } from '../../../../core/services/toast.service';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
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
    ReactiveFormsModule,
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent,
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
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);

  readonly warehouses = signal<Warehouse[]>([]);
  readonly items = signal<StockEntry[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly isAdjustModalOpen = signal(false);
  readonly isSavingAdjustment = signal(false);
  readonly adjustingItem = signal<StockEntry | null>(null);

  readonly warehouseId = signal('');
  readonly belowReorder = signal(false);
  readonly page = signal(1);
  readonly size = signal(20);

  readonly adjustmentForm = this.fb.group({
    itemCode: ['', [Validators.required, Validators.maxLength(20)]],
    itemName: [''],
    warehouseId: ['', [Validators.required]],
    currentQuantity: ['0'],
    newQuantity: ['0', [Validators.required, Validators.pattern(/^\d+(\.\d{1,4})?$/)]],
    unit: ['', [Validators.required, Validators.maxLength(20)]],
    reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]]
  });

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

  openAdjustment(item: StockEntry): void {
    this.adjustingItem.set(item);
    this.adjustmentForm.reset({
      itemCode: item.itemCode,
      itemName: item.itemName,
      warehouseId: item.warehouseId,
      currentQuantity: item.quantityOnHand,
      newQuantity: item.quantityOnHand,
      unit: item.unit,
      reason: ''
    });
    this.isAdjustModalOpen.set(true);
  }

  closeAdjustment(): void {
    if (this.isSavingAdjustment()) {
      return;
    }
    this.isAdjustModalOpen.set(false);
    this.adjustingItem.set(null);
  }

  saveAdjustment(): void {
    if (this.adjustmentForm.invalid || this.isSavingAdjustment()) {
      this.adjustmentForm.markAllAsTouched();
      return;
    }

    const raw = this.adjustmentForm.getRawValue();
    const current = this.toNumber(raw.currentQuantity);
    const next = this.toNumber(raw.newQuantity);
    if (current === next) {
      this.adjustmentForm.controls.newQuantity.setErrors({ unchanged: true });
      return;
    }

    this.isSavingAdjustment.set(true);
    this.stockService
      .adjust({
        warehouseId: raw.warehouseId ?? '',
        itemCode: raw.itemCode ?? '',
        newQuantity: raw.newQuantity ?? '0',
        unit: raw.unit ?? '',
        reason: raw.reason ?? ''
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSavingAdjustment.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey('inventory.stock.adjustment.toast.success');
          this.closeAdjustment();
          this.loadData();
        },
        error: () => {
          this.toastService.errorKey('inventory.stock.adjustment.toast.failed');
        }
      });
  }

  adjustmentDelta(): string {
    const current = this.toNumber(this.adjustmentForm.controls.currentQuantity.value);
    const next = this.toNumber(this.adjustmentForm.controls.newQuantity.value);
    const delta = next - current;
    if (!Number.isFinite(delta)) {
      return '0';
    }
    return this.formatDecimal(delta);
  }

  adjustmentFormError(
    controlName: 'itemCode' | 'itemName' | 'warehouseId' | 'currentQuantity' | 'newQuantity' | 'unit' | 'reason'
  ): string | null {
    const control = this.adjustmentForm.controls[controlName];
    if (!control || !control.touched || !control.errors) {
      return null;
    }
    if (control.errors['required']) {
      return 'inventory.stock.adjustment.validation.required';
    }
    if (control.errors['pattern']) {
      return 'inventory.stock.adjustment.validation.numeric';
    }
    if (control.errors['minlength']) {
      return 'inventory.stock.adjustment.validation.reasonLength';
    }
    if (control.errors['maxlength']) {
      return 'inventory.stock.adjustment.validation.maxLength';
    }
    if (control.errors['unchanged']) {
      return 'inventory.stock.adjustment.validation.unchanged';
    }
    return 'inventory.stock.adjustment.validation.invalid';
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

  private toNumber(value: string | number | null | undefined): number {
    if (value === null || value === undefined || value === '') {
      return 0;
    }
    return Number(value);
  }

  private formatDecimal(value: number): string {
    return value.toLocaleString('vi-VN', {
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
