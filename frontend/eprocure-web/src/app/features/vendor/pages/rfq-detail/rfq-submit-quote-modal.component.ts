import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { Money } from '../../../procurement/models/purchase-request.model';
import { RfqDetail, RfqInvitation, RfqLineItem, SubmitQuoteRequest } from '../../models/vendor.model';

type SubmitQuoteLineControls = {
  rfqLineItemId: FormControl<string>;
  itemName: FormControl<string>;
  quantity: FormControl<string>;
  unit: FormControl<string>;
  unitPrice: FormControl<string | number>;
  deliveryDays: FormControl<string | number>;
  warranty: FormControl<string>;
};

type SubmitQuoteLineForm = FormGroup<SubmitQuoteLineControls>;

type SubmitQuoteForm = FormGroup<{
  vendorId: FormControl<string>;
  currency: FormControl<string>;
  validUntil: FormControl<string>;
  paymentTerms: FormControl<string>;
  notes: FormControl<string>;
  lineItems: FormArray<SubmitQuoteLineForm>;
}>;

@Component({
  selector: 'ep-rfq-submit-quote-modal',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpAmountComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpModalComponent
  ],
  templateUrl: './rfq-submit-quote-modal.component.html',
  styleUrl: './rfq-submit-quote-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RfqSubmitQuoteModalComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly open = input(false);
  readonly rfq = input<RfqDetail | null>(null);
  readonly loading = input(false);
  readonly close = output<void>();
  readonly quoteSubmit = output<SubmitQuoteRequest>();

  readonly formRevision = signal(0);
  readonly today = this.formatDateInput(new Date());
  readonly form: SubmitQuoteForm = new FormGroup({
    vendorId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currency: new FormControl('VND', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]
    }),
    validUntil: new FormControl(this.today, { nonNullable: true, validators: [Validators.required] }),
    paymentTerms: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    notes: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    lineItems: new FormArray<SubmitQuoteLineForm>([])
  });

  readonly pendingInvitations = computed(() => {
    const data = this.rfq();
    return data?.invitations.filter((invitation) => !invitation.hasSubmitted) ?? [];
  });
  readonly selectedVendorName = computed(() => {
    this.formRevision();
    return this.pendingInvitations().find((invitation) => invitation.vendor.id === this.form.controls.vendorId.value)
      ?.vendor.name ?? '--';
  });
  readonly quoteCurrency = computed(() => {
    this.formRevision();
    return this.form.controls.currency.value || 'VND';
  });
  readonly estimatedTotal = computed(() => {
    this.formRevision();
    return this.lineItems.controls
      .reduce((sum, group) => sum + this.lineTotal(group), 0)
      .toFixed(4);
  });
  readonly completedLineCount = computed(() => {
    this.formRevision();
    return this.lineItems.controls.filter((group) => Number(group.controls.unitPrice.value) > 0).length;
  });

  private readonly hydrateEffect = effect(() => {
    const data = this.rfq();
    if (!this.open() || !data) {
      return;
    }
    this.hydrateForm(data);
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.formRevision.update((value) => value + 1));
  }

  get lineItems(): FormArray<SubmitQuoteLineForm> {
    return this.form.controls.lineItems;
  }

  closeModal(): void {
    if (!this.loading()) {
      this.close.emit();
    }
  }

  submitForm(): void {
    this.form.markAllAsTouched();
    this.validateDate();
    if (this.form.invalid || this.lineItems.length === 0 || this.loading()) {
      return;
    }

    const raw = this.form.getRawValue();
    this.quoteSubmit.emit({
      vendorId: raw.vendorId,
      currency: raw.currency.trim().toUpperCase(),
      validUntil: raw.validUntil,
      lineItems: raw.lineItems.map((line) => ({
        rfqLineItemId: line.rfqLineItemId,
        unitPrice: this.normalizeDecimal(line.unitPrice),
        deliveryDays: this.normalizeOptionalInteger(line.deliveryDays),
        warranty: line.warranty.trim() || null
      })),
      paymentTerms: raw.paymentTerms.trim() || null,
      notes: raw.notes.trim() || null
    });
  }

  fieldError(controlName: 'vendorId' | 'currency' | 'validUntil' | 'paymentTerms' | 'notes'): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'rfq.detail.submit.validation.required';
    }
    if (control.hasError('pattern')) {
      return 'rfq.detail.submit.validation.currency';
    }
    if (control.hasError('date')) {
      return 'rfq.detail.submit.validation.validUntil';
    }
    if (control.hasError('maxlength')) {
      return 'rfq.detail.submit.validation.maxLength';
    }
    return 'rfq.detail.submit.validation.invalid';
  }

  lineError(index: number, controlName: 'unitPrice' | 'deliveryDays' | 'warranty'): string | null {
    const control = this.lineItems.at(index).controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'rfq.detail.submit.validation.required';
    }
    if (control.hasError('min')) {
      return 'rfq.detail.submit.validation.positive';
    }
    if (control.hasError('maxlength')) {
      return 'rfq.detail.submit.validation.maxLength';
    }
    return 'rfq.detail.submit.validation.invalid';
  }

  lineTotalAmount(index: number): Money {
    return {
      amount: this.lineTotal(this.lineItems.at(index)).toFixed(4),
      currency: this.quoteCurrency()
    };
  }

  estimatedTotalAmount(): Money {
    return {
      amount: this.estimatedTotal(),
      currency: this.quoteCurrency()
    };
  }

  quantityLabel(line: SubmitQuoteLineForm): string {
    const raw = line.getRawValue();
    return `${this.trimDecimal(raw.quantity)} ${raw.unit}`;
  }

  trackInvitation(_: number, invitation: RfqInvitation): string {
    return invitation.id;
  }

  trackLine(_: number, group: SubmitQuoteLineForm): string {
    return group.controls.rfqLineItemId.value;
  }

  private hydrateForm(data: RfqDetail): void {
    const invitations = this.pendingInvitations();
    this.form.reset({
      vendorId: invitations[0]?.vendor.id ?? '',
      currency: 'VND',
      validUntil: this.today,
      paymentTerms: '',
      notes: ''
    });
    this.lineItems.clear();
    data.lineItems.forEach((item) => this.lineItems.push(this.createLineForm(item)));
    this.formRevision.update((value) => value + 1);
  }

  private createLineForm(item: RfqLineItem): SubmitQuoteLineForm {
    return new FormGroup<SubmitQuoteLineControls>({
      rfqLineItemId: new FormControl(item.id, { nonNullable: true, validators: [Validators.required] }),
      itemName: new FormControl(item.itemName, { nonNullable: true }),
      quantity: new FormControl(item.quantity, { nonNullable: true }),
      unit: new FormControl(item.unit, { nonNullable: true }),
      unitPrice: new FormControl<string | number>('', { nonNullable: true, validators: [Validators.required, Validators.min(0.0001)] }),
      deliveryDays: new FormControl<string | number>('', { nonNullable: true, validators: [Validators.min(0)] }),
      warranty: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] })
    });
  }

  private validateDate(): void {
    const control = this.form.controls.validUntil;
    const validUntil = control.value;
    if (validUntil && validUntil < this.today) {
      control.setErrors({ ...(control.errors ?? {}), date: true });
      return;
    }
    if (control.hasError('date')) {
      const remainingErrors = { ...(control.errors ?? {}) };
      delete remainingErrors['date'];
      control.setErrors(Object.keys(remainingErrors).length ? remainingErrors : null);
    }
  }

  private lineTotal(group: SubmitQuoteLineForm): number {
    const raw = group.getRawValue();
    return Number(raw.quantity || 0) * Number(raw.unitPrice || 0);
  }

  private normalizeDecimal(value: string | number | null | undefined): string {
    const trimmed = String(value ?? '').trim();
    return trimmed === '' ? '0' : trimmed;
  }

  private normalizeOptionalInteger(value: string | number | null | undefined): number | null {
    const trimmed = String(value ?? '').trim();
    return trimmed === '' ? null : Number(trimmed);
  }

  private trimDecimal(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '--';
    }
    const trimmed = String(value).replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '');
    return trimmed === '' ? '0' : trimmed;
  }

  private formatDateInput(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
