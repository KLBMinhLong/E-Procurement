import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { ToastService } from '../../../../core/services/toast.service';
import { CreateVendorRequest } from '../../models/vendor.model';
import { VendorService } from '../../services/vendor.service';

@Component({
  selector: 'ep-vendor-create',
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
  templateUrl: './vendor-create.component.html',
  styleUrl: './vendor-create.component.scss'
})
export class VendorCreateComponent {
  private readonly vendorService = inject(VendorService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  readonly isSaving = signal(false);
  readonly submitted = signal(false);

  readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(300)]],
    taxCode: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    street: [''],
    district: [''],
    city: [''],
    country: ['Vietnam'],
    categoriesRaw: ['', Validators.required],
    notes: [''],
    contacts: this.fb.array([this.createContactGroup(true)])
  });

  get contacts(): FormArray {
    return this.form.get('contacts') as FormArray;
  }

  addContact(): void {
    this.contacts.push(this.createContactGroup(false));
  }

  removeContact(index: number): void {
    if (this.contacts.length <= 1) return;
    this.contacts.removeAt(index);
  }

  fieldError(controlName: string): string | null {
    const control = this.form.get(controlName);
    if (!control || (!control.touched && !this.submitted())) return null;
    if (control.hasError('required')) return 'vendor.create.validation.required';
    if (control.hasError('email')) return 'vendor.create.validation.email';
    if (control.hasError('maxlength')) return 'vendor.create.validation.maxLength';
    return null;
  }

  contactFieldError(index: number, field: string): string | null {
    const control = this.contacts.at(index).get(field);
    if (!control || (!control.touched && !this.submitted())) return null;
    if (control.hasError('required')) return 'vendor.create.validation.required';
    if (control.hasError('email')) return 'vendor.create.validation.email';
    return null;
  }

  save(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid || this.isSaving()) return;

    const raw = this.form.getRawValue();
    const categories = raw.categoriesRaw
      .split(',')
      .map((c: string) => c.trim())
      .filter(Boolean);

    if (!categories.length) {
      this.toastService.error('vendor.create.validation.categoriesRequired');
      return;
    }

    const contacts = raw.contacts
      .filter((c: { name: string }) => c.name?.trim())
      .map((c: { name: string; role: string; email: string; phone: string; isPrimary: boolean }) => ({
        name: c.name.trim(),
        role: c.role?.trim() || null,
        email: c.email.trim(),
        phone: c.phone.trim(),
        isPrimary: c.isPrimary ?? false
      }));

    const request: CreateVendorRequest = {
      name: raw.name.trim(),
      taxCode: raw.taxCode.trim(),
      email: raw.email.trim(),
      phone: raw.phone.trim(),
      address: {
        street: raw.street?.trim() || null,
        district: raw.district?.trim() || null,
        city: raw.city?.trim() || null,
        country: raw.country?.trim() || null
      },
      categories,
      contacts: contacts.length ? contacts : undefined,
      notes: raw.notes?.trim() || null
    };

    this.isSaving.set(true);
    this.vendorService.create(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toastService.success('vendor.create.toast.success');
          this.router.navigate(['/vendors', res.data.id]);
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/vendors']);
  }

  private createContactGroup(isPrimary: boolean): FormGroup {
    return this.fb.group({
      name: [''],
      role: [''],
      email: [''],
      phone: [''],
      isPrimary: [isPrimary]
    });
  }
}
