import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { PageMeta } from '../../../../core/models/api-response.model';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { ToastService } from '../../../../core/services/toast.service';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { CatalogItem, CatalogItemFilter, CreateCatalogItemRequest, UpdateCatalogItemRequest } from '../../models/catalog.model';
import { InventoryCatalogService } from '../../services/catalog.service';

type ActiveFilter = 'all' | 'active' | 'inactive';

@Component({
  selector: 'ep-catalog-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    HasPermissionDirective,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent,
    EpSkeletonComponent
  ],
  templateUrl: './catalog-list.html',
  styleUrl: './catalog-list.scss'
})
export class CatalogListComponent implements OnInit {
  private readonly catalogService = inject(InventoryCatalogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly items = signal<CatalogItem[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isModalOpen = signal(false);
  readonly editingItem = signal<CatalogItem | null>(null);

  readonly query = signal('');
  readonly categoryCode = signal('');
  readonly activeFilter = signal<ActiveFilter>('all');
  readonly belowReorder = signal(false);
  readonly page = signal(1);
  readonly size = signal(20);

  readonly form = this.fb.group({
    itemCode: ['', [Validators.required, Validators.maxLength(20)]],
    name: ['', [Validators.required, Validators.maxLength(300)]],
    description: ['', [Validators.maxLength(2000)]],
    categoryCode: ['', [Validators.required, Validators.maxLength(50)]],
    unit: ['', [Validators.required, Validators.maxLength(20)]],
    unitPrice: ['0', [Validators.required, Validators.pattern(/^\d+(\.\d{1,4})?$/)]],
    currency: ['VND', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    preferredVendorId: [''],
    reorderPoint: ['', [Validators.pattern(/^\d+(\.\d{1,4})?$/)]],
    isActive: [true]
  });

  readonly filter = computed<CatalogItemFilter>(() => ({
    q: this.query().trim() || undefined,
    category_code: this.categoryCode().trim() || undefined,
    is_active: this.activeFilter() === 'all' ? undefined : this.activeFilter() === 'active',
    below_reorder: this.belowReorder() || undefined,
    page: this.page(),
    size: this.size()
  }));

  readonly activeCount = computed(() => this.items().filter((item) => item.isActive).length);
  readonly inactiveCount = computed(() => this.items().filter((item) => !item.isActive).length);
  readonly belowReorderCount = computed(() => this.items().filter((item) => this.isBelowReorder(item)).length);
  readonly categoryCount = computed(() => new Set(this.items().map((item) => item.categoryCode)).size);
  readonly hasActiveFilters = computed(() =>
    Boolean(this.query().trim() || this.categoryCode().trim() || this.activeFilter() !== 'all' || this.belowReorder())
  );
  readonly isEditMode = computed(() => Boolean(this.editingItem()));

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.catalogService
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
          this.toastService.errorKey('inventory.catalog.toast.loadFailed');
        }
      });
  }

  onQueryChange(value: string): void {
    this.query.set(value);
    this.resetPageAndLoad();
  }

  onCategoryChange(value: string): void {
    this.categoryCode.set(value);
    this.resetPageAndLoad();
  }

  onActiveFilterChange(value: ActiveFilter): void {
    this.activeFilter.set(value);
    this.resetPageAndLoad();
  }

  onBelowReorderChange(checked: boolean): void {
    this.belowReorder.set(checked);
    this.resetPageAndLoad();
  }

  clearFilters(): void {
    this.query.set('');
    this.categoryCode.set('');
    this.activeFilter.set('all');
    this.belowReorder.set(false);
    this.resetPageAndLoad();
  }

  onPageChange(page: number, size = this.size()): void {
    this.page.set(page);
    this.size.set(size);
    this.loadData();
  }

  openCreateModal(): void {
    this.editingItem.set(null);
    this.form.reset({
      itemCode: '',
      name: '',
      description: '',
      categoryCode: '',
      unit: '',
      unitPrice: '0',
      currency: 'VND',
      preferredVendorId: '',
      reorderPoint: '',
      isActive: true
    });
    this.isModalOpen.set(true);
  }

  openEditModal(item: CatalogItem): void {
    this.editingItem.set(item);
    this.form.reset({
      itemCode: item.itemCode,
      name: item.name,
      description: item.description ?? '',
      categoryCode: item.categoryCode,
      unit: item.unit,
      unitPrice: item.unitPrice.amount,
      currency: item.unitPrice.currency,
      preferredVendorId: item.preferredVendorId ?? '',
      reorderPoint: item.reorderPoint ?? '',
      isActive: item.isActive
    });
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    if (this.isSaving()) {
      return;
    }
    this.isModalOpen.set(false);
    this.editingItem.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const item = this.editingItem();
    this.isSaving.set(true);

    const request$ = item
      ? this.catalogService.update(item.itemCode, this.toUpdateRequest())
      : this.catalogService.create(this.toCreateRequest());

    request$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: (res) => {
          const saved = res.data;
          this.toastService.successKey(
            item ? 'inventory.catalog.toast.updateSuccess' : 'inventory.catalog.toast.createSuccess'
          );
          this.isModalOpen.set(false);
          this.editingItem.set(null);
          if (item) {
            this.items.update((items) => items.map((current) => current.itemCode === saved.itemCode ? saved : current));
          } else {
            this.loadData();
          }
        },
        error: () => {
          this.toastService.errorKey(
            item ? 'inventory.catalog.toast.updateFailed' : 'inventory.catalog.toast.createFailed'
          );
        }
      });
  }

  viewDetail(item: CatalogItem): void {
    this.router.navigate(['/inventory/catalog', item.itemCode]);
  }

  viewMovements(item: CatalogItem): void {
    this.router.navigate(['/inventory/stock/movements'], {
      queryParams: { item_code: item.itemCode }
    });
  }

  formError(controlName: string): string | null {
    const control = this.form.get(controlName);
    if (!control || !control.touched || control.valid) {
      return null;
    }

    if (control.hasError('required')) {
      return 'inventory.catalog.validation.required';
    }
    if (control.hasError('maxlength')) {
      return 'inventory.catalog.validation.maxLength';
    }
    if (control.hasError('pattern')) {
      return 'inventory.catalog.validation.numeric';
    }
    return 'inventory.catalog.validation.invalid';
  }

  statusTone(item: CatalogItem): EpBadgeTone {
    return item.isActive ? 'success' : 'neutral';
  }

  isBelowReorder(item: CatalogItem): boolean {
    if (!item.reorderPoint) {
      return false;
    }
    const reorderPoint = Number(item.reorderPoint);
    if (!Number.isFinite(reorderPoint)) {
      return false;
    }
    return item.stockSummary.some((stock) => Number(stock.quantityOnHand) <= reorderPoint);
  }

  totalStock(item: CatalogItem): string {
    const total = item.stockSummary.reduce((sum, stock) => sum + Number(stock.quantityOnHand || 0), 0);
    return this.formatQuantity(String(total));
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

  private resetPageAndLoad(): void {
    this.page.set(1);
    this.loadData();
  }

  private toCreateRequest(): CreateCatalogItemRequest {
    const value = this.form.getRawValue();
    return {
      itemCode: value.itemCode?.trim() ?? '',
      name: value.name?.trim() ?? '',
      description: this.nullable(value.description),
      categoryCode: value.categoryCode?.trim() ?? '',
      unit: value.unit?.trim() ?? '',
      unitPrice: {
        amount: value.unitPrice ?? '0',
        currency: (value.currency ?? 'VND').toUpperCase()
      },
      preferredVendorId: this.nullable(value.preferredVendorId),
      reorderPoint: this.nullable(value.reorderPoint)
    };
  }

  private toUpdateRequest(): UpdateCatalogItemRequest {
    const value = this.form.getRawValue();
    return {
      name: this.nullable(value.name),
      description: this.nullable(value.description),
      unitPrice: {
        amount: value.unitPrice ?? '0',
        currency: (value.currency ?? 'VND').toUpperCase()
      },
      preferredVendorId: this.nullable(value.preferredVendorId),
      reorderPoint: this.nullable(value.reorderPoint),
      isActive: Boolean(value.isActive)
    };
  }

  private nullable(value: string | null | undefined): string | null {
    const trimmed = value?.trim();
    return trimmed ? trimmed : null;
  }
}
