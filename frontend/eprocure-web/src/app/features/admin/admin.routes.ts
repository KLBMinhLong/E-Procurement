import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/permissions/permission.guard';

export const adminRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'users'
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./pages/user-management/user-management.component').then((m) => m.UserManagementComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['ADMIN_USER_VIEW', 'ADMIN_USER_MANAGE'] },
    title: 'route.admin.users'
  },
  {
    path: 'rbac',
    loadComponent: () =>
      import('./pages/rbac/rbac.component').then((m) => m.RbacComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['ADMIN_ROLE_MANAGE'] },
    title: 'route.admin.rbac'
  },
  {
    path: 'org-chart',
    loadComponent: () =>
      import('./pages/org-chart/org-chart.component').then((m) => m.OrgChartComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['ADMIN_USER_VIEW', 'ADMIN_USER_MANAGE'] },
    title: 'route.admin.orgChart'
  }
];
