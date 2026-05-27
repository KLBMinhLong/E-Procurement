import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
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
export class UserFormModalComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);

  @Input() isOpen = false;
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() user: AdminUserSummary | null = null;
  @Input() departments: AdminDepartment[] = [];
  @Input() roles: AdminRole[] = [];
  @Input() isLoading = false;
  @Input() isSubmitting = false;

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
