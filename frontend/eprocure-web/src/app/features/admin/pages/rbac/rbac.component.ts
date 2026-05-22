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
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';

import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import { AdminRbacService } from '../../services/admin-rbac.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AdminPermission, AdminRole } from '../../models/admin.model';

@Component({
  selector: 'ep-admin-rbac',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpSkeletonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpStatCardComponent
  ],
  templateUrl: './rbac.component.html',
  styleUrl: './rbac.component.scss'
})
export class RbacComponent implements OnInit {
  private readonly rbacService = inject(AdminRbacService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly roles = signal<AdminRole[]>([]);
  readonly permissions = signal<AdminPermission[]>([]);
  readonly rolePermissionsMap = signal<Map<string, Set<string>>>(new Map());
  readonly isMutatingMap = signal<Map<string, boolean>>(new Map());
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');
  readonly selectedModule = signal('all');
  readonly focusRole = signal('all');
  readonly assignedOnly = signal(false);
  readonly roleSearchQuery = signal('');
  readonly visibleRoleCodes = signal<Set<string>>(new Set());

  readonly allModules = computed(() => {
    const modules = new Set<string>();
    for (const perm of this.permissions()) {
      modules.add(perm.module || 'SYSTEM');
    }
    return Array.from(modules).sort((a, b) => a.localeCompare(b));
  });

  readonly visibleRoles = computed(() => {
    const visible = this.visibleRoleCodes();
    return this.roles().filter(role => visible.has(role.code));
  });

  readonly visibleRolesCount = computed(() => this.visibleRoles().length);

  readonly filteredRoleOptions = computed(() => {
    const query = this.normalizeQuery(this.roleSearchQuery());
    return this.roles()
      .filter(role => {
        if (!query) {
          return true;
        }
        const haystack = `${role.name} ${role.code}`.toLowerCase();
        return haystack.includes(query);
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  });

  readonly filteredPermissions = computed(() => {
    const query = this.normalizeQuery(this.searchQuery());
    const selectedModule = this.selectedModule();
    const focusRole = this.focusRole();
    const assignedOnly = this.assignedOnly();
    const focusPermissions = focusRole !== 'all' ? this.rolePermissionsMap().get(focusRole) : undefined;

    return this.permissions().filter(perm => {
      const moduleName = perm.module || 'SYSTEM';
      if (selectedModule !== 'all' && moduleName !== selectedModule) {
        return false;
      }

      if (query && !this.matchesPermissionQuery(perm, query)) {
        return false;
      }

      if (!assignedOnly) {
        return true;
      }

      if (focusRole !== 'all') {
        return focusPermissions?.has(perm.code) ?? false;
      }

      return this.isPermissionAssigned(perm.code);
    });
  });

  // Group permissions by their business module (after filters)
  readonly permissionsByModule = computed(() => {
    const list = this.filteredPermissions();
    const groups = new Map<string, AdminPermission[]>();
    for (const perm of list) {
      const mod = perm.module || 'SYSTEM';
      if (!groups.has(mod)) {
        groups.set(mod, []);
      }
      groups.get(mod)!.push(perm);
    }
    return new Map([...groups.entries()].sort(([a], [b]) => a.localeCompare(b)));
  });

  readonly modulesList = computed(() => {
    return Array.from(this.permissionsByModule().keys());
  });

  readonly filteredPermissionsCount = computed(() => this.filteredPermissions().length);

  readonly totalPermissions = computed(() => this.permissions().length);

  readonly focusedRoleName = computed(() => {
    const focus = this.focusRole();
    if (focus === 'all') {
      return null;
    }
    return this.roles().find(role => role.code === focus)?.name ?? focus;
  });

  readonly assignedPermissionCount = computed(() => {
    const focus = this.focusRole();
    const map = this.rolePermissionsMap();
    if (focus !== 'all') {
      return map.get(focus)?.size ?? 0;
    }

    const assigned = new Set<string>();
    for (const permissions of map.values()) {
      permissions.forEach(code => assigned.add(code));
    }
    return assigned.size;
  });

  readonly assignedCoverage = computed(() => {
    const total = this.totalPermissions();
    if (!total) {
      return 0;
    }
    return Math.round((this.assignedPermissionCount() / total) * 100);
  });

  ngOnInit(): void {
    this.loadMatrixData();
  }

  loadMatrixData(): void {
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
          this.syncVisibleRoles(fetchedRoles);
          this.permissions.set(permissionsRes.data ?? []);

          // Load active permissions for each role concurrently
          if (fetchedRoles.length > 0) {
            this.loadAllRolePermissions(fetchedRoles);
          }
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to load RBAC matrix');
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
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to load role permissions');
        }
      });
  }

