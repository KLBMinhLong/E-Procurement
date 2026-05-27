import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiErrorResponse } from '../../../core/models/api-response.model';
import { EpButtonComponent } from '../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../shared/components/ep-icon/ep-icon.component';
import { EpLangSwitcherComponent } from '../../../shared/components/ep-lang-switcher/ep-lang-switcher.component';

@Component({
  selector: 'ep-login',
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
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly motionAttr = 'data-motion';
  private readonly motionValue = 'full';
  private motionOverrideApplied = false;

  readonly isSubmitting = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly requiresTwoFactor = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required]]
  });

  readonly otpForm = this.formBuilder.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^(?:\d{6}|[A-Za-z2-9]{4}-[A-Za-z2-9]{4})$/)]]
  });


  ngOnInit(): void {
    // Check if redirected here because account was locked (423 Locked)
    const reason = this.route.snapshot.queryParamMap.get('reason');
    if (reason === 'locked') {
      this.errorKey.set('auth.login.accountLocked');
      return;
    }

    // Check Google OAuth redirect errors
    const error = this.route.snapshot.queryParamMap.get('error');
    if (error) {
      if (error === 'IAM_030') {
        this.errorKey.set('auth.login.googleUserNotFound');
      } else {
        this.errorKey.set('auth.login.googleFailed');
      }
      return;
    }

    // Check Google OAuth 2FA requirement
    const requires2Fa = this.route.snapshot.queryParamMap.get('requiresTwoFactor');
    if (requires2Fa === 'true') {
      this.requiresTwoFactor.set(true);
      this.errorKey.set(null);
      this.otpForm.reset();
    }
  }

  submit(): void {
    if (this.requiresTwoFactor()) {
      this.verifyOtp();
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorKey.set('auth.login.validation');
      return;
    }

    this.isSubmitting.set(true);
    this.errorKey.set(null);
    this.setMotionOverride(true);

    this.authService.login(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSubmitting.set(false);
          this.setMotionOverride(false);
        })
      )
      .subscribe({
        next: (response) => {
          if (response.data.requiresTwoFactor) {
            this.requiresTwoFactor.set(true);
            this.errorKey.set(null);
            this.otpForm.reset();
          } else {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (err: HttpErrorResponse) => {
          const body = err.error as ApiErrorResponse | undefined;
          const code = body?.code;
          if (err.status === 423 || code === 'IAM_002') {
            this.errorKey.set('auth.login.accountLocked');
          } else {
            this.errorKey.set('auth.login.failed');
          }
        }
      });
  }

  verifyOtp(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      this.errorKey.set('profile.security.2fa.otpInvalid');
      return;
    }

    this.isSubmitting.set(true);
    this.errorKey.set(null);
    this.setMotionOverride(true);

    this.authService.verifyTwoFactor(this.otpForm.getRawValue().code)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSubmitting.set(false);
          this.setMotionOverride(false);
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err: HttpErrorResponse) => {
          this.errorKey.set('auth.login.twoFactorInvalid');
        }
      });
  }

  cancelTwoFactor(): void {
    this.requiresTwoFactor.set(false);
    this.errorKey.set(null);
    this.otpForm.reset();
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((visible) => !visible);
  }

  loginWithGoogle(): void {
    window.location.assign(this.authService.googleLoginUrl());
  }

  private setMotionOverride(enabled: boolean): void {
    const root = this.document?.documentElement;
    if (!root) {
      return;
    }

    if (enabled) {
      if (root.getAttribute(this.motionAttr) !== this.motionValue) {
        root.setAttribute(this.motionAttr, this.motionValue);
        this.motionOverrideApplied = true;
      }
      return;
    }

    if (this.motionOverrideApplied && root.getAttribute(this.motionAttr) === this.motionValue) {
      root.removeAttribute(this.motionAttr);
    }
    this.motionOverrideApplied = false;
  }
}
