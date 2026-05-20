import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, debounceTime, distinctUntilChanged } from 'rxjs';

import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpAmountComponent } from '../../../../shared/components/ep-amount/ep-amount.component';
import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpModalComponent } from '../../../../shared/components/ep-modal/ep-modal.component';

import { PurchaseRequestService } from '../../services/purchase-request.service';
import { CatalogService } from '../../services/catalog.service';
import {
  CatalogCategory,
  CatalogItem,
  CreatePrRequest,
  PrLineItemRequest,
  PrPriority
} from '../../models/purchase-request.model';

@Component({
  selector: 'ep-pr-create',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    EpButtonComponent,
    EpFormFieldComponent,
    EpBreadcrumbComponent,
    EpAmountComponent,
    EpCardComponent,
    EpIconComponent,
    EpBadgeComponent,
    EpSkeletonComponent,
    EpModalComponent
  ],
  templateUrl: './pr-create.component.html',
  styleUrl: './pr-create.component.scss'
})
export class PrCreateComponent implements OnInit {
  private readonly prService = inject(PurchaseRequestService);
  private readonly catalogService = inject(CatalogService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  // ── State ──────────────────────────────────────────────────────────
  readonly isSubmitting = signal(false);
  readonly isUploadingFile = signal(false);
  readonly showCatalogPicker = signal(false);
  readonly catalogPickerTargetIndex = signal<number | null>(null);
  readonly catalogSearchQuery = signal('');
  readonly catalogItems = signal<CatalogItem[]>([]);
  readonly catalogCategories = signal<CatalogCategory[]>([]);
  readonly isCatalogLoading = signal(false);
  readonly uploadedAttachments = signal<{ id: string; fileName: string; fileSize: number }[]>([]);

  readonly priorities: PrPriority[] = ['NORMAL', 'URGENT', 'EMERGENCY'];

  readonly showUrgencyReason = computed(() => {
    const p = this.form.get('priority')?.value as PrPriority;
    return p === 'URGENT' || p === 'EMERGENCY';
  });

  readonly totalAmount = computed(() => {
    let total = 0;
    this.lineItems.controls.forEach((ctrl) => {
      const qty = parseFloat(ctrl.get('quantityAmount')?.value || '0');
      const price = parseFloat(ctrl.get('unitPriceAmount')?.value || '0');
      if (!isNaN(qty) && !isNaN(price)) total += qty * price;
    });
    return total.toFixed(4);
  });

  // ── Form ────────────────────────────────────────────────────────────
  readonly form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(500)]],
    justification: ['', [Validators.required, Validators.minLength(50)]],
    priority: ['NORMAL' as PrPriority, Validators.required],
    urgencyReason: [null as string | null],
    needByDate: [null as string | null],
    lineItems: this.fb.array([this.createLineItemGroup()])
  });

  get lineItems(): FormArray {
    return this.form.get('lineItems') as FormArray;
  }

  // ── Lifecycle ──────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadCategories();
  }

  // ── Line item helpers ──────────────────────────────────────────────
  createLineItemGroup(): FormGroup {
    return this.fb.group({
      itemCode: [null as string | null],
      itemName: ['', [Validators.required, Validators.maxLength(300)]],
      description: [null as string | null],
      categoryCode: ['', Validators.required],
      quantityAmount: ['1', Validators.required],
      quantityUnit: ['cái', Validators.required],
      unitPriceAmount: ['0', Validators.required],
      glAccountCode: ['', Validators.required],
      specifications: [null as string | null],
      isFromCatalog: [false]
    });
  }

  addLineItem(): void {
    this.lineItems.push(this.createLineItemGroup());
  }

  removeLineItem(index: number): void {
    if (this.lineItems.length > 1) {
      this.lineItems.removeAt(index);
    }
  }

  getLineItemError(index: number, field: string): string | null {
    const ctrl = this.lineItems.at(index).get(field);
    if (ctrl?.invalid && ctrl.touched) {
      if (ctrl.errors?.['required']) return 'validation.required';
      if (ctrl.errors?.['maxlength']) return 'validation.maxLength';
    }
    return null;
  }

  // ── Catalog picker ─────────────────────────────────────────────────
  openCatalogPicker(index: number): void {
    this.catalogPickerTargetIndex.set(index);
    this.showCatalogPicker.set(true);
    this.catalogSearchQuery.set('');
    this.catalogItems.set([]);
  }

  closeCatalogPicker(): void {
    this.showCatalogPicker.set(false);
    this.catalogPickerTargetIndex.set(null);
  }

  onCatalogSearch(q: string): void {
    this.catalogSearchQuery.set(q);
    if (q.length < 2) { this.catalogItems.set([]); return; }
    this.isCatalogLoading.set(true);
    this.catalogService.searchItems({ q, size: 20 })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isCatalogLoading.set(false)))
      .subscribe({ next: (res) => this.catalogItems.set(res.data ?? []) });
  }

  selectCatalogItem(item: CatalogItem): void {
    const idx = this.catalogPickerTargetIndex();
    if (idx === null) return;
    const ctrl = this.lineItems.at(idx);
    ctrl.patchValue({
      itemCode: item.itemCode,
      itemName: item.name,
      categoryCode: item.categoryCode,
      unitPriceAmount: item.unitPrice.amount,
      quantityUnit: item.unit,
      isFromCatalog: true
    });
    this.closeCatalogPicker();
  }

  // ── Attachment upload ──────────────────────────────────────────────
  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.isUploadingFile.set(true);
    this.prService.uploadAttachment(file)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isUploadingFile.set(false)))
      .subscribe({
        next: (res) => {
          this.uploadedAttachments.update((list) => [
            ...list,
            { id: res.data.id, fileName: res.data.fileName, fileSize: res.data.fileSize }
          ]);
        }
      });
    input.value = '';
  }

  removeAttachment(id: string): void {
    this.uploadedAttachments.update((list) => list.filter((a) => a.id !== id));
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // ── Submit ─────────────────────────────────────────────────────────
  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);

    const v = this.form.value;
    const lineItems: PrLineItemRequest[] = v.lineItems.map((li: any) => ({
      itemCode: li.itemCode || null,
      itemName: li.itemName,
      description: li.description || null,
      categoryCode: li.categoryCode,
      quantity: { amount: li.quantityAmount, unit: li.quantityUnit },
      unitPrice: { amount: li.unitPriceAmount, currency: 'VND' },
      glAccountCode: li.glAccountCode,
      specifications: li.specifications || null,
      isFromCatalog: li.isFromCatalog ?? false
    }));

    const request: CreatePrRequest = {
      title: v.title,
      justification: v.justification,
      priority: v.priority,
      urgencyReason: this.showUrgencyReason() ? v.urgencyReason : null,
      needByDate: v.needByDate || null,
      lineItems,
      attachmentIds: this.uploadedAttachments().map((a) => a.id)
    };

    this.prService.create(request)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (res) => this.router.navigate(['/procurement', res.data.id])
      });
  }

  onCancel(): void {
    this.router.navigate(['/procurement']);
  }

  // ── Private ────────────────────────────────────────────────────────
  private loadCategories(): void {
    this.catalogService.getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (res) => this.catalogCategories.set(res.data ?? []) });
  }

  // ── Field error helpers ────────────────────────────────────────────
  fieldError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl?.invalid || !ctrl.touched) return null;
    if (ctrl.errors?.['required']) return 'validation.required';
    if (ctrl.errors?.['minlength']) return 'validation.minLength';
    if (ctrl.errors?.['maxlength']) return 'validation.maxLength';
    return null;
  }
}
