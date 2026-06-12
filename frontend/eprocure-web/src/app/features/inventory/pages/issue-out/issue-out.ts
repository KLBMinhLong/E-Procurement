import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { ToastService } from '../../../../core/services/toast.service';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import {
  IssueOutStockRequest,
  StockEntry,
  StockMovement,
  Warehouse
} from '../../models/stock.model';
import { StockService } from '../../services/stock.service';
import { WarehouseService } from '../../services/warehouse.service';

@Component({
  selector: 'ep-issue-out',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpCardComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpSkeletonComponent
  ],
  templateUrl: './issue-out.html',
  styleUrl: './issue-out.scss'
})
export class IssueOutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly stockService = inject(StockService);
  private readonly warehouseService = inject(WarehouseService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(true);
  readonly stockLoading = signal(false);
  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly stockRows = signal<StockEntry[]>([]);
  readonly createdMovements = signal<StockMovement[]>([]);

  readonly form: FormGroup = this.fb.group({
    warehouseId: ['', Validators.required],
    recipientId: ['', Validators.required],
    prId: [''],
    notes: [''],
    lines: this.fb.array([])
  });

  get lines(): FormArray {
    return this.form.get('lines') as FormArray;
  }

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const initialWarehouseId = params.get('warehouse_id') ?? '';
    const initialItemCode = params.get('item_code') ?? '';

    forkJoin({
      warehouses: this.warehouseService.list()
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: ({ warehouses }) => {
          const items = warehouses.data ?? [];
          this.warehouses.set(items);
          this.form.patchValue({ warehouseId: initialWarehouseId || items[0]?.id || '' });
          this.addLine(initialItemCode);
          this.loadStock();
        },
        error: () => {
          this.warehouses.set([]);
          this.addLine(initialItemCode);
        }
      });
  }

  addLine(itemCode = ''): void {
    this.lines.push(this.fb.group({
      itemCode: [itemCode, Validators.required],
      quantity: ['1', [Validators.required, Validators.min(0.0001)]],
      unit: [{ value: '', disabled: true }]
    }));

    if (itemCode) {
      this.syncLineUnit(this.lines.length - 1, itemCode);
    }
  }

  removeLine(index: number): void {
    if (this.lines.length === 1) {
      this.lines.at(0).reset({ itemCode: '', quantity: '1', unit: '' });
      return;
    }

    this.lines.removeAt(index);
  }

  onWarehouseChange(): void {
    this.createdMovements.set([]);
    this.lines.clear();
    this.addLine();
    this.loadStock();
  }

  onLineItemChange(index: number, itemCode: string): void {
    this.syncLineUnit(index, itemCode);
  }

  fieldError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl || (!ctrl.touched && !this.submitted())) return null;
    if (ctrl.errors?.['required']) return 'features.inventory.issueOut.validation.required';
    return null;
  }

  lineError(index: number, field: 'itemCode' | 'quantity'): string | null {
    const ctrl = this.lines.at(index).get(field);
    if (!ctrl || (!ctrl.touched && !this.submitted())) return null;
    if (ctrl.errors?.['required']) return 'features.inventory.issueOut.validation.required';
    if (ctrl.errors?.['min']) return 'features.inventory.issueOut.validation.positive';
    if (field === 'quantity' && this.isLineInsufficient(index)) {
      return 'features.inventory.issueOut.validation.insufficient';
    }
    return null;
  }

  availableQuantity(index: number): string {
    const itemCode = this.lines.at(index).get('itemCode')?.value;
    return this.stockRows().find((item) => item.itemCode === itemCode)?.quantityOnHand ?? '0';
  }

  isLineInsufficient(index: number): boolean {
    const quantity = Number(this.lines.at(index).get('quantity')?.value || '0');
    const available = Number(this.availableQuantity(index));
    return quantity > available;
  }

  hasInvalidLines(): boolean {
    if (this.lines.length === 0) {
      return true;
    }

    return this.lines.controls.some((_, index) => this.isLineInsufficient(index));
  }

  onSubmit(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid || this.hasInvalidLines() || this.submitting()) {
      this.toast.errorKey('features.inventory.issueOut.validation.invalid');
      return;
    }

    const rawValue = this.form.getRawValue();
    const request: IssueOutStockRequest = {
      warehouseId: rawValue.warehouseId,
      recipientId: rawValue.recipientId,
      prId: rawValue.prId || null,
      notes: rawValue.notes || null,
      items: rawValue.lines.map((line: { itemCode: string; quantity: string; unit: string }) => ({
        itemCode: line.itemCode,
        quantity: String(line.quantity),
        unit: line.unit
      }))
    };

    this.submitting.set(true);
    this.stockService
      .issueOut(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.createdMovements.set(res.data?.movements ?? []);
          this.toast.successKey('features.inventory.issueOut.toast.success');
          this.loadStock();
        },
        error: () => this.toast.errorKey('features.inventory.issueOut.toast.failed')
      });
  }

  navigateStock(): void {
    this.router.navigate(['/inventory/stock'], {
      queryParams: { warehouse_id: this.form.get('warehouseId')?.value || undefined }
    });
  }

  navigateMovements(): void {
    this.router.navigate(['/inventory/stock/movements'], {
      queryParams: { warehouse_id: this.form.get('warehouseId')?.value || undefined }
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

  private loadStock(): void {
    const warehouseId = this.form.get('warehouseId')?.value;
    if (!warehouseId) {
      this.stockRows.set([]);
      return;
    }

    this.stockLoading.set(true);
    this.stockService
      .listWarehouseStock({ warehouseId, page: 1, size: 200 })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.stockLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.stockRows.set(res.data ?? []);
          this.lines.controls.forEach((control, index) => {
            this.syncLineUnit(index, control.get('itemCode')?.value ?? '');
          });
        },
        error: () => this.stockRows.set([])
      });
  }

  private syncLineUnit(index: number, itemCode: string): void {
    const stock = this.stockRows().find((item) => item.itemCode === itemCode);
    this.lines.at(index).patchValue({ unit: stock?.unit ?? '' }, { emitEvent: false });
  }
}
