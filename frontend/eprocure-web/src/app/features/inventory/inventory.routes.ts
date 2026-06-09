import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/permissions/permission.guard';

export const inventoryRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'goods-receipts'
  },
  {
    path: 'goods-receipts',
    loadComponent: () =>
      import('./pages/gr-list/gr-list').then((m) => m.GrList),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['GR_VIEW'] },
    title: 'route.inventory.gr.list'
  },
  {
    path: 'goods-receipts/create',
    loadComponent: () =>
      import('./pages/gr-create/gr-create').then((m) => m.GrCreateComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['GR_CREATE'] },
    title: 'route.inventory.gr.create'
  },
  {
    path: 'goods-receipts/:id',
    loadComponent: () =>
      import('./pages/gr-detail/gr-detail').then((m) => m.GrDetail),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['GR_VIEW'] },
    title: 'route.inventory.gr.detail'
  }
];
