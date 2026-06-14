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
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

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
import { CatalogItem, UpdateCatalogItemRequest } from '../../models/catalog.model';
import { InventoryCatalogService } from '../../services/catalog.service';

@Component({
  selector: 'ep-catalog-detail',
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
  templateUrl: './catalog-detail.html',
  styleUrl: './catalog-detail.scss'
})
export class CatalogDetailComponent implements OnInit {
  private readonly catalogService = inject(InventoryCatalogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly item = signal<CatalogItem | null>(null);
  readonly itemCode = signal('');
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isModalOpen = signal(false);

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

  readonly totalStock = computed(() => {
    const item = this.item();
    if (!item) {
      return '0';
    }
    const total = item.stockSummary.reduce((sum, stock) => sum + Number(stock.quantityOnHand || 0), 0);
    return this.formatQuantity(String(total));
  });

  readonly warehouseCount = computed(() => this.item()?.stockSummary.length ?? 0);
  readonly belowReorder = computed(() => {
    const item = this.item();
    return item ? this.isBelowReorder(item) : false;
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.itemCode.set(params.get('itemCode') ?? '');
        this.loadData();
      });
  }

  loadData(): void {
    const itemCode = this.itemCode();
    if (!itemCode) {
      this.item.set(null);
      return;
    }

    this.isLoading.set(true);
    this.catalogService
      .get(itemCode)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => this.item.set(res.data),
        error: () => {
          this.item.set(null);
          this.toastService.errorKey('inventory.catalog.toast.loadFailed');
        }
      });
  }

  openEditModal(item: CatalogItem): void {
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
  }

  save(): void {
    const item = this.item();
    if (!item || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.catalogService
      .update(item.itemCode, this.toUpdateRequest())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: (res) => {
          this.item.set(res.data);
          this.isModalOpen.set(false);
          this.toastService.successKey('inventory.catalog.toast.updateSuccess');
        },
        error: () => this.toastService.errorKey('inventory.catalog.toast.updateFailed')
      });
  }

  backToList(): void {
    this.router.navigate(['/inventory/catalog']);
  }

  viewMovements(): void {
    const item = this.item();
    this.router.navigate(['/inventory/stock/movements'], {
      queryParams: { item_code: item?.itemCode || this.itemCode() }
    });
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

  formatQuantity(value: string | null | undefined): string {
    if (!value) {
      return '0';
    }
    return Number(value).toLocaleString('vi-VN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4
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
