import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PermissionService } from './permission.service';

export const permissionGuard: CanActivateFn = (route) => {
  const permissionService = inject(PermissionService);
  const router = inject(Router);
  const requiredPermissions = route.data['requiredPermissions'] as string[] | undefined;

  if (!requiredPermissions || !Array.isArray(requiredPermissions)) {
    return true;
  }

  return permissionService.hasAnyPermission(requiredPermissions)
    ? true
    : router.createUrlTree(['/forbidden']);
};
