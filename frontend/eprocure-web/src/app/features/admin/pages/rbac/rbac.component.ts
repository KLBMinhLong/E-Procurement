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
    EpIconComponent
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

  // Group permissions by their business module
  readonly permissionsByModule = computed(() => {
    const list = this.permissions();
    const groups = new Map<string, AdminPermission[]>();
    for (const perm of list) {
      const mod = perm.module || 'SYSTEM';
      if (!groups.has(mod)) {
        groups.set(mod, []);
      }
      groups.get(mod)!.push(perm);
    }
    return groups;
  });

  readonly modulesList = computed(() => {
    return Array.from(this.permissionsByModule().keys());
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
}
