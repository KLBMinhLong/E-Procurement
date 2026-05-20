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
import { finalize } from 'rxjs';

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
import { ToastService } from '../../../../core/services/toast.service';
import { AdminUserSummary, AdminDepartment, UserListFilter } from '../../models/admin.model';
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
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  // ── State ──────────────────────────────────────────────────────────
  readonly items = signal<AdminUserSummary[]>([]);
  readonly departments = signal<AdminDepartment[]>([]);
  readonly meta = signal<PageMeta | null>(null);
  readonly isLoading = signal(false);
  readonly isStatusMutating = signal<string | null>(null);

  readonly page = signal(1);
  readonly size = signal(10);
  readonly searchQuery = signal('');
  readonly activeStatus = signal<'PENDING_VERIFY' | 'ACTIVE' | 'INACTIVE' | 'LOCKED' | ''>('');
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

  readonly availableRoles = [
    { code: 'ADMIN', name: 'System Admin' },
    { code: 'REQUESTER', name: 'Requester' },
    { code: 'MANAGER', name: 'Department Manager' },
    { code: 'FINANCE', name: 'Finance Reviewer' },
    { code: 'PROCUREMENT', name: 'Procurement Executive' }
  ];

  readonly userForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', [Validators.required]],
    phone: [''],
    departmentId: ['', [Validators.required]],
    roles: [[] as string[], [Validators.required]]
  });

  ngOnInit(): void {
    this.loadDepartments();
    this.loadData();
  }

  // ── Event Handlers ────────────────────────────────────────────────
  onSearchChange(q: string): void {
    this.searchQuery.set(q);
    this.page.set(1);
    this.loadData();
  }

  onStatusChange(status: string): void {
    this.activeStatus.set(status as any);
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

  toggleStatus(user: AdminUserSummary): void {
    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    this.isStatusMutating.set(user.id);

    this.userService
      .changeStatus(user.id, newStatus)
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
    this.userForm.reset({
      username: '',
      email: '',
      fullName: '',
      phone: '',
      departmentId: '',
      roles: []
    });
    this.userForm.get('username')?.enable();
    this.userForm.get('email')?.enable();
    this.isModalOpen.set(true);
  }

  openEditModal(user: AdminUserSummary): void {
    this.modalMode.set('edit');
    this.selectedUserId.set(user.id);
    this.isLoading.set(true);

    this.userService
      .getById(user.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => {
          const detail = res.data;
          if (detail) {
            this.userForm.reset({
              username: detail.username,
              email: detail.email,
              fullName: detail.fullName,
              phone: detail.phone || '',
              departmentId: detail.departmentId || '',
              roles: detail.roles || []
            });
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
    const formValue = this.userForm.getRawValue(); // raw value gets disabled values like username/email too
    const obs$ = this.modalMode() === 'create'
      ? this.userService.create(formValue)
      : this.userService.update(this.selectedUserId()!, {
          fullName: formValue.fullName,
          phone: formValue.phone || null,
          departmentId: formValue.departmentId,
          roles: formValue.roles
        });

    obs$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.successKey(
            this.modalMode() === 'create'
              ? 'admin.users.toast.createSuccess'
              : 'admin.users.toast.updateSuccess'
          );
          this.closeModal();
          this.loadData();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Save operation failed');
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
}
