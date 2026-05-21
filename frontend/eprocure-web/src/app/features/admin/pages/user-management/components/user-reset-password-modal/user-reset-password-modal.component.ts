import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminUserSummary } from '../../../../models/admin.model';
import { EpModalComponent } from '../../../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpButtonComponent } from '../../../../../../shared/components/ep-button/ep-button.component';

@Component({
  selector: 'ep-admin-user-reset-password-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpModalComponent,
    EpFormFieldComponent,
    EpButtonComponent
  ],
  templateUrl: './user-reset-password-modal.component.html',
  styleUrl: './user-reset-password-modal.component.scss'
})
export class UserResetPasswordModalComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);

  @Input() isOpen = false;
  @Input() user: AdminUserSummary | null = null;
  @Input() isSubmitting = false;

  @Output() closeModal = new EventEmitter<void>();
  @Output() submitForm = new EventEmitter<string>();

  readonly resetPasswordForm: FormGroup = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^])[A-Za-z\d@$!%*?&#^]{8,128}$/)]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.resetPasswordForm.reset({ newPassword: '' });
    }
  }

  onSubmit(): void {
    this.resetPasswordForm.markAllAsTouched();
    if (this.resetPasswordForm.invalid) {
      return;
    }
    
    const { newPassword } = this.resetPasswordForm.getRawValue();
    this.submitForm.emit(newPassword);
  }
}
