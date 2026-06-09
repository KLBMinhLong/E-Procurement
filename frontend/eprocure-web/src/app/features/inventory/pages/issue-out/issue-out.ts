import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { StockService, IssueOutCommand } from '../../services/stock.service';
import { ToastService } from '../../../../core/services/toast.service';

import { EpCardComponent } from '../../../../shared/components/ep-card/ep-card.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpFormFieldComponent } from '../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';

@Component({
  selector: 'app-issue-out',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    TranslateModule,
    EpCardComponent,
    EpButtonComponent,
    EpFormFieldComponent,
    EpBreadcrumbComponent
  ],
  templateUrl: './issue-out.html',
  styleUrls: ['./issue-out.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IssueOut {
  private readonly stockService = inject(StockService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);

  readonly form: FormGroup = this.fb.group({
    warehouseId: ['', Validators.required],
    recipientId: ['', Validators.required],
    prId: [''],
    notes: [''],
    items: this.fb.array([
      this.createItemFormGroup()
    ])
  });

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  private createItemFormGroup(): FormGroup {
    return this.fb.group({
      itemCode: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(0.01)]],
      unit: ['EA', Validators.required]
    });
  }

  addItem(): void {
    this.items.push(this.createItemFormGroup());
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting() || this.items.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const formValue = this.form.getRawValue();
    const request: IssueOutCommand = {
      warehouseId: formValue.warehouseId,
      recipientId: formValue.recipientId,
      prId: formValue.prId || null,
      notes: formValue.notes || null,
      items: formValue.items.map((i: any) => ({
        itemCode: i.itemCode,
        quantity: i.quantity.toString(),
        unit: i.unit
      }))
    };

    const idempotencyKey = crypto.randomUUID();

    this.stockService.issueOutStock(request, idempotencyKey)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (res) => {
          this.toast.success('inventory.stock.issueSuccess');
          this.router.navigate(['/inventory/stock']);
        },
        error: (err) => {
          console.error('Failed to issue stock', err);
          this.toast.error('inventory.stock.issueFailed');
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/inventory/stock']);
  }
}
