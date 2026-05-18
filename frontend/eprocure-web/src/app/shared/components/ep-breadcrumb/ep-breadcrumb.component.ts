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

const ROUTE_LABELS: Record<string, string> = {
  dashboard: 'route.dashboard',
  procurement: 'route.procurement',
  approvals: 'route.approvals',
  admin: 'route.admin',
  'ui-showcase': 'route.uiShowcase'
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

    return segments.map((segment, index) => ({
      labelKey: ROUTE_LABELS[segment] ?? segment,
      route: `/${segments.slice(0, index + 1).join('/')}`
    }));
  }
}