  togglePermission(roleCode: string, permissionCode: string): void {
    const key = `${roleCode}-${permissionCode}`;
    this.isMutatingMap.update(map => {
      map.set(key, true);
      return new Map(map);
    });

    const activePermissions = new Set(this.rolePermissionsMap().get(roleCode) ?? []);
    if (activePermissions.has(permissionCode)) {
      activePermissions.delete(permissionCode);
    } else {
      activePermissions.add(permissionCode);
    }

    const payload = Array.from(activePermissions);
    this.rbacService
      .updateRolePermissions(roleCode, payload)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isMutatingMap.update(map => {
            map.delete(key);
            return new Map(map);
          });
        })
      )
      .subscribe({
        next: () => {
          this.rolePermissionsMap.update(map => {
            map.set(roleCode, activePermissions);
            return new Map(map);
          });
          this.toastService.successKey('admin.rbac.toast.updateSuccess');
        },
        error: (err: any) => {
          this.toastService.error(err.message || 'Failed to update permission');
        }
      });
  }

  hasPermission(roleCode: string, permissionCode: string): boolean {
    return !!this.rolePermissionsMap().get(roleCode)?.has(permissionCode);
  }

  isToggling(roleCode: string, permissionCode: string): boolean {
    return !!this.isMutatingMap().get(`${roleCode}-${permissionCode}`);
  }

  onSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedModule.set('all');
    this.focusRole.set('all');
    this.assignedOnly.set(false);
    this.roleSearchQuery.set('');
    this.showAllRoles();
  }

  onRoleSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.roleSearchQuery.set(value);
  }

  isRoleVisible(roleCode: string): boolean {
    return this.visibleRoleCodes().has(roleCode);
  }

  toggleRoleVisibility(roleCode: string): void {
    this.visibleRoleCodes.update(set => {
      const next = new Set(set);
      if (next.has(roleCode)) {
        next.delete(roleCode);
      } else {
        next.add(roleCode);
      }
      return next;
    });
  }

  showAllRoles(): void {
    this.visibleRoleCodes.set(new Set(this.roles().map(role => role.code)));
  }

  hideAllRoles(): void {
    this.visibleRoleCodes.set(new Set());
  }

  showFocusedRole(): void {
    const focused = this.focusRole();
    if (focused === 'all') {
      return;
    }
    this.visibleRoleCodes.set(new Set([focused]));
  }

  getRoleAssignedCount(roleCode: string): number {
    return this.rolePermissionsMap().get(roleCode)?.size ?? 0;
  }

  isRoleFocused(roleCode: string): boolean {
    return this.focusRole() === roleCode;
  }

  isPermissionInFocus(permissionCode: string): boolean {
    const focus = this.focusRole();
    if (focus === 'all') {
      return false;
    }
    return this.hasPermission(focus, permissionCode);
  }

  private isPermissionAssigned(permissionCode: string): boolean {
    for (const permissions of this.rolePermissionsMap().values()) {
      if (permissions.has(permissionCode)) {
        return true;
      }
    }
    return false;
  }

  private normalizeQuery(value: string): string {
    return value.trim().toLowerCase();
  }

  private matchesPermissionQuery(permission: AdminPermission, query: string): boolean {
    if (!query) {
      return true;
    }

    const haystack = [
      permission.code,
      permission.name,
      permission.description,
      permission.module
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    return haystack.includes(query);
  }

  private syncVisibleRoles(fetchedRoles: AdminRole[]): void {
    this.visibleRoleCodes.update(existing => {
      const next = new Set<string>();
      const initial = existing.size === 0;
      const base = initial ? new Set(fetchedRoles.map(role => role.code)) : existing;
      for (const role of fetchedRoles) {
        if (base.has(role.code)) {
          next.add(role.code);
        }
      }
      return next;
    });
  }
}
