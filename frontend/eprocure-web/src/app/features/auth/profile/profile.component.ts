import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, effect } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { EpBreadcrumbComponent } from '../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../shared/components/ep-icon/ep-icon.component';
import { EpAvatarComponent } from '../../../shared/components/ep-avatar/ep-avatar.component';

@Component({
  selector: 'ep-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    DatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpAvatarComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly user = this.authService.currentUser;

  // Tabs state: 'info' | 'password' | 'security'
  readonly activeTab = signal<'info' | 'password' | 'security'>('info');

  // Loading states
  readonly isSubmittingProfile = signal(false);
  readonly isSubmittingPassword = signal(false);
  readonly isEnabling2FA = signal(false);
  readonly isConfirming2FA = signal(false);
  readonly isDisabling2FA = signal(false);

  // 2FA Setup state
  readonly twoFactorSetupData = signal<{ secret: string; qrCodeUrl: string; manualEntryKey: string } | null>(null);
  readonly otpCodeControl = this.fb.control('', [Validators.required, Validators.pattern(/^\d{6}$/)]);
  readonly backupCodes = signal<string[]>([]);

  // Selected avatar state
  readonly selectedAvatarUrl = signal<string | null>(null);
  readonly isCustomAvatar = signal(false);

  // Preset avatars
  readonly presetAvatars = [
    { name: 'Deep Violet', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Admin&backgroundColor=673ab7,3f51b5' },
    { name: 'Ocean Blue', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Procure&backgroundColor=03a9f4,00bcd4' },
    { name: 'Fresh Emerald', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Manager&backgroundColor=4caf50,8bc34a' },
    { name: 'Sunset Peach', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Buyer&backgroundColor=ff5722,ff9800' },
    { name: 'Crimson Red', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Director&backgroundColor=e91e63,f44336' },
    { name: 'Dark Charcoal', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Sys&backgroundColor=37474f,263238' },
    { name: 'Creative Pink-Purple', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Creative&backgroundColor=ec407a,7e57c2' },
    { name: 'Cyberpunk Teal-Green', url: 'https://api.dicebear.com/7.x/initials/svg?seed=Cyber&backgroundColor=00897b,00e676' }
  ];

  // Forms using nonNullable to guarantee values are always string
  readonly profileForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    phone: ['', [Validators.maxLength(30)]],
    customAvatarUrl: ['']
  });

  readonly passwordForm = this.fb.nonNullable.group({
    oldPassword: ['', [Validators.required, Validators.maxLength(100)]],
    newPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(128),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^])[A-Za-z\d@$!%*?&#^]{8,128}$/)
      ]
    ],
    confirmPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]]
  }, {
    validators: (control: AbstractControl): ValidationErrors | null => {
      const oldPassword = control.get('oldPassword');
      const password = control.get('newPassword');
      const confirmPassword = control.get('confirmPassword');
      
      let hasError = false;

      // Check if new password is same as old password
      if (oldPassword && password && oldPassword.value && password.value) {
        if (oldPassword.value === password.value) {
          password.setErrors({ sameAsOld: true });
          hasError = true;
        } else {
          const newErrors = password.errors;
          if (newErrors && newErrors['sameAsOld']) {
            delete newErrors['sameAsOld'];
            password.setErrors(Object.keys(newErrors).length ? newErrors : null);
          }
        }
      }

      // Check if confirm password matches new password
      if (password && confirmPassword) {
        if (password.value !== confirmPassword.value) {
          confirmPassword.setErrors({ mismatch: true });
          hasError = true;
        } else {
          const confirmErrors = confirmPassword.errors;
          if (confirmErrors && confirmErrors['mismatch']) {
            delete confirmErrors['mismatch'];
            confirmPassword.setErrors(Object.keys(confirmErrors).length ? confirmErrors : null);
          }
        }
      }
      return hasError ? { formInvalid: true } : null;
    }
  });

  // Real-time password requirement indicators
  readonly newPasswordVal = signal('');
  readonly hasMinLength = computed(() => this.newPasswordVal().length >= 8);
  readonly hasUppercase = computed(() => /[A-Z]/.test(this.newPasswordVal()));
  readonly hasLowercase = computed(() => /[a-z]/.test(this.newPasswordVal()));
  readonly hasNumber = computed(() => /\d/.test(this.newPasswordVal()));
  readonly hasSpecialChar = computed(() => /[@$!%*?&#^]/.test(this.newPasswordVal()));

  constructor() {
    // Listen to new password changes for real-time validation feedback
    this.passwordForm.get('newPassword')?.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(val => this.newPasswordVal.set(val || ''));

    // Reactively update form and avatar selection when user signal changes
    effect(() => {
      const currentUserValue = this.user();
      if (currentUserValue) {
        this.profileForm.patchValue({
          fullName: currentUserValue.fullName,
          phone: currentUserValue.phone || ''
        });
        
        this.selectedAvatarUrl.set(currentUserValue.avatarUrl);
        
        // If the current avatarUrl is not in the preset list, it's custom
        const isPreset = this.presetAvatars.some(p => p.url === currentUserValue.avatarUrl);
        if (currentUserValue.avatarUrl && !isPreset) {
          this.isCustomAvatar.set(true);
          this.profileForm.patchValue({
            customAvatarUrl: currentUserValue.avatarUrl
          });
        } else {
          this.isCustomAvatar.set(false);
          this.profileForm.patchValue({
            customAvatarUrl: ''
          });
        }
      }
    }, { allowSignalWrites: true });
  }

  setTab(tab: 'info' | 'password' | 'security'): void {
    this.activeTab.set(tab);
    this.twoFactorSetupData.set(null);
    this.otpCodeControl.reset();
    this.backupCodes.set([]);
  }

  selectPresetAvatar(url: string): void {
    this.isCustomAvatar.set(false);
    this.selectedAvatarUrl.set(url);
    this.profileForm.patchValue({ customAvatarUrl: '' });
  }

  enableCustomAvatar(): void {
    this.isCustomAvatar.set(true);
    const customUrl = this.profileForm.get('customAvatarUrl')?.value || '';
    this.selectedAvatarUrl.set(customUrl || null);
  }

  onCustomAvatarUrlChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (this.isCustomAvatar()) {
      this.selectedAvatarUrl.set(value || null);
    }
  }

  submitProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSubmittingProfile.set(true);
    const formValue = this.profileForm.getRawValue();
    const avatarUrl = this.isCustomAvatar() ? formValue.customAvatarUrl : this.selectedAvatarUrl();

    this.authService.updateProfile({
      fullName: formValue.fullName,
      phone: formValue.phone || '',
      avatarUrl: avatarUrl || null
    })
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isSubmittingProfile.set(false))
    )
    .subscribe({
      next: () => {
        this.toastService.successKey('profile.info.updateProfileSuccess');
      },
      error: (err: HttpErrorResponse) => {
        this.toastService.error(err.error?.message || 'error.generic');
      }
    });
  }

  submitPassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      this.toastService.errorKey('profile.password.validation');
      return;
    }

    const val = this.passwordForm.getRawValue();
    if (val.newPassword !== val.confirmPassword) {
      this.passwordForm.get('confirmPassword')?.setErrors({ mismatch: true });
      this.toastService.errorKey('profile.password.mismatch');
      return;
    }

    this.isSubmittingPassword.set(true);
    this.authService.changePassword({
      oldPassword: val.oldPassword,
      newPassword: val.newPassword,
      confirmPassword: val.confirmPassword
    })
    .pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isSubmittingPassword.set(false))
    )
    .subscribe({
      next: () => {
        this.toastService.successKey('profile.password.success');
        this.passwordForm.reset();
      },
      error: (err: HttpErrorResponse) => {
        if (err.error?.code === 'IAM_001') {
          this.toastService.errorKey('error.oldPasswordIncorrect');
        } else {
          this.toastService.error(err.error?.message || 'error.generic');
        }
      }
    });
  }

  startEnable2FA(): void {
    this.isEnabling2FA.set(true);
    this.authService.enableTwoFactor()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isEnabling2FA.set(false))
      )
      .subscribe({
        next: (res) => {
          this.twoFactorSetupData.set(res.data);
          this.otpCodeControl.reset();
          this.backupCodes.set([]);
          this.toastService.successKey('profile.security.2fa.setupStarted');
        },
        error: (err: HttpErrorResponse) => {
          this.toastService.error(err.error?.message || 'error.generic');
        }
      });
  }

  confirm2FA(): void {
    if (this.otpCodeControl.invalid) {
      this.otpCodeControl.markAsTouched();
      return;
    }
    const code = this.otpCodeControl.value || '';
    this.isConfirming2FA.set(true);
    this.authService.confirmTwoFactor(code)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isConfirming2FA.set(false))
      )
      .subscribe({
        next: (res) => {
          this.backupCodes.set(res.data.backupCodes || []);
          this.twoFactorSetupData.set(null);
          this.otpCodeControl.reset();
          this.toastService.successKey('profile.security.2fa.confirmSuccess');
          this.authService.hydrateUserContext().subscribe();
        },
        error: (err: HttpErrorResponse) => {
          this.toastService.error(err.error?.message || 'error.generic');
        }
      });
  }

  cancelSetup(): void {
    this.twoFactorSetupData.set(null);
    this.otpCodeControl.reset();
    this.backupCodes.set([]);
  }

  disable2FA(): void {
    if (!confirm('Bạn có chắc chắn muốn tắt xác thực 2FA không? Quá trình này sẽ làm giảm tính bảo mật của tài khoản.')) {
      return;
    }
    this.isDisabling2FA.set(true);
    this.authService.disableTwoFactor()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isDisabling2FA.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey('profile.security.2fa.disableSuccess');
          this.twoFactorSetupData.set(null);
          this.otpCodeControl.reset();
          this.backupCodes.set([]);
          this.authService.hydrateUserContext().subscribe();
        },
        error: (err: HttpErrorResponse) => {
          this.toastService.error(err.error?.message || 'error.generic');
        }
      });
  }

  encodeURIComponent(val: string): string {
    return encodeURIComponent(val);
  }

  copyBackupCodes(): void {
    const text = this.backupCodes().join('\n');
    navigator.clipboard.writeText(text).then(() => {
      this.toastService.success('Đã sao chép danh sách mã dự phòng vào Clipboard.');
    });
  }

  downloadBackupCodes(): void {
    const text = `DANH SÁCH MÃ DỰ PHÒNG XÁC THỰC 2FA (E-PROCUREMENT)\nNgày tạo: ${new Date().toLocaleString()}\nTài khoản: ${this.user()?.username}\n\nHãy lưu trữ các mã này ở nơi an toàn. Mỗi mã chỉ có thể sử dụng một lần:\n\n${this.backupCodes().join('\n')}\n`;
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `eprocure-2fa-backup-codes-${this.user()?.username}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
