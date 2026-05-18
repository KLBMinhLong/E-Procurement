import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
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
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly motionAttr = 'data-motion';
  private readonly motionValue = 'full';
  private motionOverrideApplied = false;

  readonly isSubmitting = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required]]
  });

  submit(): void {
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
        next: () => this.router.navigate(['/dashboard']),
        error: () => this.errorKey.set('auth.login.failed')
      });
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
