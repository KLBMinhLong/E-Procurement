import { computed, inject, Injectable } from '@angular/core';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly authService = inject(AuthService);

  readonly permissionSet = computed(() => new Set(this.authService.currentUser()?.permissions ?? []));

  hasPermission(permission: string): boolean {
    return this.permissionSet().has(permission);
  }

  hasAnyPermission(permissions: string[] | undefined): boolean {
    if (!permissions || permissions.length === 0) {
      return true;
    }

    const currentPermissions = this.permissionSet();
    return permissions.some((permission) => currentPermissions.has(permission));
  }
}
