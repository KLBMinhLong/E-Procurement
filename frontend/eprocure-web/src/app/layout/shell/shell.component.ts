import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IMessage } from '@stomp/stompjs';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { NavigationItem } from '../../core/models/navigation.model';
import { PermissionService } from '../../core/permissions/permission.service';
import { NotificationItem, NotificationService } from '../../core/services/notification.service';
import { ToastService } from '../../core/services/toast.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { EpAvatarComponent } from '../../shared/components/ep-avatar/ep-avatar.component';
import { EpBreadcrumbComponent } from '../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../shared/components/ep-icon/ep-icon.component';
import { EpLangSwitcherComponent } from '../../shared/components/ep-lang-switcher/ep-lang-switcher.component';

@Component({
  selector: 'ep-shell',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    DatePipe,
    TranslatePipe,
    EpAvatarComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpIconComponent,
    EpLangSwitcherComponent
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly permissionService = inject(PermissionService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);
  private readonly toastService = inject(ToastService);
  private readonly websocketService = inject(WebsocketService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSidebarOpen = signal(false);
  readonly isSidebarCollapsed = signal(false);
  readonly isNotificationOpen = signal(false);
  readonly notificationsLoading = signal(false);
  readonly unreadCount = signal(0);
  readonly latestNotifications = signal<NotificationItem[]>([]);
  readonly user = this.authService.currentUser;
  readonly isHydrating = this.authService.isHydrating;
  readonly toastMessages = this.toastService.messages;
  readonly currentYear = new Date().getFullYear();
  readonly canViewNotifications = computed(() => this.permissionService.hasPermission('NOTIFICATION_VIEW_OWN'));
  readonly notificationBadge = computed(() => {
    const count = this.unreadCount();
    return count > 99 ? '99+' : String(count);
  });

  readonly navItems = computed<NavigationItem[]>(() => [
    { icon: 'layout-dashboard', labelKey: 'nav.dashboard', route: '/dashboard' },
    { icon: 'shopping-cart', labelKey: 'nav.purchaseRequest', route: '/procurement', permissions: ['PR_VIEW_OWN', 'PR_VIEW_DEPARTMENT', 'PR_VIEW_ALL'] },
    { icon: 'inbox', labelKey: 'nav.approvals', route: '/approvals', permissions: ['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'PR_APPROVE_FINANCE', 'PR_APPROVE_EMERGENCY'] },
    { icon: 'building-2', labelKey: 'nav.vendors', route: '/vendors', permissions: ['VENDOR_VIEW'] },
    { icon: 'file-search', labelKey: 'nav.rfq', route: '/vendors/rfq', permissions: ['RFQ_VIEW', 'RFQ_CREATE'] },
    { icon: 'package', labelKey: 'nav.inventory', route: '/inventory', permissions: ['GR_VIEW', 'GR_CREATE', 'STOCK_VIEW'] },
    { icon: 'receipt-text', labelKey: 'nav.purchaseOrders', route: '/finance/purchase-orders', permissions: ['PO_VIEW_OWN', 'PO_VIEW_ALL', 'PO_CREATE'] },
    { icon: 'file-check-2', labelKey: 'nav.invoices', route: '/finance/invoices', permissions: ['INVOICE_VIEW', 'INVOICE_CREATE', 'PAYMENT_CONFIRM'] },
    { icon: 'users', labelKey: 'nav.adminUsers', route: '/admin/users', permissions: ['ADMIN_USER_VIEW', 'ADMIN_USER_MANAGE'] },
    { icon: 'user-cog', labelKey: 'nav.adminRoles', route: '/admin/roles', permissions: ['ADMIN_ROLE_MANAGE'] },
    { icon: 'shield', labelKey: 'nav.adminRbac', route: '/admin/rbac', permissions: ['ADMIN_ROLE_MANAGE'] },
    { icon: 'network', labelKey: 'nav.adminOrgChart', route: '/admin/org-chart', permissions: ['ORG_VIEW', 'ADMIN_DEPARTMENT_MANAGE'] },
    { icon: 'file-code-2', labelKey: 'nav.notificationTemplates', route: '/admin/notification-templates', permissions: ['SYSTEM_CONFIG'] },
    { icon: 'workflow', labelKey: 'nav.approvalRules', route: '/approvals/rules', permissions: ['ADMIN_APPROVAL_RULE'] }
  ].filter((item) => this.permissionService.hasAnyPermission(item.permissions)));

  ngOnInit(): void {
    if (!this.canViewNotifications()) {
      return;
    }

    this.loadNotifications();
    this.websocketService.connect((message) => this.handleRealtimeNotification(message));
    this.destroyRef.onDestroy(() => this.websocketService.disconnect());
  }

  toggleSidebar(): void {
    if (typeof window !== 'undefined' && window.innerWidth > 900) {
      this.isSidebarCollapsed.update((value) => !value);
    } else {
      this.isSidebarOpen.update((value) => !value);
    }
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
  }

  toggleNotifications(): void {
    if (!this.canViewNotifications()) {
      return;
    }
    this.isNotificationOpen.update((value) => !value);
    if (this.isNotificationOpen()) {
      this.loadNotifications();
    }
  }

  closeNotifications(): void {
    this.isNotificationOpen.set(false);
  }

  markAllNotificationsRead(): void {
    if (this.unreadCount() === 0) {
      return;
    }

    this.notificationService.markAllRead()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.unreadCount.set(0);
          this.latestNotifications.update((items) => items.map((item) => ({
            ...item,
            isRead: true,
            readAt: item.readAt ?? new Date().toISOString()
          })));
        }
      });
  }

  markNotificationRead(notification: NotificationItem, event?: Event): void {
    event?.stopPropagation();
    if (notification.isRead) {
      return;
    }

    this.notificationService.markRead(notification.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.applyReadNotification(response.data)
      });
  }

  openNotification(notification: NotificationItem): void {
    if (!notification.isRead) {
      this.markNotificationRead(notification);
    }
    if (notification.actionUrl?.startsWith('/')) {
      this.router.navigateByUrl(notification.actionUrl);
      this.closeNotifications();
    }
  }

  logout(): void {
    this.authService.logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate(['/login']),
        error: () => {
          this.authService.clearSession();
          this.router.navigate(['/login']);
        }
      });
  }

  dismissToast(id: string): void {
    this.toastService.dismiss(id);
  }

  private loadNotifications(): void {
    this.notificationsLoading.set(true);
    forkJoin({
      count: this.notificationService.countUnread(),
      list: this.notificationService.listLatest(5)
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.notificationsLoading.set(false))
    ).subscribe({
      next: ({ count, list }) => {
        this.unreadCount.set(count.data.unread);
        this.latestNotifications.set(list.data);
      }
    });
  }

  private handleRealtimeNotification(message: IMessage): void {
    const notification = this.parseNotificationMessage(message.body);
    if (!notification) {
      return;
    }

    this.upsertNotification(notification);
    if (!notification.isRead) {
      this.unreadCount.update((count) => count + 1);
    }
    this.toastService.info(notification.subject ?? notification.body);
  }

  private parseNotificationMessage(rawMessage: string): NotificationItem | null {
    try {
      const payload = JSON.parse(rawMessage) as Partial<NotificationItem> & { read?: boolean };
      if (!payload.id || !payload.body || !payload.createdAt) {
        return null;
      }
      return {
        id: payload.id,
        eventType: payload.eventType ?? 'UNKNOWN',
        channel: payload.channel ?? 'IN_APP',
        subject: payload.subject ?? null,
        body: payload.body,
        isRead: payload.isRead ?? payload.read ?? false,
        readAt: payload.readAt ?? null,
        referenceType: payload.referenceType ?? null,
        referenceId: payload.referenceId ?? null,
        referenceNumber: payload.referenceNumber ?? null,
        actionUrl: payload.actionUrl ?? null,
        createdAt: payload.createdAt
      };
    } catch {
      return null;
    }
  }

  private applyReadNotification(notification: NotificationItem): void {
    this.unreadCount.update((count) => Math.max(0, count - 1));
    this.latestNotifications.update((items) => items.map((item) =>
      item.id === notification.id ? notification : item
    ));
  }

  private upsertNotification(notification: NotificationItem): void {
    this.latestNotifications.update((items) => [
      notification,
      ...items.filter((item) => item.id !== notification.id)
    ].slice(0, 5));
  }
}
