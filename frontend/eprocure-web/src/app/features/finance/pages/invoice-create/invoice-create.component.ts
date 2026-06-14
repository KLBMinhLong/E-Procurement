import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { ToastService } from '../../../../core/services/toast.service';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { PurchaseOrder, PurchaseOrderStatus } from '../../models/purchase-order.model';
import { InvoiceService } from '../../services/invoice.service';
import { PurchaseOrderService } from '../../services/purchase-order.service';

type InvoiceLineForm = FormGroup<{
  poLineItemId: FormControl<string>;
  description: FormControl<string>;
  quantity: FormControl<string>;
  unitPrice: FormControl<string>;
  taxRate: FormControl<string>;
}>;

type CreateInvoiceForm = FormGroup<{
  invoiceNumber: FormControl<string>;
  poId: FormControl<string>;
  invoiceDate: FormControl<string>;
  dueDate: FormControl<string>;
  lineItems: FormArray<InvoiceLineForm>;
}>;

const PO_STATUS_TONE: Record<string, EpBadgeTone> = {
  SENT_TO_VENDOR: 'info',
  PARTIALLY_RECEIVED: 'warning',
  FULLY_RECEIVED: 'success',
  INVOICED: 'info',
  PAID: 'success'
};

const INVOICEABLE_STATUSES = new Set<PurchaseOrderStatus>([
  'SENT_TO_VENDOR',
  'PARTIALLY_RECEIVED',
  'FULLY_RECEIVED',
  'INVOICED'
]);

@Component({
  selector: 'ep-invoice-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpAmountComponent,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpSkeletonComponent
  ],
  templateUrl: './invoice-create.component.html',
  styleUrl: './invoice-create.component.scss'
})
export class InvoiceCreateComponent implements OnInit {
  private readonly invoiceService = inject(InvoiceService);
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly purchaseOrders = signal<PurchaseOrder[]>([]);
  readonly selectedPo = signal<PurchaseOrder | null>(null);
  readonly isLoadingPo = signal(false);
  readonly isSubmitting = signal(false);
  readonly lineRevision = signal(0);
  readonly poStatusTone = PO_STATUS_TONE;

