import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { GoodsReceiptService } from '../../services/goods-receipt.service';
import { GoodsReceiptDetail, GoodsReceiptUpdateCommand, GrLineItem } from '../../models/goods-receipt.model';
import { CompleteGrResponse } from '../../models/stock.model';
import { ToastService } from '../../../../core/services/toast.service';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { HasPermissionDirective } from '../../../../core/permissions/has-permission.directive';

@Component({
  selector: 'app-gr-detail',
  standalone: true,
  imports: [
    TranslatePipe,
    EpCardComponent,
    EpButtonComponent,
    EpBadgeComponent,
    EpIconComponent,
    EpBreadcrumbComponent,
    EpSkeletonComponent,
    EpModalComponent,
    EpFormFieldComponent,
    ReactiveFormsModule,
    HasPermissionDirective
  ],
  templateUrl: './gr-detail.html',
  styleUrls: ['./gr-detail.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GrDetail implements OnInit {
  private readonly grService = inject(GoodsReceiptService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly savingEdit = signal(false);
  readonly showCompleteModal = signal(false);
  readonly showEditModal = signal(false);
  readonly gr = signal<GoodsReceiptDetail | null>(null);
  readonly completeSummary = signal<CompleteGrResponse | null>(null);
  readonly editForm = this.fb.group({
    receivedAt: [''],
    notes: ['', [Validators.maxLength(2000)]],
    lineItems: this.fb.array([])
  });

  get editLineItems(): FormArray {
    return this.editForm.get('lineItems') as FormArray;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadGoodsReceipt(id);
    } else {
      this.toast.error('inventory.gr.detail.toast.notFound');
      this.router.navigate(['/inventory/goods-receipts']);
    }
  }

  private loadGoodsReceipt(id: string): void {
    this.loading.set(true);
    this.grService.getById(id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (res) => this.gr.set(res.data),
        error: () => {
          this.toast.error('inventory.gr.detail.toast.loadFailed');
          this.router.navigate(['/inventory/goods-receipts']);
        }
      });
  }

  openCompleteModal(): void {
    this.showCompleteModal.set(true);
  }

  closeCompleteModal(): void {
    this.showCompleteModal.set(false);
  }

  openEditModal(): void {
    const currentGr = this.gr();
    if (!currentGr || !this.canEdit()) {
      return;
    }
    this.editForm.controls.receivedAt.setValue(this.toDateTimeLocal(currentGr.receivedAt));
    this.editForm.controls.notes.setValue(currentGr.notes ?? '');
    this.editLineItems.clear();
    currentGr.lineItems.forEach((line) => this.editLineItems.push(this.createLineGroup(line)));
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    if (this.savingEdit()) {
      return;
    }
    this.showEditModal.set(false);
  }

  saveDraft(): void {
    const currentGr = this.gr();
    if (!currentGr || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.savingEdit.set(true);
    this.grService.update(currentGr.id, this.toUpdateCommand(), crypto.randomUUID())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.savingEdit.set(false))
      )
      .subscribe({
        next: (res) => {
          this.gr.set(res.data);
          this.toast.success('inventory.gr.detail.toast.updateSuccess');
          this.showEditModal.set(false);
        },
        error: () => this.toast.error('inventory.gr.detail.toast.updateFailed')
      });
  }

  confirmComplete(): void {
    const currentGr = this.gr();
    if (!currentGr) return;

    this.submitting.set(true);
    this.grService.complete(currentGr.id, crypto.randomUUID())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.completeSummary.set(res.data ?? null);
          this.toast.success('inventory.gr.detail.toast.completeSuccess');
          this.showCompleteModal.set(false);
          this.loadGoodsReceipt(currentGr.id);
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/inventory/goods-receipts']);
  }

  navigateStock(): void {
    const currentGr = this.gr();
    this.router.navigate(['/inventory/stock'], {
      queryParams: { warehouse_id: currentGr?.warehouse.id || undefined }
    });
  }

  navigateMovements(itemCode?: string | null): void {
    const currentGr = this.gr();
    this.router.navigate(['/inventory/stock/movements'], {
      queryParams: {
        warehouse_id: currentGr?.warehouse.id || undefined,
        item_code: itemCode || undefined
      }
    });
  }

  canComplete(): boolean {
    return this.gr()?.status === 'DRAFT';
  }

  canEdit(): boolean {
    return this.gr()?.status === 'DRAFT';
  }

  statusTone(status?: string): 'neutral' | 'success' | 'warning' | 'danger' | 'info' {
    switch (status) {
      case 'COMPLETE': return 'success';
      case 'PARTIAL': return 'warning';
      case 'DRAFT': return 'neutral';
      case 'DISCREPANCY': return 'danger';
      default: return 'neutral';
    }
  }

  shortId(id?: string): string {
    if (!id) return '';
    return id.length <= 12 ? id : `${id.slice(0, 8)}...${id.slice(-4)}`;
  }

  formatDateTime(isoString?: string): string {
    if (!isoString) return '--';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(isoString));
  }

  formatQuantity(value?: string | null): string {
    if (!value) return '0';
    return Number(value).toLocaleString('vi-VN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4
    });
  }

  lineControlError(index: number, controlName: string): string | null {
    const control = this.editLineItems.at(index)?.get(controlName);
    if (!control || !control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'inventory.gr.detail.edit.validation.required';
    }
    if (control.hasError('pattern')) {
      return 'inventory.gr.detail.edit.validation.numeric';
    }
    return 'inventory.gr.detail.edit.validation.invalid';
  }

  private createLineGroup(line: GrLineItem) {
    return this.fb.group({
      poLineItemId: [line.poLineItemId, [Validators.required]],
      itemName: [line.itemName],
      orderedQuantity: [line.orderedQuantity],
      unit: [line.unit],
      receivedQuantity: [line.receivedQuantity, [Validators.required, Validators.pattern(/^\d+(\.\d{1,4})?$/)]],
      rejectedQuantity: [line.rejectedQuantity ?? '0', [Validators.required, Validators.pattern(/^\d+(\.\d{1,4})?$/)]],
      rejectionReason: [line.rejectionReason ?? '', [Validators.maxLength(1000)]],
      lotNumber: [line.lotNumber ?? '', [Validators.maxLength(100)]]
    });
  }

  private toUpdateCommand(): GoodsReceiptUpdateCommand {
    const value = this.editForm.getRawValue();
    return {
      receivedAt: value.receivedAt ? new Date(value.receivedAt).toISOString() : null,
      notes: this.nullable(value.notes),
      lineItems: this.editLineItems.controls.map((control) => {
        const line = control.getRawValue() as {
          poLineItemId?: string | null;
          receivedQuantity?: string | null;
          rejectedQuantity?: string | null;
          rejectionReason?: string | null;
          lotNumber?: string | null;
        };
        return {
          poLineItemId: line.poLineItemId ?? '',
          receivedQuantity: line.receivedQuantity ?? '0',
          rejectedQuantity: line.rejectedQuantity ?? '0',
          rejectionReason: this.nullable(line.rejectionReason),
          lotNumber: this.nullable(line.lotNumber)
        };
      })
    };
  }

  private toDateTimeLocal(isoString?: string): string {
    if (!isoString) {
      return '';
    }
    const date = new Date(isoString);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    const offsetMs = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
  }

  private nullable(value: string | null | undefined): string | null {
    const trimmed = value?.trim();
    return trimmed ? trimmed : null;
  }
}
