import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { NavigationItem } from '../../core/models/navigation.model';
import { PermissionService } from '../../core/permissions/permission.service';
import { ToastService } from '../../core/services/toast.service';
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
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly permissionService = inject(PermissionService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSidebarOpen = signal(false);
  readonly user = this.authService.currentUser;
  readonly toastMessages = this.toastService.messages;

  readonly navItems = computed<NavigationItem[]>(() => [
    { icon: 'layout-dashboard', labelKey: 'nav.dashboard', route: '/dashboard' },
    { icon: 'shopping-cart', labelKey: 'nav.purchaseRequest', route: '/procurement', permissions: ['PR_VIEW_OWN', 'PR_VIEW_DEPARTMENT', 'PR_VIEW_ALL', 'ADMIN_USER_VIEW'] },
    { icon: 'inbox', labelKey: 'nav.approvals', route: '/approvals', permissions: ['PR_APPROVE_L1', 'PR_APPROVE_L2', 'PR_APPROVE_L3', 'ADMIN_USER_VIEW'] },
    { icon: 'users', labelKey: 'nav.admin', route: '/admin', permissions: ['ADMIN_USER_VIEW', 'SYSTEM_CONFIG'] }
  ].filter((item) => this.permissionService.hasAnyPermission(item.permissions)));

  toggleSidebar(): void {
    this.isSidebarOpen.update((value) => !value);
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
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
}
