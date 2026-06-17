import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { loginGuard } from './core/auth/login.guard';
import { permissionGuard } from './core/permissions/permission.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [loginGuard],
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component')
      .then((m) => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component')
      .then((m) => m.ResetPasswordComponent)
  },
  {
    path: 'ui-showcase',
    title: 'route.uiShowcase',
    loadComponent: () => import('./ui-showcase/ui-showcase.component').then((m) => m.UiShowcaseComponent)
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./features/errors/forbidden/forbidden.component').then((m) => m.ForbiddenComponent)
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        title: 'route.dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'procurement',
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['PR_VIEW_OWN', 'PR_VIEW_DEPARTMENT', 'PR_VIEW_ALL'] },
        loadChildren: () =>
          import('./features/procurement/procurement.routes').then((m) => m.procurementRoutes)
      },
      {
        path: 'approvals',
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'PR_APPROVE_FINANCE', 'PR_APPROVE_EMERGENCY', 'ADMIN_APPROVAL_RULE'] },
        loadChildren: () =>
          import('./features/approvals/approvals.routes').then((m) => m.approvalsRoutes)
      },
      {
        path: 'vendors',
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['VENDOR_VIEW', 'VENDOR_CREATE', 'RFQ_VIEW'] },
        loadChildren: () => import('./features/vendor/vendor.routes').then((m) => m.vendorRoutes)
      },
      {
        path: 'inventory',
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['GR_VIEW', 'GR_CREATE', 'GR_ISSUE_OUT', 'ADMIN_CATALOG_MANAGE'] },
        loadChildren: () => import('./features/inventory/inventory.routes').then((m) => m.inventoryRoutes)
      },
      {
        path: 'finance',
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['PO_VIEW_OWN', 'PO_VIEW_ALL', 'PO_CREATE', 'INVOICE_VIEW', 'INVOICE_CREATE', 'PAYMENT_CONFIRM'] },
        loadChildren: () => import('./features/finance/finance.routes').then((m) => m.financeRoutes)
      },
      {
        path: 'admin',
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['ADMIN_USER_VIEW', 'ADMIN_ROLE_MANAGE', 'SYSTEM_CONFIG', 'SYSTEM_AUDIT_VIEW', 'ADMIN_APPROVAL_RULE', 'ADMIN_CATALOG_MANAGE', 'ADMIN_DEPARTMENT_MANAGE', 'ORG_VIEW'] },
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.adminRoutes)
      },
      {
        path: 'profile',
        title: 'route.profile',
        loadComponent: () => import('./features/auth/profile/profile.component').then((m) => m.ProfileComponent)
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./features/errors/not-found/not-found.component').then((m) => m.NotFoundComponent)
  }
];
