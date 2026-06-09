import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { ToastService } from '../../../../core/services/toast.service';
import { PurchaseRequestService } from '../../../procurement/services/purchase-request.service';
import { PurchaseRequestSummary } from '../../../procurement/models/purchase-request.model';
import { CreateRfqRequest, VendorSummary } from '../../models/vendor.model';
import { RfqService } from '../../services/rfq.service';
import { VendorService } from '../../services/vendor.service';

@Component({
  selector: 'ep-rfq-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpSkeletonComponent
  ],
  templateUrl: './rfq-create.component.html',
  styleUrl: './rfq-create.component.scss'
})
export class RfqCreateComponent implements OnInit {
  private readonly rfqService = inject(RfqService);
  private readonly vendorService = inject(VendorService);
  private readonly prService = inject(PurchaseRequestService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly approvedPrs = signal<PurchaseRequestSummary[]>([]);
  readonly avlVendors = signal<VendorSummary[]>([]);
  readonly selectedVendorIds = signal<Set<string>>(new Set());

  readonly form: FormGroup = this.fb.group({
    prId: ['', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(300)]],
    submissionDeadline: ['', Validators.required],
    requirements: ['']
  });

  readonly selectedVendorCount = computed(() => this.selectedVendorIds().size);
  readonly canSubmit = computed(() => this.selectedVendorCount() >= 2);

  ngOnInit(): void {
    forkJoin({
      prs: this.prService.list({
        page: 1,
        size: 100,
        sort: 'createdAt,desc',
        status: 'APPROVED'
      }),
      vendors: this.vendorService.list({
        page: 1,
        size: 100,
        sort: 'name,asc',
        status: 'APPROVED',
        onAvlOnly: true
      })
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: ({ prs, vendors }) => {
          this.approvedPrs.set(prs.data ?? []);
          this.avlVendors.set(vendors.data ?? []);
        }
      });
  }

  toggleVendor(vendorId: string): void {
    this.selectedVendorIds.update((current) => {
      const next = new Set(current);
      if (next.has(vendorId)) {
        next.delete(vendorId);
      } else {
        next.add(vendorId);
      }
      return next;
    });
  }

  isVendorSelected(vendorId: string): boolean {
    return this.selectedVendorIds().has(vendorId);
  }

  fieldError(controlName: string): string | null {
    const control = this.form.get(controlName);
    if (!control || (!control.touched && !this.submitted())) return null;
    if (control.hasError('required')) return 'rfq.create.validation.required';
    if (control.hasError('maxlength')) return 'rfq.create.validation.maxLength';
    return null;
  }

  save(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid || !this.canSubmit() || this.isSaving()) {
      if (!this.canSubmit()) {
        this.toastService.error('rfq.create.validation.vendorsMin');
      }
      return;
    }

    const raw = this.form.getRawValue();
    const deadline = new Date(raw.submissionDeadline);
    if (Number.isNaN(deadline.getTime()) || deadline <= new Date()) {
      this.toastService.error('rfq.create.validation.deadlineFuture');
      return;
    }

    const request: CreateRfqRequest = {
      prId: raw.prId,
      title: raw.title.trim(),
      submissionDeadline: deadline.toISOString(),
      invitedVendorIds: Array.from(this.selectedVendorIds()),
      requirements: raw.requirements?.trim() || null
    };

    this.isSaving.set(true);
    this.rfqService.create(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toastService.success('rfq.create.toast.success');
          this.router.navigate(['/vendors/rfq', res.data.id]);
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/vendors/rfq']);
  }
}
