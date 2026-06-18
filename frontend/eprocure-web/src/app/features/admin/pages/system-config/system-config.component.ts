import { ChangeDetectionStrategy, Component, inject, OnInit, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators, FormArray } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, catchError, of } from 'rxjs';

import { AdminOperationsService } from '../../services/admin-operations.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  ServiceConfig,
  ServiceConfigSummary,
} from '../../models/admin-operations.model';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';

@Component({
  selector: 'ep-admin-system-config',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpStatCardComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpModalComponent,
    EpFormFieldComponent,
    EpBadgeComponent,
    EpIconComponent,
    EpButtonComponent,
    HasPermissionDirective,
  ],
  templateUrl: './system-config.component.html',
  styleUrl: './system-config.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SystemConfigComponent implements OnInit {
  private readonly opsService = inject(AdminOperationsService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  // State
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  readonly summary = signal<ServiceConfigSummary | null>(null);
  readonly services = signal<ServiceConfig[]>([]);
  readonly selectedService = signal<ServiceConfig | null>(null);
  readonly selectedServiceLoading = signal(false);

  // Modals state
  readonly activeModal = signal<'update' | 'restart' | 'rotateKey' | null>(null);
  readonly modalSubmitting = signal(false);

  // Update Config Form
  readonly updateForm = this.fb.nonNullable.group({
    variables: this.fb.array<{ key: string; value: string; isSensitive: boolean; description: string | null }>([]),
    requiresRestart: [false],
    changeReason: ['', [Validators.required, Validators.minLength(10)]],
    confirmationCode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
  });

  // Restart Form
  readonly restartForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
    confirmationCode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
  });

  // Rotate Key Form
  readonly rotateKeyForm = this.fb.nonNullable.group({
    keySize: [2048 as 2048 | 4096, Validators.required],
    confirmationCode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]]
  });

  constructor() {
    // Generate UUID idempotency keys on demand
  }

  ngOnInit() {
    this.loadConfigs();
  }

  loadConfigs() {
    this.isLoading.set(true);
    this.error.set(null);

    this.opsService.listServiceConfigs()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
        catchError(err => {
          this.error.set(err.error?.message || 'Failed to load configurations');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.services.set(res.data);
          this.summary.set(res.summary);
          if (res.data.length > 0 && !this.selectedService()) {
            this.selectService(res.data[0].serviceName);
          }
        }
      });
  }

  selectService(serviceName: string) {
    this.selectedServiceLoading.set(true);
    this.opsService.getServiceConfig(serviceName)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.selectedServiceLoading.set(false)),
        catchError(err => {
          this.toast.error(err.error?.message || 'Failed to load service configuration');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.selectedService.set(res.data);
        }
      });
  }

  // ── Status helpers ──────────────────────────────────────────────

  statusIcon(status: string): string {
    switch (status?.toUpperCase()) {
      case 'UP':       return 'check-circle';
      case 'DOWN':     return 'x-circle';
      case 'DEGRADED': return 'alert-triangle';
      default:         return 'help-circle';
    }
  }

  statusTone(status: string): string {
    switch (status?.toUpperCase()) {
      case 'UP':       return 'success';
      case 'DOWN':     return 'danger';
      case 'DEGRADED': return 'warning';
      default:         return 'neutral';
    }
  }

  // ── Update Config Flow ──────────────────────────────────────────

  get variablesFormArray() {
    return this.updateForm.get('variables') as FormArray;
  }

  openUpdateModal() {
    const srv = this.selectedService();
    if (!srv) return;

    this.updateForm.reset();
    this.variablesFormArray.clear();

    // Populate variables
    srv.variables.forEach((v: any) => {
      this.variablesFormArray.push(this.fb.group({
        key: [v.key, Validators.required],
        value: [{ value: v.isSensitive ? '********' : v.value, disabled: false }, Validators.required],
        isSensitive: [v.isSensitive],
        description: [v.description]
      }));
    });

    this.activeModal.set('update');
  }

  closeModal() {
    this.activeModal.set(null);
  }

  submitUpdate() {
    if (this.updateForm.invalid) {
      this.updateForm.markAllAsTouched();
      return;
    }

    const srv = this.selectedService();
    if (!srv) return;

    this.modalSubmitting.set(true);
    const formValue = this.updateForm.getRawValue();

    // Filter out unchanged or dummy sensitive values
    const variables = formValue.variables
      .filter((v: any) => v.value !== '********') // Don't send masked dummy values
      .map((v: any) => ({
        key: v.key,
        value: v.value,
        isSensitive: v.isSensitive,
        description: v.description
      }));

    const idempotencyKey = crypto.randomUUID();

    this.opsService.updateServiceConfig(srv.serviceName, {
      variables,
      confirmationCode: formValue.confirmationCode,
      changeReason: formValue.changeReason,
      requiresRestart: formValue.requiresRestart
    }, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.modalSubmitting.set(false)),
        catchError(err => {
          this.toast.error(err.error?.message || 'Failed to update configuration');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.config.toast.updatePending');
          this.closeModal();
          this.selectService(srv.serviceName); // Reload
        }
      });
  }

  // ── Restart Service Flow ────────────────────────────────────────

  openRestartModal() {
    if (!this.selectedService()) return;
    this.restartForm.reset();
    this.activeModal.set('restart');
  }

  submitRestart() {
    if (this.restartForm.invalid) {
      this.restartForm.markAllAsTouched();
      return;
    }

    const srv = this.selectedService();
    if (!srv) return;

    this.modalSubmitting.set(true);
    const formValue = this.restartForm.getRawValue();
    const idempotencyKey = crypto.randomUUID();

    this.opsService.restartService(srv.serviceName, {
      reason: formValue.reason,
      confirmationCode: formValue.confirmationCode
    }, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.modalSubmitting.set(false)),
        catchError(err => {
          this.toast.error(err.error?.message || 'Failed to trigger restart');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.config.toast.restartPending');
          this.closeModal();
          // Status may change shortly
        }
      });
  }

  // ── Rotate Key Flow ─────────────────────────────────────────────

  openRotateKeyModal() {
    this.rotateKeyForm.reset({ keySize: 2048 });
    this.activeModal.set('rotateKey');
  }

  submitRotateKey() {
    if (this.rotateKeyForm.invalid) {
      this.rotateKeyForm.markAllAsTouched();
      return;
    }

    this.modalSubmitting.set(true);
    const formValue = this.rotateKeyForm.getRawValue();
    const idempotencyKey = crypto.randomUUID();

    this.opsService.rotateEncryptionKey({
      keySize: formValue.keySize,
      confirmationCode: formValue.confirmationCode
    }, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.modalSubmitting.set(false)),
        catchError(err => {
          this.toast.error(err.error?.message || 'Failed to rotate key');
          return of(null);
        })
      )
      .subscribe(res => {
        if (res?.success) {
          this.toast.success('adminOps.config.toast.rotatePending');
          this.closeModal();
        }
      });
  }
}
