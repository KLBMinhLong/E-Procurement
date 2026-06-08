import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { GoodsReceiptService } from '../../services/goods-receipt.service';
import { PurchaseOrderService } from '../../../finance/services/purchase-order.service';
import { PurchaseOrder } from '../../../finance/models/purchase-order.model';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-gr-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './gr-create.html',
  styleUrls: ['./gr-create.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GrCreate {
  private readonly grService = inject(GoodsReceiptService);
  private readonly poService = inject(PurchaseOrderService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly loadingPos = signal(false);
  readonly pos = signal<PurchaseOrder[]>([]);
  readonly selectedPo = signal<PurchaseOrder | null>(null);

  readonly form: FormGroup = this.fb.group({
    poId: ['', Validators.required],
    warehouse: ['', Validators.required],
    notes: [''],
    items: this.fb.array([])
  });

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  ngOnInit(): void {
    this.loadPurchaseOrders();
  }

  private loadPurchaseOrders(): void {
    this.loadingPos.set(true);
    // Ideally we would search POs with status APPROVED, SENT_TO_VENDOR or PARTIALLY_RECEIVED
    this.poService.list({ page: 0, size: 50, sort: 'createdAt,desc', status: 'SENT_TO_VENDOR' })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingPos.set(false))
      )
      .subscribe({
        next: (res) => {
          this.pos.set(res.data);
        },
        error: (err) => {
          console.error('Failed to load POs', err);
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

    const po = this.pos().find(p => p.id === poId);
    if (po) {
      this.selectedPo.set(po);
      this.items.clear();
      po.lineItems.forEach(item => {
        this.items.push(this.fb.group({
          poLineItemId: [item.id, Validators.required],
          itemName: [{ value: item.itemName, disabled: true }],
          orderedQty: [{ value: item.quantity.amount, disabled: true }],
          receivedQuantity: [0, [Validators.required, Validators.min(0)]],
          notes: ['']
        }));
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    
    // Only include items where received quantity > 0
    const rawItems = this.form.getRawValue().items;
    const itemsToReceive = rawItems.filter((i: any) => i.receivedQuantity > 0).map((i: any) => ({
      poLineItemId: i.poLineItemId,
      receivedQuantity: i.receivedQuantity.toString(),
      rejectedQuantity: "0",
      rejectionReason: null,
      lotNumber: null
    }));

    if (itemsToReceive.length === 0) {
      this.toast.error('inventory.gr.create.errorNoItems');
      this.submitting.set(false);
      return;
    }

    const request: any = {
      poId: this.form.getRawValue().poId,
      warehouseId: this.form.getRawValue().warehouse,
      receivedAt: new Date().toISOString(),
      lineItems: itemsToReceive,
      notes: this.form.getRawValue().notes || null
    };

    const idempotencyKey = crypto.randomUUID();

    this.grService.create(request, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toast.success('inventory.gr.create.success');
          this.router.navigate(['/inventory/goods-receipts', res.data.id]);
        },
        error: (err) => {
          console.error('Failed to create GR', err);
          this.toast.error('inventory.gr.create.failed');
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/inventory/goods-receipts']);
  }
}
