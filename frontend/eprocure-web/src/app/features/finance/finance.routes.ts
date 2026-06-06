import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/permissions/permission.guard';

export const financeRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'purchase-orders'
  },
  {
    path: 'purchase-orders',
    loadComponent: () =>
      import('./pages/po-list/po-list.component').then((m) => m.PoListComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PO_VIEW_OWN', 'PO_VIEW_ALL'] },
    title: 'route.finance.purchaseOrders'
  },
  {
    path: 'purchase-orders/create',
    loadComponent: () =>
      import('./pages/po-create/po-create.component').then((m) => m.PoCreateComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PO_CREATE'] },
    title: 'route.finance.createPo'
  },
  {
    path: 'purchase-orders/:id',
    loadComponent: () =>
      import('./pages/po-detail/po-detail.component').then((m) => m.PoDetailComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PO_VIEW_OWN', 'PO_VIEW_ALL'] },
    title: 'route.finance.poDetail'
  }
];
