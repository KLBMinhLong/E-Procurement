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
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { ToastService } from '../../../../core/services/toast.service';
import { formatVendorAddress, VendorDetail } from '../../models/vendor.model';
import { VendorService } from '../../services/vendor.service';

const STATUS_TONE: Record<string, EpBadgeTone> = {
  PENDING: 'warning',
  APPROVED: 'success',
  BLACKLISTED: 'danger',
  INACTIVE: 'neutral'
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
    EpModalComponent,
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

  readonly vendor = signal<VendorDetail | null>(null);
  readonly isLoading = signal(false);
  readonly isActioning = signal(false);
  readonly showApproveModal = signal(false);
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

  openApproveModal(): void {
    this.showApproveModal.set(true);
  }

  closeApproveModal(): void {
    this.showApproveModal.set(false);
  }

  confirmApprove(): void {
    const v = this.vendor();
    if (!v) return;

    this.isActioning.set(true);
    this.vendorService.approve(v.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isActioning.set(false))
      )
      .subscribe({
        next: () => {
          this.showApproveModal.set(false);
          this.toastService.success('vendor.detail.toast.approved');
          this.loadVendor(v.id);
        }
      });
  }

  navigateBack(): void {
    this.router.navigate(['/vendors']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '--';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(iso));
  }

  formatAddress(vendor: VendorDetail): string {
    const formatted = formatVendorAddress(vendor.address);
    return formatted || '--';
  }

  scoreLabel(value: number | null | undefined): string {
    if (value == null) return '--';
    return value.toFixed(1);
  }

  formatPercent(value: number | null | undefined): string {
    if (value == null) return '--';
    return `${(value * 100).toFixed(1)}%`;
  }
}
