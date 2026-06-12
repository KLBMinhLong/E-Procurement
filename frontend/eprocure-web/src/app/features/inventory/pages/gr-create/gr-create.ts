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
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';

import { GoodsReceiptService } from '../../services/goods-receipt.service';
import { WarehouseService } from '../../services/warehouse.service';
import { PurchaseOrderService } from '../../../finance/services/purchase-order.service';
import { PoLineItem, PurchaseOrder } from '../../../finance/models/purchase-order.model';
import { Warehouse } from '../../models/stock.model';
import { ToastService } from '../../../../core/services/toast.service';

import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';

const RECEIVABLE_PO_STATUSES = new Set(['SENT_TO_VENDOR', 'PARTIALLY_RECEIVED']);

function receivedRejectedWithinOrdered(control: AbstractControl): ValidationErrors | null {
  const ordered = Number(control.get('orderedQty')?.value || 0);
  const received = Number(control.get('receivedQuantity')?.value || 0);
  const rejected = Number(control.get('rejectedQuantity')?.value || 0);

  if (ordered > 0 && received + rejected > ordered) {
    return { overQuantity: true };
  }

  return null;
}

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
  readonly isLoadingPoLines = signal(false);

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
    this.items.clear();

    if (!poId) {
      this.selectedPo.set(null);
      return;
    }

    const po = this.pos().find((p) => p.id === poId);
    if (!po) {
      this.selectedPo.set(null);
      return;
    }

    this.selectedPo.set(po);
    if (po.lineItems?.length) {
      this.prefillItems(po.lineItems);
      return;
    }

    this.loadPoDetail(poId);
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
    if (
      (field === 'receivedQuantity' || field === 'rejectedQuantity')
      && this.items.at(index).errors?.['overQuantity']
      && (ctrl.touched || this.submitted())
    ) {
      return 'inventory.gr.create.validation.overQuantity';
    }
    if (field === 'rejectionReason' && this.requiresRejectionReason(index)) {
      return 'inventory.gr.create.validation.rejectionReasonRequired';
    }
    return null;
  }

  requiresRejectionReason(index: number): boolean {
    const line = this.items.at(index);
    const rejectedQuantity = Number(line.get('rejectedQuantity')?.value || 0);
    const reason = String(line.get('rejectionReason')?.value || '').trim();
    return rejectedQuantity > 0 && reason.length === 0 && (line.touched || this.submitted());
  }

  onSubmit(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid || this.submitting()) return;

    const rawItems = this.form.getRawValue().items;
    const itemsToReceive = rawItems
      .filter((i: { receivedQuantity: number; rejectedQuantity: number }) =>
        Number(i.receivedQuantity || 0) > 0 || Number(i.rejectedQuantity || 0) > 0
      )
      .map((i: {
        poLineItemId: string;
        receivedQuantity: number;
        rejectedQuantity: number;
        rejectionReason: string;
        lotNumber: string;
      }) => ({
        poLineItemId: i.poLineItemId,
        receivedQuantity: String(i.receivedQuantity || 0),
        rejectedQuantity: String(i.rejectedQuantity || 0),
        rejectionReason: i.rejectionReason?.trim() || null,
        lotNumber: i.lotNumber?.trim() || null
      }));

    if (itemsToReceive.length === 0) {
      this.toast.error('inventory.gr.create.toast.errorNoItems');
      this.submitting.set(false);
      return;
    }

    if (rawItems.some((item: { rejectedQuantity: number; rejectionReason: string }) =>
      Number(item.rejectedQuantity || 0) > 0 && !String(item.rejectionReason || '').trim()
    )) {
      this.toast.error('inventory.gr.create.validation.rejectionReasonRequired');
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

  private loadPoDetail(poId: string): void {
    this.isLoadingPoLines.set(true);
    this.poService.getById(poId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingPoLines.set(false))
      )
      .subscribe({
        next: (res) => {
          const po = res.data;
          this.selectedPo.set(po);
          this.pos.update((current) => current.map((candidate) => candidate.id === po.id ? po : candidate));
          this.prefillItems(po.lineItems ?? []);
        },
        error: () => this.toast.error('inventory.gr.create.toast.loadPoFailed')
      });
  }

  private prefillItems(lineItems: PoLineItem[]): void {
    this.items.clear();
    lineItems.forEach((item) => {
      this.items.push(this.fb.group({
        poLineItemId: [item.id, Validators.required],
        itemName: [{ value: item.itemName, disabled: true }],
        orderedQty: [{ value: item.quantity.amount, disabled: true }],
        orderedUnit: [{ value: item.quantity.unit, disabled: true }],
        unitPrice: [{ value: item.unitPrice, disabled: true }],
        currency: [{ value: item.currency, disabled: true }],
        receivedQuantity: [0, [Validators.required, Validators.min(0)]],
        rejectedQuantity: [0, [Validators.required, Validators.min(0)]],
        rejectionReason: [''],
        lotNumber: ['']
      }, { validators: receivedRejectedWithinOrdered }));
    });
  }
}
