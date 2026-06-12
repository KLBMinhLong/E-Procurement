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
  },
  {
    path: 'budgets',
    loadComponent: () =>
      import('./pages/budget-list/budget-list.component').then((m) => m.BudgetListComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
    title: 'route.finance.budgets'
  },
  {
    path: 'budgets/:id',
    loadComponent: () =>
      import('./pages/budget-detail/budget-detail.component').then((m) => m.BudgetDetailComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['BUDGET_VIEW_OWN_DEPT', 'BUDGET_VIEW_ALL'] },
    title: 'route.finance.budgetDetail'
  },
  {
    path: 'invoices',
    loadComponent: () =>
      import('./pages/invoice-list/invoice-list.component').then((m) => m.InvoiceListComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['INVOICE_VIEW'] },
    title: 'route.finance.invoices'
  },
  {
    path: 'invoices/create',
    loadComponent: () =>
      import('./pages/invoice-create/invoice-create.component').then((m) => m.InvoiceCreateComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['INVOICE_CREATE'] },
    title: 'route.finance.createInvoice'
  },
  {
    path: 'invoices/:id',
    loadComponent: () =>
      import('./pages/invoice-detail/invoice-detail.component').then((m) => m.InvoiceDetailComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['INVOICE_VIEW'] },
    title: 'route.finance.invoiceDetail'
  }
];
