import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AdminDepartment, AdminRole, AdminUserSummary } from '../../../../models/admin.model';
import { EpModalComponent } from '../../../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpFormFieldComponent } from '../../../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpButtonComponent } from '../../../../../../shared/components/ep-button/ep-button.component';

export interface UserFormSubmitEvent {
  mode: 'create' | 'edit';
  formValue: any;
  originalRoles: string[];
}

@Component({
  selector: 'ep-admin-user-form-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpModalComponent,
    EpSkeletonComponent,
    EpFormFieldComponent,
    EpButtonComponent
  ],
  templateUrl: './user-form-modal.component.html',
  styleUrl: './user-form-modal.component.scss'
})
export class UserFormModalComponent implements OnChanges, OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly translateService = inject(TranslateService);

  @Input() isOpen = false;
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() user: AdminUserSummary | null = null;
  @Input() departments: AdminDepartment[] = [];
  @Input() roles: AdminRole[] = [];
  @Input() isLoading = false;
  @Input() isSubmitting = false;

  private _apiErrors: Record<string, string> = {};

  @Input() set apiErrors(errors: Record<string, string> | null) {
    this._apiErrors = errors || {};
    if (errors) {
      Object.keys(errors).forEach(field => {
        const control = this.userForm.get(field);
        if (control) {
          control.setErrors({ serverError: errors[field] });
          control.markAsTouched();
        }
      });
    }
  }

  get apiErrors(): Record<string, string> {
    return this._apiErrors;
  }

  @Output() closeModal = new EventEmitter<void>();
  @Output() submitForm = new EventEmitter<UserFormSubmitEvent>();

  originalRoles: string[] = [];

  readonly userForm: FormGroup = this.fb.group({
    employeeCode: ['', [Validators.required, Validators.maxLength(20)]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9._-]+$/)]],
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    phone: ['', [Validators.maxLength(30)]],
    departmentId: ['', [Validators.required]],
    roles: [[] as string[], [Validators.required]]
  });

  ngOnInit(): void {
    // Listen to changes to clear serverError dynamically as user types
    Object.keys(this.userForm.controls).forEach(key => {
      this.userForm.get(key)?.valueChanges.subscribe(() => {
        const control = this.userForm.get(key);
        if (control && control.hasError('serverError')) {
          const errors = { ...control.errors };
          delete errors['serverError'];
          control.setErrors(Object.keys(errors).length ? errors : null);
          
          if (this._apiErrors && this._apiErrors[key]) {
            delete this._apiErrors[key];
          }
        }
      });
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
    }
    
    // When user updates in background (from detailed fetch)
    if (changes['user'] && this.isOpen && this.mode === 'edit' && this.user) {
      this.updateFormWithUserDetails(this.user);
    }
  }

  private initForm(): void {
    this._apiErrors = {};
    if (this.mode === 'create') {
      this.originalRoles = [];
      this.userForm.reset({
        employeeCode: '',
        username: '',
        email: '',
        fullName: '',
        phone: '',
        departmentId: '',
        roles: []
      });
      this.userForm.get('employeeCode')?.enable();
      this.userForm.get('username')?.enable();
      this.userForm.get('email')?.enable();
    } else if (this.mode === 'edit' && this.user) {
      this.updateFormWithUserDetails(this.user);
      this.userForm.get('employeeCode')?.disable();
      this.userForm.get('username')?.disable();
      this.userForm.get('email')?.disable();
    }
  }

  private updateFormWithUserDetails(user: AdminUserSummary): void {
    this.originalRoles = [...(user.roles || [])];
    this.userForm.patchValue({
      employeeCode: user.employeeCode || '',
      username: user.username,
      email: user.email,
      fullName: user.fullName,
      phone: user.phone || '',
      departmentId: user.departmentId || '',
      roles: user.roles || []
    });
  }

  onRoleCheckboxChange(event: Event, roleCode: string): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    const currentRoles = this.userForm.value.roles ?? [];
    if (isChecked) {
      if (!currentRoles.includes(roleCode)) {
        this.userForm.patchValue({ roles: [...currentRoles, roleCode] });
      }
    } else {
      this.userForm.patchValue({ roles: currentRoles.filter((r: string) => r !== roleCode) });
    }
    this.userForm.get('roles')?.markAsTouched();
  }

  isRoleSelected(roleCode: string): boolean {
    return (this.userForm.value.roles ?? []).includes(roleCode);
  }

  getFieldError(fieldName: string): string | null {
    const control = this.userForm.get(fieldName);
    if (!control || !control.touched) return null;

    if (control.hasError('required')) {
      return this.translateService.instant('admin.users.validation.required');
    }
    if (control.hasError('minlength')) {
      return this.translateService.instant('admin.users.validation.usernameMinLength');
    }
    if (control.hasError('pattern')) {
      return this.translateService.instant('admin.users.validation.usernamePattern');
    }
    if (control.hasError('email')) {
      return this.translateService.instant('admin.users.validation.email');
    }
    if (control.hasError('serverError')) {
      return this.translateServerErrorMessage(control.getError('serverError'));
    }

    return null;
  }

  private translateServerErrorMessage(msg: string): string {
    if (!msg) return '';
    // Map standard English validation errors from backend into localized beautiful Vietnamese/English
    if (msg.includes('Phone number must contain only digits')) {
      return this.translateService.instant('admin.users.validation.phonePattern') || msg;
    }
    if (msg.includes('Employee code already exists')) {
      return this.translateService.instant('admin.users.validation.employeeCodeExists') || msg;
    }
    if (msg.includes('Username already exists')) {
      return this.translateService.instant('admin.users.validation.usernameExists') || msg;
    }
    if (msg.includes('Email already exists')) {
      return this.translateService.instant('admin.users.validation.emailExists') || msg;
    }
    return msg;
  }

  onSubmit(): void {
    this.userForm.markAllAsTouched();
    if (this.userForm.invalid) {
      return;
    }
    this.submitForm.emit({
      mode: this.mode,
      formValue: this.userForm.getRawValue(),
      originalRoles: this.originalRoles
    });
  }
}
