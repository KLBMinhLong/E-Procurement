import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { ToastService } from '../../../../core/services/toast.service';
import { CreateVendorRequest, VendorContact } from '../../models/vendor.model';
import { VendorService } from '../../services/vendor.service';

@Component({
  selector: 'ep-vendor-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
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

  readonly isSaving = signal(false);

  name = '';
  taxCode = '';
  email = '';
  phone = '';
  address = '';
  categoriesRaw = '';

  contacts: Partial<VendorContact>[] = [
    { name: '', email: '', phone: '', position: '', isPrimary: true }
  ];

  addContact(): void {
    this.contacts = [
      ...this.contacts,
      { name: '', email: '', phone: '', position: '', isPrimary: false }
    ];
  }

  removeContact(index: number): void {
    this.contacts = this.contacts.filter((_, i) => i !== index);
  }

  get isValid(): boolean {
    return this.name.trim().length > 0;
  }

  save(): void {
    if (!this.isValid || this.isSaving()) return;

    const request: CreateVendorRequest = {
      name: this.name.trim(),
      taxCode: this.taxCode.trim() || null,
      email: this.email.trim() || null,
      phone: this.phone.trim() || null,
      address: this.address.trim() || null,
      categories: this.categoriesRaw
        .split(',')
        .map((c) => c.trim())
        .filter(Boolean),
      contacts: this.contacts
        .filter((c) => c.name?.trim())
        .map((c) => ({
          id: '',
          name: c.name!.trim(),
          email: c.email?.trim() || null,
          phone: c.phone?.trim() || null,
          position: c.position?.trim() || null,
          isPrimary: c.isPrimary ?? false
        }))
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
}
