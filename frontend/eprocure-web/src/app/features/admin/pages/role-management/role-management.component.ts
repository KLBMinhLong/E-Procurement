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
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { AdminRbacService } from '../../services/admin-rbac.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AdminPermission, AdminRole, CreateRolePayload } from '../../models/admin.model';

type ModalMode = 'create' | 'view';

@Component({
  selector: 'ep-admin-role-management',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpModalComponent,
    EpFormFieldComponent,
    EpBadgeComponent
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
  readonly isModalOpen = signal(false);
  readonly modalMode = signal<ModalMode>('create');
  readonly selectedRole = signal<AdminRole | null>(null);
  readonly selectedRolePermissions = signal<string[]>([]);
  readonly isSubmitting = signal(false);

  // ── Create Form State ──────────────────────────────────────────────
  readonly formCode = signal('');
  readonly formName = signal('');
  readonly formDescription = signal('');
  readonly formPermissions = signal<Set<string>>(new Set());

  // ── Expanded Role Detail ───────────────────────────────────────────
  readonly expandedRoleCode = signal<string | null>(null);

  // ── Permission Module Filter (in modal) ────────────────────────────
  readonly permissionSearch = signal('');

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

  readonly permissionsByModule = computed(() => {
    const list = this.permissions();
    const groups = new Map<string, AdminPermission[]>();
    for (const perm of list) {
      const mod = perm.module || 'SYSTEM';
      if (!groups.has(mod)) groups.set(mod, []);
      groups.get(mod)!.push(perm);
    }
    return groups;
  });

  readonly filteredPermissionsByModule = computed(() => {
    const q = this.permissionSearch().toLowerCase().trim();
    const groups = this.permissionsByModule();
    if (!q) return groups;

    const filtered = new Map<string, AdminPermission[]>();
    for (const [mod, perms] of groups) {
      const matched = perms.filter(p =>
        p.code.toLowerCase().includes(q) ||
        p.name.toLowerCase().includes(q) ||
        mod.toLowerCase().includes(q)
      );
      if (matched.length > 0) filtered.set(mod, matched);
    }
    return filtered;
  });

  readonly modulesList = computed(() =>
    Array.from(this.filteredPermissionsByModule().keys())
  );

  readonly totalPermissionsCount = computed(() => this.permissions().length);

  readonly formSelectedCount = computed(() => this.formPermissions().size);

  // ── Form Validation ────────────────────────────────────────────────
  readonly formCodeError = computed(() => {
    const code = this.formCode();
    if (!code) return null;
    if (!/^[A-Z][A-Z0-9_]+$/.test(code)) return 'admin.roles.validation.codePattern';
    return null;
  });

  readonly isFormValid = computed(() => {
    const code = this.formCode().trim();
    const name = this.formName().trim();
    return code.length > 0 && name.length > 0 && !this.formCodeError();
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

  // ── Role Detail Expand ─────────────────────────────────────────────
  toggleExpand(roleCode: string): void {
    this.expandedRoleCode.update(current =>
      current === roleCode ? null : roleCode
    );
  }

  isExpanded(roleCode: string): boolean {
    return this.expandedRoleCode() === roleCode;
  }

  getRolePermissionCount(roleCode: string): number {
    return this.rolePermissionsMap().get(roleCode)?.size ?? 0;
  }

  getRolePermissionCodes(roleCode: string): string[] {
    return Array.from(this.rolePermissionsMap().get(roleCode) ?? []);
  }

  getPermissionName(code: string): string {
    return this.permissions().find(p => p.code === code)?.name ?? code;
  }

  getPermissionModule(code: string): string {
    return this.permissions().find(p => p.code === code)?.module ?? 'SYSTEM';
  }

  // ── Modal Actions ──────────────────────────────────────────────────
  openCreateModal(): void {
    this.modalMode.set('create');
    this.formCode.set('');
    this.formName.set('');
    this.formDescription.set('');
    this.formPermissions.set(new Set());
    this.permissionSearch.set('');
    this.isModalOpen.set(true);
  }

  openViewModal(role: AdminRole): void {
    this.modalMode.set('view');
    this.selectedRole.set(role);
    const perms = this.rolePermissionsMap().get(role.code);
    this.selectedRolePermissions.set(perms ? Array.from(perms) : []);
    this.permissionSearch.set('');
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.selectedRole.set(null);
  }

  // ── Form permission toggle ─────────────────────────────────────────
  toggleFormPermission(code: string): void {
    this.formPermissions.update(set => {
      const next = new Set(set);
      if (next.has(code)) next.delete(code);
      else next.add(code);
      return next;
    });
  }

  isFormPermissionSelected(code: string): boolean {
    return this.formPermissions().has(code);
  }

  toggleModuleAll(moduleName: string): void {
    const modulePerms = this.permissionsByModule().get(moduleName) ?? [];
    const codes = modulePerms.map(p => p.code);
    const allSelected = codes.every(c => this.formPermissions().has(c));

    this.formPermissions.update(set => {
      const next = new Set(set);
      if (allSelected) {
        codes.forEach(c => next.delete(c));
      } else {
        codes.forEach(c => next.add(c));
      }
      return next;
    });
  }

  isModuleAllSelected(moduleName: string): boolean {
    const modulePerms = this.permissionsByModule().get(moduleName) ?? [];
    return modulePerms.length > 0 && modulePerms.every(p => this.formPermissions().has(p.code));
  }

  isModulePartialSelected(moduleName: string): boolean {
    const modulePerms = this.permissionsByModule().get(moduleName) ?? [];
    const selectedCount = modulePerms.filter(p => this.formPermissions().has(p.code)).length;
    return selectedCount > 0 && selectedCount < modulePerms.length;
  }

  // ── Submit Create ──────────────────────────────────────────────────
  submitCreateRole(): void {
    if (!this.isFormValid() || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    const payload: CreateRolePayload = {
      code: this.formCode().trim(),
      name: this.formName().trim(),
      description: this.formDescription().trim() || null,
      permissions: Array.from(this.formPermissions())
    };

    this.rbacService.createRole(payload)
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
          this.closeModal();
          this.loadData();
        },
        error: (err: any) => {
          this.toastService.error(err.error?.message || err.message || 'Failed to create role');
        }
      });
  }

  // ── Search ─────────────────────────────────────────────────────────
  onSearchChange(query: string): void {
    this.searchQuery.set(query);
  }

  onPermissionSearchChange(query: string): void {
    this.permissionSearch.set(query);
  }

  // ── Helpers ────────────────────────────────────────────────────────
  isViewPermissionActive(permCode: string): boolean {
    return this.selectedRolePermissions().includes(permCode);
  }

  hasModulePermissions(modName: string): boolean {
    const perms = this.filteredPermissionsByModule().get(modName) ?? [];
    return perms.some(p => this.selectedRolePermissions().includes(p.code));
  }
}
