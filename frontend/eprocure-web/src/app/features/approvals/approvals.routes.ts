import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/permissions/permission.guard';

export const approvalsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/approval-inbox/approval-inbox.component').then((m) => m.ApprovalInboxComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'ADMIN_USER_VIEW'] },
    title: 'route.approvals.inbox'
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/approval-detail/approval-detail.component').then((m) => m.ApprovalDetailComponent),
    canActivate: [permissionGuard],
    data: { requiredPermissions: ['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'ADMIN_USER_VIEW'] },
    title: 'route.approvals.detail'
  }
];
