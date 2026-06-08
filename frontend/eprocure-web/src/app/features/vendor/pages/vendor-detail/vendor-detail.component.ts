import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';
import { EpBadgeComponent, EpBadgeTone } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { ToastService } from '../../../../core/services/toast.service';
import { Vendor } from '../../models/vendor.model';
import { VendorService } from '../../services/vendor.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  PENDING: 'warning',
  APPROVED: 'success',
  SUSPENDED: 'danger',
  DEACTIVATED: 'neutral'
};

@Component({
  selector: 'ep-vendor-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpIconComponent,
    EpSkeletonComponent,
    HasPermissionDirective
  ],
  templateUrl: './vendor-detail.component.html',
  styleUrl: './vendor-detail.component.scss'
})
export class VendorDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vendorService = inject(VendorService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly vendor = signal<Vendor | null>(null);
  readonly isLoading = signal(false);
  readonly isActioning = signal(false);
  readonly statusTone = STATUS_TONE;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadVendor(id);
  }

  loadVendor(id: string): void {
    this.isLoading.set(true);
    this.vendorService.getById(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (res) => this.vendor.set(res.data),
        error: () => this.router.navigate(['/vendors'])
      });
  }

  approveVendor(): void {
    const v = this.vendor();
    if (!v) return;

    this.isActioning.set(true);
    this.vendorService.approve(v.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: (res) => {
          this.vendor.set(res.data);
          this.toastService.success('vendor.detail.toast.approved');
        }
      });
  }

  deactivateVendor(): void {
    const v = this.vendor();
    if (!v) return;

    this.isActioning.set(true);
    this.vendorService.deactivate(v.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: (res) => {
          this.vendor.set(res.data);
          this.toastService.success('vendor.detail.toast.deactivated');
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/vendors']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  scoreLabel(value: number | null | undefined): string {
    if (!value) return '--';
    return value.toFixed(1);
  }
}
