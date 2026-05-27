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
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { AdminRbacService } from '../../services/admin-rbac.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AdminPermission, AdminRole, CreateRolePayload, UpdateRolePayload } from '../../models/admin.model';

import { RoleListComponent } from './components/role-list/role-list.component';
import { RoleFormModalComponent } from './components/role-form-modal/role-form-modal.component';

@Component({
  selector: 'ep-admin-role-management',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpIconComponent,
    RoleListComponent,
    RoleFormModalComponent
  ],
  templateUrl: './role-management.component.html',
  styleUrl: './role-management.component.scss'
})
export class RoleManagementComponent implements OnInit {
  private readonly rbacService = inject(AdminRbacService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly roles = signal<AdminRole[]>([]);
  readonly permissions = signal<AdminPermission[]>([]);
  readonly rolePermissionsMap = signal<Map<string, Set<string>>>(new Map());
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');

  // ── Modal State ────────────────────────────────────────────────────
  readonly isFormOpen = signal(false);
  readonly selectedRole = signal<AdminRole | null>(null);
  readonly isSubmitting = signal(false);

  // Computed
  readonly filteredRoles = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const list = this.roles();
    if (!q) return list;
    return list.filter(r =>
      r.code.toLowerCase().includes(q) ||
      r.name.toLowerCase().includes(q) ||
      (r.description?.toLowerCase().includes(q) ?? false)
    );
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    forkJoin({
      rolesRes: this.rbacService.getRoles(),
      permissionsRes: this.rbacService.getPermissions()
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: ({ rolesRes, permissionsRes }) => {
          const fetchedRoles = rolesRes.data ?? [];
          this.roles.set(fetchedRoles);
          this.permissions.set(permissionsRes.data ?? []);

          if (fetchedRoles.length > 0) {
            this.loadAllRolePermissions(fetchedRoles);
          }
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to load roles');
        }
      });
  }

  private loadAllRolePermissions(fetchedRoles: AdminRole[]): void {
    const requests = fetchedRoles.reduce((acc, role) => {
      acc[role.code] = this.rbacService.getRolePermissions(role.code);
      return acc;
    }, {} as Record<string, any>);

    forkJoin(requests)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (results: any) => {
          const map = new Map<string, Set<string>>();
          for (const roleCode of Object.keys(results)) {
            map.set(roleCode, new Set(results[roleCode].data ?? []));
          }
          this.rolePermissionsMap.set(map);
          this.cdr.markForCheck();
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to load role permissions');
        }
      });
  }

  // ── Modal Actions ──────────────────────────────────────────────────
  openCreateModal(): void {
    this.selectedRole.set(null);
    this.isFormOpen.set(true);
  }

  openEditModal(role: AdminRole): void {
    this.selectedRole.set(role);
    this.isFormOpen.set(true);
  }

  closeFormModal(): void {
    this.isFormOpen.set(false);
    this.selectedRole.set(null);
  }

  // ── Submit Create / Update ─────────────────────────────────────────
  submitRoleForm(payload: CreateRolePayload | UpdateRolePayload): void {
    if (this.isSubmitting()) return;

    this.isSubmitting.set(true);
    const role = this.selectedRole();

    if (role) {
      this.rbacService.updateRole(role.code, payload as UpdateRolePayload)
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => {
            this.isSubmitting.set(false);
            this.cdr.markForCheck();
          })
        )
        .subscribe({
          next: () => {
            this.toastService.successKey('admin.roles.toast.updateDetailsSuccess');
            this.closeFormModal();
            this.loadData();
          },
          error: (err: any) => {
            this.toastService.error(err.error?.message || err.message || 'Failed to update role');
          }
        });
    } else {
      this.rbacService.createRole(payload as CreateRolePayload)
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          finalize(() => {
            this.isSubmitting.set(false);
            this.cdr.markForCheck();
          })
        )
        .subscribe({
          next: () => {
            this.toastService.successKey('admin.roles.toast.createSuccess');
            this.closeFormModal();
            this.loadData();
          },
          error: (err: any) => {
            this.toastService.error(err.error?.message || err.message || 'Failed to create role');
          }
        });
    }
  }

  // ── Delete Role ────────────────────────────────────────────────────
  onDeleteRole(role: AdminRole): void {
    this.toastService.error('Không thể xóa vai trò này. Nhằm đảm bảo an toàn hệ thống và tránh xung đột phân quyền, việc xóa vai trò bị vô hiệu hóa bởi chính sách bảo mật của tổ chức.');
  }

  // ── Search ─────────────────────────────────────────────────────────
  onSearchChange(query: string): void {
    this.searchQuery.set(query);
  }
}

