import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { ToastService } from '../../../../core/services/toast.service';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { PurchaseOrderService } from '../../services/purchase-order.service';

type CreatePoForm = FormGroup<{
  prId: FormControl<string>;
  vendorId: FormControl<string>;
  deliveryAddress: FormControl<string>;
  deliveryDeadline: FormControl<string>;
  paymentTerms: FormControl<string>;
  notes: FormControl<string>;
}>;

@Component({
  selector: 'ep-po-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent
  ],
  templateUrl: './po-create.component.html',
  styleUrl: './po-create.component.scss'
})
export class PoCreateComponent implements OnInit {
  private readonly purchaseOrderService = inject(PurchaseOrderService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSubmitting = signal(false);

  readonly form: CreatePoForm = new FormGroup({
    prId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    vendorId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    deliveryAddress: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8)] }),
    deliveryDeadline: new FormControl('', { nonNullable: true }),
    paymentTerms: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true })
  });

  ngOnInit(): void {
    this.form.patchValue({
      prId: this.route.snapshot.queryParamMap.get('prId') ?? '',
      vendorId: this.route.snapshot.queryParamMap.get('vendorId') ?? ''
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.purchaseOrderService
      .create({
        prId: raw.prId.trim(),
        vendorId: raw.vendorId.trim(),
        deliveryAddress: raw.deliveryAddress.trim(),
        deliveryDeadline: raw.deliveryDeadline || null,
        paymentTerms: raw.paymentTerms.trim() || null,
        notes: raw.notes.trim() || null
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toastService.successKey('finance.po.create.toast.success');
          this.router.navigate(['/finance', 'purchase-orders', res.data.id]);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/finance', 'purchase-orders']);
  }

  fieldError(controlName: keyof CreatePoForm['controls']): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'finance.po.create.validation.required';
    }
    if (control.hasError('minlength')) {
      return 'finance.po.create.validation.minLength';
    }
    return 'finance.po.create.validation.invalid';
  }
}
