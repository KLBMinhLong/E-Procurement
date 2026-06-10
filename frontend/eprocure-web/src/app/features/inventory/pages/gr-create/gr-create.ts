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
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { GoodsReceiptService } from '../../services/goods-receipt.service';
import { WarehouseService } from '../../services/warehouse.service';
import { PurchaseOrderService } from '../../../finance/services/purchase-order.service';
import { PurchaseOrder } from '../../../finance/models/purchase-order.model';
import { Warehouse } from '../../models/stock.model';
import { ToastService } from '../../../../core/services/toast.service';

import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';

const RECEIVABLE_PO_STATUSES = new Set(['SENT_TO_VENDOR', 'PARTIALLY_RECEIVED']);

@Component({
  selector: 'app-gr-create',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpButtonComponent,
    EpFormFieldComponent,
    EpBreadcrumbComponent,
    EpCardComponent,
    EpAmountComponent,
    EpSkeletonComponent
  ],
  templateUrl: './gr-create.html',
  styleUrls: ['./gr-create.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GrCreateComponent implements OnInit {
  private readonly grService = inject(GoodsReceiptService);
  private readonly poService = inject(PurchaseOrderService);
  private readonly warehouseService = inject(WarehouseService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly isLoading = signal(true);
  readonly submitted = signal(false);
  readonly pos = signal<PurchaseOrder[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly selectedPo = signal<PurchaseOrder | null>(null);

  readonly totalReceivedAmount = computed(() => {
    let total = 0;
    this.items.controls.forEach((ctrl) => {
      const qty = parseFloat(ctrl.get('receivedQuantity')?.value || '0');
      const price = parseFloat(ctrl.get('unitPrice')?.value || '0');
      if (!isNaN(qty) && !isNaN(price)) total += qty * price;
    });
    return total.toFixed(4);
  });

  readonly form: FormGroup = this.fb.group({
    poId: ['', Validators.required],
    warehouseId: ['', Validators.required],
    notes: [''],
    items: this.fb.array([])
  });

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  ngOnInit(): void {
    forkJoin({
      pos: this.poService.list({ page: 1, size: 100, sort: 'createdAt,desc' }),
      warehouses: this.warehouseService.list()
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: ({ pos, warehouses }) => {
          const receivable = (pos.data ?? []).filter((po) => RECEIVABLE_PO_STATUSES.has(po.status));
          this.pos.set(receivable);
          this.warehouses.set(warehouses.data ?? []);
        }
      });
  }

  onPoSelected(event: Event): void {
    const poId = (event.target as HTMLSelectElement).value;
    if (!poId) {
      this.selectedPo.set(null);
      this.items.clear();
      return;
    }

    const po = this.pos().find((p) => p.id === poId);
    if (po) {
      this.selectedPo.set(po);
      this.items.clear();
      po.lineItems.forEach((item) => {
        this.items.push(this.fb.group({
          poLineItemId: [item.id, Validators.required],
          itemName: [{ value: item.itemName, disabled: true }],
          orderedQty: [{ value: item.quantity.amount, disabled: true }],
          orderedUnit: [{ value: item.quantity.unit, disabled: true }],
          unitPrice: [{ value: item.unitPrice, disabled: true }],
          currency: [{ value: item.currency, disabled: true }],
          receivedQuantity: [0, [Validators.required, Validators.min(0)]],
          notes: ['']
        }));
      });
    }
  }

  fieldError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl || (!ctrl.touched && !this.submitted())) return null;
    if (ctrl.errors?.['required']) return 'inventory.gr.create.validation.required';
    return null;
  }

  getLineItemError(index: number, field: string): string | null {
    const ctrl = this.items.at(index).get(field);
    if (!ctrl || (!ctrl.touched && !this.submitted())) return null;
    if (ctrl.errors?.['required']) return 'inventory.gr.create.validation.required';
    if (ctrl.errors?.['min']) return 'inventory.gr.create.validation.min';
    return null;
  }

  onSubmit(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid || this.submitting()) return;

    const rawItems = this.form.getRawValue().items;
    const itemsToReceive = rawItems
      .filter((i: { receivedQuantity: number }) => i.receivedQuantity > 0)
      .map((i: { poLineItemId: string; receivedQuantity: number }) => ({
        poLineItemId: i.poLineItemId,
        receivedQuantity: i.receivedQuantity.toString(),
        rejectedQuantity: '0',
        rejectionReason: null,
        lotNumber: null
      }));

    if (itemsToReceive.length === 0) {
      this.toast.error('inventory.gr.create.toast.errorNoItems');
      this.submitting.set(false);
      return;
    }

    const request = {
      poId: this.form.getRawValue().poId,
      warehouseId: this.form.getRawValue().warehouseId,
      receivedAt: new Date().toISOString(),
      lineItems: itemsToReceive,
      notes: this.form.getRawValue().notes || null
    };

    this.submitting.set(true);
    this.grService.create(request, crypto.randomUUID())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toast.success('inventory.gr.create.toast.success');
          this.router.navigate(['/inventory/goods-receipts', res.data.id]);
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/inventory/goods-receipts']);
  }
}
