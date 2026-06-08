import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/permissions/permission.guard';

export const vendorRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'list'
  },
  {
    path: 'list',
    loadComponent: () =>
      import('./pages/vendor-list/vendor-list.component').then((m) => m.VendorListComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['VENDOR_VIEW'] },
    title: 'route.vendor.list'
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/vendor-create/vendor-create.component').then((m) => m.VendorCreateComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['VENDOR_CREATE'] },
    title: 'route.vendor.create'
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/vendor-detail/vendor-detail.component').then((m) => m.VendorDetailComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['VENDOR_VIEW'] },
    title: 'route.vendor.detail'
  },
  {
    path: 'rfq',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/rfq-list/rfq-list.component').then((m) => m.RfqListComponent),
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['RFQ_VIEW'] },
        title: 'route.rfq.list'
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./pages/rfq-detail/rfq-detail.component').then((m) => m.RfqDetailComponent),
        canActivate: [permissionGuard],
        data: { requiredPermissions: ['RFQ_VIEW'] },
        title: 'route.rfq.detail'
      }
    ]
  }
];
