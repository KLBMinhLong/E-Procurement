import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { permissionGuard } from './core/permissions/permission.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
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
        title: 'route.procurement',
        canActivate: [permissionGuard],
        data: { requiredPermission: 'PR_VIEW_OWN' },
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'approvals',
        title: 'route.approvals',
        canActivate: [permissionGuard],
        data: { requiredPermission: 'PR_APPROVE_L1' },
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'admin',
        title: 'route.admin',
        canActivate: [permissionGuard],
        data: { requiredPermission: 'ADMIN_USER_VIEW' },
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./features/errors/not-found/not-found.component').then((m) => m.NotFoundComponent)
  }
];
