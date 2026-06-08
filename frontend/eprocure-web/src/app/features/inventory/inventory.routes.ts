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
      import('./pages/gr-create/gr-create').then((m) => m.GrCreate),
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
  },
  {
    path: 'stock',
    loadComponent: () =>
      import('./pages/stock-dashboard/stock-dashboard').then((m) => m.StockDashboard),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['STOCK_VIEW'] },
    title: 'route.inventory.stock.dashboard'
  },
  {
    path: 'movements',
    loadComponent: () =>
      import('./pages/stock-movements/stock-movements').then((m) => m.StockMovements),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['STOCK_VIEW'] },
    title: 'route.inventory.stock.movements'
  },
  {
    path: 'issue-out',
    loadComponent: () =>
      import('./pages/issue-out/issue-out').then((m) => m.IssueOut),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['STOCK_ISSUE'] },
    title: 'route.inventory.stock.issue'
  }
];
