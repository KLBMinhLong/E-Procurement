import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiErrorResponse } from '../../../core/models/api-response.model';
import { EpButtonComponent } from '../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../shared/components/ep-icon/ep-icon.component';
import { EpLangSwitcherComponent } from '../../../shared/components/ep-lang-switcher/ep-lang-switcher.component';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^])[A-Za-z\d@$!%*?&#^]{8,128}$/;

@Component({
  selector: 'ep-reset-password',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpLangSwitcherComponent
  ],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResetPasswordComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSubmitting = signal(false);
  readonly isCompleted = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly showNewPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly resetToken = signal(this.route.snapshot.queryParamMap.get('token')?.trim() ?? '');
  readonly hasToken = computed(() => this.resetToken().length > 0);
  readonly form = this.formBuilder.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128), Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', [Validators.required, Validators.maxLength(128)]]
  }, {
    validators: this.passwordMatchValidator
  });

  submit(): void {
    if (!this.hasToken()) {
      this.errorKey.set('auth.resetPassword.missingToken');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorKey.set('auth.resetPassword.validation');
      return;
    }

    const value = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.errorKey.set(null);

    this.authService.resetPassword({
      resetToken: this.resetToken(),
      newPassword: value.newPassword,
      confirmPassword: value.confirmPassword
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          this.form.reset();
          this.isCompleted.set(true);
        },
        error: (error: HttpErrorResponse) => this.errorKey.set(this.errorKeyFor(error))
      });
  }

  toggleNewPasswordVisibility(): void {
    this.showNewPassword.update((value) => !value);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update((value) => !value);
  }

  newPasswordErrorKey(): string | null {
    const control = this.form.controls.newPassword;
    if (!control.touched) {
      return null;
    }
    if (control.hasError('required')) {
      return 'auth.resetPassword.required';
    }
    if (control.hasError('minlength') || control.hasError('maxlength') || control.hasError('pattern')) {
      return 'auth.resetPassword.passwordPolicy';
    }
    return null;
  }

  confirmPasswordErrorKey(): string | null {
    const control = this.form.controls.confirmPassword;
    if (!control.touched) {
      return null;
    }
    if (control.hasError('required')) {
      return 'auth.resetPassword.required';
    }
    if (this.form.hasError('passwordMismatch')) {
      return 'auth.resetPassword.passwordMismatch';
    }
    return null;
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    if (!newPassword || !confirmPassword) {
      return null;
    }
    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  private errorKeyFor(error: HttpErrorResponse): string {
    const body = error.error as ApiErrorResponse | undefined;
    if (body?.code === 'IAM_007' || error.status === 401) {
      return 'auth.resetPassword.tokenExpired';
    }
    if (body?.code === 'IAM_008' || error.status === 422) {
      return 'auth.resetPassword.passwordPolicy';
    }
    return 'auth.resetPassword.failed';
  }
}