  readonly form: CreateInvoiceForm = new FormGroup({
    invoiceNumber: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)]
    }),
    poId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    invoiceDate: new FormControl(this.today(), { nonNullable: true, validators: [Validators.required] }),
    dueDate: new FormControl(this.today(), { nonNullable: true, validators: [Validators.required] }),
    lineItems: new FormArray<InvoiceLineForm>([])
  });

  readonly invoiceablePurchaseOrders = computed(() =>
    this.purchaseOrders().filter((po) => INVOICEABLE_STATUSES.has(po.status))
  );
  readonly hasPoSelection = computed(() => Boolean(this.selectedPo()));
  readonly selectedVendorName = computed(() => this.selectedPo()?.vendor.name ?? '--');
  readonly selectedVendorId = computed(() => this.selectedPo()?.vendor.id ?? '--');
  readonly selectedPoNumber = computed(() => this.selectedPo()?.poNumber ?? '--');
  readonly lineCount = computed(() => this.form.controls.lineItems.length);
  readonly estimatedSubtotal = computed(() => {
    this.lineRevision();
    return this.form.controls.lineItems.controls
      .reduce((sum, group) => sum + this.lineSubtotal(group), 0)
      .toFixed(4);
  });
  readonly estimatedTax = computed(() => {
    this.lineRevision();
    return this.form.controls.lineItems.controls
      .reduce((sum, group) => sum + this.lineTax(group), 0)
      .toFixed(4);
  });
  readonly estimatedTotal = computed(() => {
    this.lineRevision();
    return this.form.controls.lineItems.controls
      .reduce((sum, group) => sum + this.lineSubtotal(group) + this.lineTax(group), 0)
      .toFixed(4);
  });

  get lineItems(): FormArray<InvoiceLineForm> {
    return this.form.controls.lineItems;
  }

  ngOnInit(): void {
    this.loadPurchaseOrders();
    this.form.controls.poId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((poId) => this.selectPo(poId, false));
    this.lineItems.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.lineRevision.update((value) => value + 1));

    const preselectedPoId = this.route.snapshot.queryParamMap.get('poId');
    if (preselectedPoId) {
      this.form.controls.poId.setValue(preselectedPoId);
      this.loadPurchaseOrder(preselectedPoId);
    }
  }

  loadPurchaseOrders(): void {
    this.isLoadingPo.set(true);
    this.purchaseOrderService
      .list({ page: 1, size: 100, sort: 'createdAt,desc' })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoadingPo.set(false))
      )
      .subscribe({
        next: (res) => {
          this.purchaseOrders.set(res.data ?? []);
          const poId = this.form.controls.poId.value;
          if (poId && !this.selectedPo()) {
            this.selectPo(poId, false);
          }
        },
        error: () => this.purchaseOrders.set([])
      });
  }

  onPoChange(poId: string): void {
    this.form.controls.poId.setValue(poId);
  }

  restorePoLines(): void {
    const po = this.selectedPo();
    if (!po) {
      return;
    }
    this.hydrateLines(po);
  }

  removeLine(index: number): void {
    if (this.lineItems.length <= 1) {
      return;
    }
    this.lineItems.removeAt(index);
    this.lineRevision.update((value) => value + 1);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (!this.selectedPo()) {
      this.form.controls.poId.setErrors({ required: true });
    }
    if (!this.isDateRangeValid()) {
      this.form.controls.dueDate.setErrors({ range: true });
    }
    if (this.form.invalid || this.lineItems.length === 0 || this.isSubmitting()) {
      return;
    }
    const po = this.selectedPo();
    if (!po) {
      return;
    }
    const raw = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.invoiceService
      .create({
        invoiceNumber: raw.invoiceNumber.trim(),
        vendorId: po.vendor.id,
        poId: po.id,
        invoiceDate: raw.invoiceDate,
        dueDate: raw.dueDate,
        lineItems: raw.lineItems.map((line) => ({
          poLineItemId: line.poLineItemId,
          description: line.description.trim(),
          quantity: this.normalizeDecimal(line.quantity),
          unitPrice: this.normalizeDecimal(line.unitPrice),
          taxRate: line.taxRate.trim() || null
        })),
        attachmentIds: null
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toastService.successKey('finance.invoice.create.toast.success');
          this.router.navigate(['/finance', 'invoices', res.data.id]);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/finance', 'invoices']);
  }

  fieldError(controlName: 'invoiceNumber' | 'poId' | 'invoiceDate' | 'dueDate'): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.invoice.create.validation.required';
    }
    if (control.hasError('maxlength')) {
      return 'finance.invoice.create.validation.maxLength';
    }
    if (control.hasError('range')) {
      return 'finance.invoice.create.validation.dueDateRange';
    }
    return 'finance.invoice.create.validation.invalid';
  }

  lineError(index: number, controlName: keyof InvoiceLineForm['controls']): string | null {
    const control = this.lineItems.at(index).controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.invoice.create.validation.required';
    }
    if (control.hasError('min')) {
      return 'finance.invoice.create.validation.positive';
    }
    if (control.hasError('maxlength')) {
      return 'finance.invoice.create.validation.maxLength';
    }
    return 'finance.invoice.create.validation.invalid';
  }

  lineEstimatedTotal(index: number): string {
    const group = this.lineItems.at(index);
    return (this.lineSubtotal(group) + this.lineTax(group)).toFixed(4);
  }

  quantityLabel(value: string | number | null | undefined): string {
    return this.trimDecimal(value);
  }

  private selectPo(poId: string, forceHydrate: boolean): void {
    const po = this.purchaseOrders().find((item) => item.id === poId) ?? null;
    if (po) {
      this.selectedPo.set(po);
      if (forceHydrate || this.lineItems.length === 0) {
        this.hydrateLines(po);
      }
      return;
    }
    if (!poId) {
      this.selectedPo.set(null);
      this.lineItems.clear();
      return;
    }
    this.loadPurchaseOrder(poId);
  }

  private loadPurchaseOrder(poId: string): void {
    this.purchaseOrderService
      .getById(poId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.purchaseOrders.update((items) => items.some((item) => item.id === res.data.id) ? items : [res.data, ...items]);
          this.selectedPo.set(res.data);
          this.hydrateLines(res.data);
        }
      });
  }

  private hydrateLines(po: PurchaseOrder): void {
    this.lineItems.clear();
    po.lineItems.forEach((line) => {
      this.lineItems.push(new FormGroup({
        poLineItemId: new FormControl(line.id, { nonNullable: true, validators: [Validators.required] }),
        description: new FormControl(line.itemName, {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(500)]
        }),
        quantity: new FormControl(this.normalizeDecimal(line.quantity.amount), {
          nonNullable: true,
          validators: [Validators.required, Validators.min(0.0001)]
        }),
        unitPrice: new FormControl(this.normalizeDecimal(line.unitPrice), {
          nonNullable: true,
          validators: [Validators.required, Validators.min(0)]
        }),
        taxRate: new FormControl('0', { nonNullable: true, validators: [Validators.min(0)] })
      }));
    });
    this.lineRevision.update((value) => value + 1);
  }

  private isDateRangeValid(): boolean {
    const invoiceDate = this.form.controls.invoiceDate.value;
    const dueDate = this.form.controls.dueDate.value;
    return !invoiceDate || !dueDate || dueDate >= invoiceDate;
  }

  private lineSubtotal(group: InvoiceLineForm): number {
    const raw = group.getRawValue();
    return Number(raw.quantity || 0) * Number(raw.unitPrice || 0);
  }

  private lineTax(group: InvoiceLineForm): number {
    const taxRate = Number(group.controls.taxRate.value || 0);
    return this.lineSubtotal(group) * taxRate;
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private normalizeDecimal(value: string | number): string {
    const text = String(value).trim();
    return text === '' ? '0' : text;
  }

  private trimDecimal(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '--';
    }
    const trimmed = String(value).replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '');
    return trimmed === '' ? '0' : trimmed;
  }
}
