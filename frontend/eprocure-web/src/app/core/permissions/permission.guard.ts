import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PermissionService } from './permission.service';

export const permissionGuard: CanActivateFn = (route) => {
  const permissionService = inject(PermissionService);
  const router = inject(Router);
  const requiredPermission = route.data['requiredPermission'];

  if (typeof requiredPermission !== 'string') {
    return true;
  }

  return permissionService.hasPermission(requiredPermission)
    ? true
    : router.createUrlTree(['/forbidden']);
};
