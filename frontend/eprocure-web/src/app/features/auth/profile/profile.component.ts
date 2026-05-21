import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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

  // Tabs state: 'info' | 'password'
  readonly activeTab = signal<'info' | 'password'>('info');

  // Loading states
  readonly isSubmittingProfile = signal(false);
  readonly isSubmittingPassword = signal(false);

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
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]]
  });

  constructor() {
    // Set form initial values from current user
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
      }
    }
  }

  setTab(tab: 'info' | 'password'): void {
    this.activeTab.set(tab);
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
        this.toastService.error(err.error?.message || 'error.generic');
      }
    });
  }
}
