import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFilterBarComponent } from '../../../../shared/components/ep-filter-bar/ep-filter-bar.component';
import { AdminUserService } from '../../services/admin-user.service';
import { AdminOrgService } from '../../services/admin-org.service';
import { AdminRbacService } from '../../services/admin-rbac.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  AdminUserSummary,
  AdminDepartment,
  AdminRole,
  UserListFilter,
  UserStatus
} from '../../models/admin.model';
import { PageMeta } from '../../../../core/models/api-response.model';
import { EpPageChangeEvent } from '../../../../shared/shared.index';

import { UserListComponent } from './components/user-list/user-list.component';
import { UserFormModalComponent, UserFormSubmitEvent } from './components/user-form-modal/user-form-modal.component';
import { UserStatusDialogComponent } from './components/user-status-dialog/user-status-dialog.component';
import { UserResetPasswordModalComponent } from './components/user-reset-password-modal/user-reset-password-modal.component';

@Component({
  selector: 'ep-admin-user-management',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpFilterBarComponent,
    UserListComponent,
    UserFormModalComponent,
    UserStatusDialogComponent,
    UserResetPasswordModalComponent
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
  private readonly cdr = inject(ChangeDetectorRef);

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

  // ── Modal State ──────────────────────────────────────────────────
  readonly isModalOpen = signal(false);
  readonly modalMode = signal<'create' | 'edit'>('create');
  readonly selectedUser = signal<AdminUserSummary | null>(null);
  readonly isSubmitting = signal(false);
  readonly apiErrors = signal<Record<string, string> | null>(null);

  // ── Status confirm dialog ────────────────────────────────────
  readonly isStatusDialogOpen = signal(false);
  readonly statusDialogUser = signal<AdminUserSummary | null>(null);

  // ── Reset Password Modal ───────────────────────────────────────
  readonly isResetPasswordModalOpen = signal(false);
  readonly resetPasswordUser = signal<AdminUserSummary | null>(null);
  readonly isResetPasswordSubmitting = signal(false);

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
    this.isStatusDialogOpen.set(true);
  }

  closeStatusDialog(): void {
    this.isStatusDialogOpen.set(false);
    this.statusDialogUser.set(null);
  }

  confirmToggleStatus(reason: string): void {
    const user = this.statusDialogUser();
    if (!user) return;

    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    const finalReason = reason.trim() || undefined;

    this.isStatusMutating.set(user.id);
    this.closeStatusDialog();

    this.userService
      .changeStatus(user.id, newStatus, finalReason)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isStatusMutating.set(null);
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.toastService.successKey(
            newStatus === 'ACTIVE' ? 'admin.users.toast.unlockSuccess' : 'admin.users.toast.lockSuccess'
          );
          this.items.update(users =>
            users.map(u => u.id === user.id ? { ...u, status: newStatus } : u)
          );
          this.cdr.detectChanges();
          this.loadData(false);
        },
        error: (err: any) => {
          const body = err?.error;
          const code = body?.code;
          if (code === 'IAM_036') {
            this.toastService.errorKey('admin.users.toast.selfLockError');
          } else {
            this.toastService.error(body?.message || err.message || 'Error occurred');
          }
          this.cdr.detectChanges();
        }
      });
  }

  // ── Modal Handlers ───────────────────────────────────────────────
  openCreateModal(): void {
    this.apiErrors.set(null);
    this.modalMode.set('create');
    this.selectedUser.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(user: AdminUserSummary): void {
    this.apiErrors.set(null);
    this.modalMode.set('edit');
    this.selectedUser.set(user);
    this.isModalOpen.set(true);

    // Refresh details silently in the background
    this.userService
      .getById(user.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (res) => {
          const detail = res.data;
          if (detail && this.isModalOpen() && this.selectedUser()?.id === user.id) {
            this.selectedUser.set(detail);
            this.cdr.detectChanges();
          }
        },
        error: (err: any) => {
          console.warn('Failed to background-refresh user details', err);
        }
      });
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.apiErrors.set(null);
  }

  parseApiValidationError(err: any): Record<string, string> | null {
    const errorBody = err?.error;
    if (!errorBody) return null;

    const apiErrors: Record<string, string> = {};

    // 1. Single field validation error
    if (errorBody.field && errorBody.reason) {
      apiErrors[errorBody.field] = errorBody.reason;
      return apiErrors;
    }

    // 2. List of validation errors
    if (Array.isArray(errorBody.errors)) {
      errorBody.errors.forEach((e: any) => {
        if (e.field && e.reason) {
          apiErrors[e.field] = e.reason;
        } else if (e.field && e.message) {
          apiErrors[e.field] = e.message;
        }
      });
      return Object.keys(apiErrors).length ? apiErrors : null;
    }

    // 3. Fallback for other formats
    if (errorBody.details && typeof errorBody.details === 'object') {
      return errorBody.details;
    }

    return null;
  }

  onSubmitUserForm(event: UserFormSubmitEvent): void {
    this.isSubmitting.set(true);
    this.apiErrors.set(null);
    const formValue = event.formValue;

    if (event.mode === 'create') {
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
          finalize(() => {
            this.isSubmitting.set(false);
            this.cdr.detectChanges();
          })
        )
        .subscribe({
          next: () => {
            this.toastService.successKey('admin.users.toast.createSuccess');
            this.closeModal();
            this.loadData(false);
          },
          error: (err: any) => {
            const validationErrors = this.parseApiValidationError(err);
            if (validationErrors) {
              this.apiErrors.set(validationErrors);
              this.toastService.errorKey('admin.users.validation.validationFailed');
            } else {
              this.toastService.error(err.error?.message || err.message || 'Create failed');
            }
            this.cdr.detectChanges();
          }
        });
    } else {
      const user = this.selectedUser();
      if (!user) {
        this.isSubmitting.set(false);
        return;
      }

      const userId = user.id;
      const updateInfo$ = this.userService.update(userId, {
        fullName: formValue.fullName,
        phone: formValue.phone || null,
        departmentId: formValue.departmentId
      });

      const newRoles: string[] = formValue.roles || [];
      const rolesChanged = !this.arraysEqual(newRoles, event.originalRoles);

      if (rolesChanged) {
        forkJoin([updateInfo$, this.userService.assignRoles(userId, newRoles)])
          .pipe(
            takeUntilDestroyed(this.destroyRef),
            finalize(() => {
              this.isSubmitting.set(false);
              this.cdr.detectChanges();
            })
          )
          .subscribe({
            next: () => {
              this.toastService.successKey('admin.users.toast.updateSuccess');
              this.closeModal();
              this.loadData(false);
            },
            error: (err: any) => {
              const validationErrors = this.parseApiValidationError(err);
              if (validationErrors) {
                this.apiErrors.set(validationErrors);
                this.toastService.errorKey('admin.users.validation.validationFailed');
              } else {
                this.toastService.error(err.error?.message || err.message || 'Update failed');
              }
              this.cdr.detectChanges();
            }
          });
      } else {
        updateInfo$
          .pipe(
            takeUntilDestroyed(this.destroyRef),
            finalize(() => {
              this.isSubmitting.set(false);
              this.cdr.detectChanges();
            })
          )
          .subscribe({
            next: () => {
              this.toastService.successKey('admin.users.toast.updateSuccess');
              this.closeModal();
              this.loadData(false);
            },
            error: (err: any) => {
              const validationErrors = this.parseApiValidationError(err);
              if (validationErrors) {
                this.apiErrors.set(validationErrors);
                this.toastService.errorKey('admin.users.validation.validationFailed');
              } else {
                this.toastService.error(err.error?.message || err.message || 'Update failed');
              }
              this.cdr.detectChanges();
            }
          });
      }
    }
  }

  // ── Reset Password Handlers ─────────────────────────────────────
  openResetPasswordModal(user: AdminUserSummary): void {
    this.resetPasswordUser.set(user);
    this.isResetPasswordModalOpen.set(true);
  }

  closeResetPasswordModal(): void {
    this.isResetPasswordModalOpen.set(false);
    this.resetPasswordUser.set(null);
  }

  onResetPasswordSubmit(newPassword: string): void {
    const user = this.resetPasswordUser();
    if (!user) return;

    this.isResetPasswordSubmitting.set(true);

    this.userService
      .resetPassword(user.id, newPassword)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isResetPasswordSubmitting.set(false);
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.toastService.successKey('admin.users.toast.resetPasswordSuccess');
          this.closeResetPasswordModal();
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to reset password');
          this.cdr.detectChanges();
        }
      });
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
          this.cdr.detectChanges();
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
          this.cdr.detectChanges();
        },
        error: () => {
          // Fallback: keep empty, form will still work
          this.roles.set([]);
          this.cdr.detectChanges();
        }
      });
  }

  loadData(showSkeleton = true): void {
    if (showSkeleton) {
      this.isLoading.set(true);
    }
    this.userService
      .list(this.filter())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (showSkeleton) {
            this.isLoading.set(false);
          }
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (res) => {
          this.items.set(res.data ?? []);
          this.meta.set(res.meta ?? null);
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          this.items.set([]);
          this.meta.set(null);
          this.toastService.error(err.message || 'Failed to load users');
          this.cdr.detectChanges();
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
