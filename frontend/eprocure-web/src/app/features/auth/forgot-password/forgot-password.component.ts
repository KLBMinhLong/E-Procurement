import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { EpButtonComponent } from '../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../shared/components/ep-icon/ep-icon.component';
import { EpLangSwitcherComponent } from '../../../shared/components/ep-lang-switcher/ep-lang-switcher.component';

@Component({
  selector: 'ep-forgot-password',
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
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ForgotPasswordComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSubmitting = signal(false);
  readonly messageKey = signal<string | null>(null);
  readonly errorKey = signal<string | null>(null);
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorKey.set('auth.forgotPassword.validation');
      this.messageKey.set(null);
      return;
    }

    this.isSubmitting.set(true);
    this.errorKey.set(null);
    this.messageKey.set(null);

    this.authService.forgotPassword(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => this.messageKey.set('auth.forgotPassword.success'),
        error: () => this.errorKey.set('auth.forgotPassword.failed')
      });
  }
}
