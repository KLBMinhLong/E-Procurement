import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpAvatarComponent } from '../../../../shared/components/ep-avatar/ep-avatar.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { AdminUserService } from '../../services/admin-user.service';
import { AdminOrgService } from '../../services/admin-org.service';
import { AdminRbacService } from '../../services/admin-rbac.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  AdminUserSummary,
  AdminUserDetail,
  AdminDepartment,
  AdminRole,
  UserListFilter,
  UserStatus
} from '../../models/admin.model';
import { PageMeta } from '../../../../core/models/api-response.model';
import { EpPageChangeEvent } from '../../../../shared/shared.index';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  LOCKED: 'danger',
  PENDING_VERIFY: 'warning'
};

@Component({
  selector: 'ep-admin-user-management',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFilterBarComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpBadgeComponent,
    EpAvatarComponent,
    EpIconComponent,
    EpModalComponent,
    EpFormFieldComponent
  ],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  private readonly userService = inject(AdminUserService);
  private readonly orgService = inject(AdminOrgService);
  private readonly rbacService = inject(AdminRbacService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  // ── State ──────────────────────────────────────────────────────────
  readonly items = signal<AdminUserSummary[]>([]);
  readonly departments = signal<AdminDepartment[]>([]);
  readonly roles = signal<AdminRole[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly isStatusMutating = signal<string | null>(null);

  readonly page = signal(1);
  readonly size = signal(10);
  readonly searchQuery = signal('');
  readonly activeStatus = signal<UserStatus | ''>('');
  readonly selectedDepartmentId = signal('');

  readonly filter = computed<UserListFilter>(() => ({
    page: this.page(),
    size: this.size(),
    q: this.searchQuery() || undefined,
    status: this.activeStatus() || undefined,
    departmentId: this.selectedDepartmentId() || undefined
  }));

  readonly statusTone = STATUS_TONE;

  // ── Modal State & Reactive Form ──────────────────────────────
  readonly isModalOpen = signal(false);
  readonly modalMode = signal<'create' | 'edit'>('create');
  readonly selectedUserId = signal<string | null>(null);
  readonly isSubmitting = signal(false);
  readonly isModalLoading = signal(false);

  // ── Status confirm dialog ────────────────────────────────────
  readonly isStatusDialogOpen = signal(false);
  readonly statusDialogUser = signal<AdminUserSummary | null>(null);
  readonly statusDialogReason = signal('');

  // ── Reset Password Modal ───────────────────────────────────────
  readonly isResetPasswordModalOpen = signal(false);
  readonly resetPasswordUser = signal<AdminUserSummary | null>(null);
  readonly isResetPasswordSubmitting = signal(false);
  readonly resetPasswordForm: FormGroup = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^])[A-Za-z\d@$!%*?&#^]{8,128}$/)]]
  });

  // Original roles for the user being edited (to detect changes)
  private editOriginalRoles: string[] = [];

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
    this.loadDepartments();
    this.loadRoles();
    this.loadData();
  }

  // ── Event Handlers ────────────────────────────────────────────────
  onSearchChange(q: string): void {
    this.searchQuery.set(q);
    this.page.set(1);
    this.loadData();
  }

  onStatusChange(status: string): void {
    this.activeStatus.set(status as UserStatus | '');
    this.page.set(1);
    this.loadData();
  }

  onDepartmentChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedDepartmentId.set(value);
    this.page.set(1);
    this.loadData();
  }

  onPageChange(event: EpPageChangeEvent): void {
    this.page.set(event.page);
    this.size.set(event.size);
    this.loadData();
  }

  // ── Status toggle with confirm dialog ──────────────────────────
  requestToggleStatus(user: AdminUserSummary): void {
    this.statusDialogUser.set(user);
    this.statusDialogReason.set('');
    this.isStatusDialogOpen.set(true);
  }

  closeStatusDialog(): void {
    this.isStatusDialogOpen.set(false);
    this.statusDialogUser.set(null);
    this.statusDialogReason.set('');
  }

  confirmToggleStatus(): void {
    const user = this.statusDialogUser();
    if (!user) return;

    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    const reason = this.statusDialogReason().trim() || undefined;

    this.isStatusMutating.set(user.id);
    this.closeStatusDialog();

    this.userService
      .changeStatus(user.id, newStatus, reason)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isStatusMutating.set(null))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey(
            newStatus === 'ACTIVE' ? 'admin.users.toast.unlockSuccess' : 'admin.users.toast.lockSuccess'
          );
          this.loadData();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Error occurred');
        }
      });
  }

  // ── Modal Handlers ───────────────────────────────────────────────
  openCreateModal(): void {
    this.modalMode.set('create');
    this.selectedUserId.set(null);
    this.editOriginalRoles = [];
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
    this.isModalOpen.set(true);
  }

  openEditModal(user: AdminUserSummary): void {
    this.modalMode.set('edit');
    this.selectedUserId.set(user.id);
    this.isModalLoading.set(true);

    this.userService
      .getById(user.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isModalLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          const detail = res.data;
          if (detail) {
            this.editOriginalRoles = [...(detail.roles || [])];
            this.userForm.reset({
              employeeCode: detail.employeeCode || '',
              username: detail.username,
              email: detail.email,
              fullName: detail.fullName,
              phone: detail.phone || '',
              departmentId: detail.departmentId || '',
              roles: detail.roles || []
            });
            this.userForm.get('employeeCode')?.disable();
            this.userForm.get('username')?.disable();
            this.userForm.get('email')?.disable();
            this.isModalOpen.set(true);
          }
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to load user details');
        }
      });
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  // ── Reset Password Handlers ─────────────────────────────────────
  openResetPasswordModal(user: AdminUserSummary): void {
    this.resetPasswordUser.set(user);
    this.resetPasswordForm.reset({ newPassword: '' });
    this.isResetPasswordModalOpen.set(true);
  }

  closeResetPasswordModal(): void {
    this.isResetPasswordModalOpen.set(false);
    this.resetPasswordUser.set(null);
  }

  onResetPasswordSubmit(): void {
    this.resetPasswordForm.markAllAsTouched();
    if (this.resetPasswordForm.invalid) {
      return;
    }

    const user = this.resetPasswordUser();
    if (!user) return;

    this.isResetPasswordSubmitting.set(true);
    const { newPassword } = this.resetPasswordForm.getRawValue();

    this.userService
      .resetPassword(user.id, newPassword)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isResetPasswordSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey('admin.users.toast.resetPasswordSuccess');
          this.closeResetPasswordModal();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to reset password');
        }
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

    this.isSubmitting.set(true);
    const formValue = this.userForm.getRawValue();

    if (this.modalMode() === 'create') {
      // Create: single call includes roles
      this.userService.create({
        employeeCode: formValue.employeeCode,
        username: formValue.username,
        email: formValue.email,
        fullName: formValue.fullName,
        phone: formValue.phone || null,
        departmentId: formValue.departmentId,
        roles: formValue.roles
      })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => this.isSubmitting.set(false))
        )
        .subscribe({
          next: () => {
            this.toastService.successKey('admin.users.toast.createSuccess');
            this.closeModal();
            this.loadData();
          },
          error: (err: any) => {
            this.toastService.error(err.message || 'Create failed');
          }
        });
    } else {
      // Edit: update info + assign roles (if changed) as separate calls
      const userId = this.selectedUserId()!;
      const updateInfo$ = this.userService.update(userId, {
        fullName: formValue.fullName,
        phone: formValue.phone || null,
        departmentId: formValue.departmentId
      });

      const newRoles: string[] = formValue.roles || [];
      const rolesChanged = !this.arraysEqual(newRoles, this.editOriginalRoles);

      if (rolesChanged) {
        forkJoin([updateInfo$, this.userService.assignRoles(userId, newRoles)])
          .pipe(
            takeUntilDestroyed(this.destroyRef),
            finalize(() => this.isSubmitting.set(false))
          )
          .subscribe({
            next: () => {
              this.toastService.successKey('admin.users.toast.updateSuccess');
              this.closeModal();
              this.loadData();
            },
            error: (err: any) => {
              this.toastService.error(err.message || 'Update failed');
            }
          });
      } else {
        updateInfo$
          .pipe(
            takeUntilDestroyed(this.destroyRef),
            finalize(() => this.isSubmitting.set(false))
          )
          .subscribe({
            next: () => {
              this.toastService.successKey('admin.users.toast.updateSuccess');
              this.closeModal();
              this.loadData();
            },
            error: (err: any) => {
              this.toastService.error(err.message || 'Update failed');
            }
          });
      }
    }
  }

  // ── Private Loader methods ────────────────────────────────────────
  loadDepartments(): void {
    this.orgService
      .getDepartments()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const flatList: AdminDepartment[] = [];
          const flatten = (nodes: AdminDepartment[]) => {
            for (const node of nodes) {
              flatList.push(node);
              if (node.children && node.children.length > 0) {
                flatten(node.children);
              }
            }
          };
          flatten(res.data ?? []);
          this.departments.set(flatList);
        }
      });
  }

  loadRoles(): void {
    this.rbacService
      .getRoles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.roles.set(res.data ?? []);
        },
        error: () => {
          // Fallback: keep empty, form will still work
          this.roles.set([]);
        }
      });
  }

  loadData(): void {
    this.isLoading.set(true);
    this.userService
      .list(this.filter())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          this.items.set(res.data ?? []);
          this.meta.set(res.meta ?? null);
        },
        error: (err: any) => {
          this.items.set([]);
          this.meta.set(null);
          this.toastService.error(err.message || 'Failed to load users');
        }
      });
  }

  private arraysEqual(a: string[], b: string[]): boolean {
    if (a.length !== b.length) return false;
    const sortedA = [...a].sort();
    const sortedB = [...b].sort();
    return sortedA.every((val, i) => val === sortedB[i]);
  }
}
