import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/permissions/permission.guard';

export const procurementRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/pr-list/pr-list.component').then((m) => m.PrListComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PR_VIEW_OWN', 'PR_VIEW_DEPARTMENT', 'PR_VIEW_ALL', 'ADMIN_USER_VIEW'] },
    title: 'route.procurement'
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/pr-create/pr-create.component').then((m) => m.PrCreateComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PR_CREATE', 'ADMIN_USER_VIEW'] },
    title: 'route.pr.create'
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/pr-detail/pr-detail.component').then((m) => m.PrDetailComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PR_VIEW_OWN', 'PR_VIEW_DEPARTMENT', 'PR_VIEW_ALL', 'ADMIN_USER_VIEW'] },
    title: 'route.pr.detail'
  }
];
