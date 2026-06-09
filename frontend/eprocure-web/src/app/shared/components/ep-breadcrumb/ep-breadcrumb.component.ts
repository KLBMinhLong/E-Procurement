import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { EpIconComponent } from '../ep-icon/ep-icon.component';

interface BreadcrumbItem {
  labelKey: string;
  route: string;
}

const PATH_MAP: Record<string, string> = {
  '/dashboard': 'route.dashboard',
  '/procurement': 'route.procurement',
  '/procurement/create': 'route.pr.create',
  '/approvals': 'route.approvals.self',
  '/approvals/rules': 'route.approvals.rules',
  '/admin': 'route.admin.self',
  '/admin/users': 'route.admin.users',
  '/admin/roles': 'route.admin.roles',
  '/admin/rbac': 'route.admin.rbac',
  '/admin/org-chart': 'route.admin.orgChart',
  '/profile': 'route.profile',
  '/ui-showcase': 'route.uiShowcase',
  '/vendors': 'route.vendor.self',
  '/vendors/list': 'route.vendor.list',
  '/vendors/create': 'route.vendor.create',
  '/vendors/rfq': 'route.rfq.list',
  '/vendors/rfq/create': 'route.rfq.create'
};

@Component({
  selector: 'ep-breadcrumb',
  standalone: true,
  imports: [RouterLink, TranslatePipe, EpIconComponent],
  templateUrl: './ep-breadcrumb.component.html',
  styleUrl: './ep-breadcrumb.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpBreadcrumbComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly items = signal<BreadcrumbItem[]>(this.toBreadcrumbs(this.router.url));

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((event) => this.items.set(this.toBreadcrumbs(event.urlAfterRedirects)));
  }

  private toBreadcrumbs(url: string): BreadcrumbItem[] {
    const segments = url.split('?')[0].split('/').filter(Boolean);

    if (segments.length === 0) {
      return [{ labelKey: 'route.dashboard', route: '/dashboard' }];
    }

    return segments.map((segment, index) => {
      const path = '/' + segments.slice(0, index + 1).join('/');
      let labelKey = segment;

      if (PATH_MAP[path]) {
        labelKey = PATH_MAP[path];
      } else {
        // Handle dynamic routes with route params (like UUIDs/IDs)
        if (/^\/procurement\/[^/]+$/.test(path)) {
          labelKey = 'route.pr.detail';
        } else if (/^\/approvals\/[^/]+$/.test(path)) {
          labelKey = 'route.approvals.detail';
        } else if (/^\/vendors\/rfq\/[^/]+$/.test(path)) {
          labelKey = 'route.rfq.detail';
        } else if (/^\/vendors\/[^/]+$/.test(path) && segment !== 'list' && segment !== 'create' && segment !== 'rfq') {
          labelKey = 'route.vendor.detail';
        }
      }

      return {
        labelKey,
        route: path
      };
    });
  }
}
