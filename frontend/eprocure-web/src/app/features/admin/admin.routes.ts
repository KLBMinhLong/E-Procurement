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
    path: 'roles',
    loadComponent: () =>
      import('./pages/role-management/role-management.component').then((m) => m.RoleManagementComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['ADMIN_ROLE_MANAGE'] },
    title: 'route.admin.roles'
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
    data: { requiredPermissions: ['ORG_VIEW', 'ADMIN_DEPARTMENT_MANAGE'] },
    title: 'route.admin.orgChart'
  },
  {
    path: 'notification-templates',
    loadComponent: () =>
      import('./pages/notification-templates/notification-templates.component').then((m) => m.NotificationTemplatesComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['SYSTEM_CONFIG'] },
    title: 'route.admin.notificationTemplates'
  },
  {
    path: 'config',
    loadComponent: () =>
      import('./pages/system-config/system-config.component').then((m) => m.SystemConfigComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['SYSTEM_CONFIG'] },
    title: 'route.admin.config'
  },
  {
    path: 'audit-log',
    loadComponent: () =>
      import('./pages/audit-log/audit-log.component').then((m) => m.AuditLogComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['SYSTEM_AUDIT_VIEW'] },
    title: 'route.admin.auditLog'
  },
  {
    path: 'catalog-categories',
    loadComponent: () =>
      import('./pages/catalog-categories/catalog-categories.component').then((m) => m.CatalogCategoriesComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['ADMIN_CATALOG_MANAGE'] },
    title: 'route.admin.catalogCategories'
  },
  {
    path: 'sessions',
    loadComponent: () =>
      import('./pages/sessions/sessions.component').then((m) => m.SessionsComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['SYSTEM_CONFIG'] },
    title: 'route.admin.sessions'
  },
  {
    path: 'health',
    loadComponent: () =>
      import('./pages/system-health/system-health.component').then((m) => m.SystemHealthComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['SYSTEM_CONFIG'] },
    title: 'route.admin.health'
  },
  {
    path: 'approval-rules',
    redirectTo: '/approvals/rules'
  }
];
