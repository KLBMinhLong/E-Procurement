import {
  ChangeDetectionStrategy,
  Component,
  computed,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminRole, CreateRolePayload, UpdateRolePayload } from '../../../../models/admin.model';
import { EpModalComponent } from '../../../../../../shared/components/ep-modal/ep-modal.component';
import { EpFormFieldComponent } from '../../../../../../shared/components/ep-form-field/ep-form-field.component';
import { EpIconComponent } from '../../../../../../shared/components/ep-icon/ep-icon.component';
import { EpButtonComponent } from '../../../../../../shared/components/ep-button/ep-button.component';

@Component({
  selector: 'ep-admin-role-form-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TranslatePipe,
    EpModalComponent,
    EpFormFieldComponent,
    EpIconComponent,
    EpButtonComponent
  ],
  templateUrl: './role-form-modal.component.html',
  styleUrl: './role-form-modal.component.scss'
})
export class RoleFormModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() role: AdminRole | null = null;
  @Input() isSubmitting = false;

  @Output() closeForm = new EventEmitter<void>();
  @Output() submitForm = new EventEmitter<CreateRolePayload | UpdateRolePayload>();

  // ── Form State ─────────────────────────────────────────────────────
  readonly formCode = signal('');
  readonly formName = signal('');
  readonly formDescription = signal('');

  // ── Computeds ──────────────────────────────────────────────────────
  readonly formCodeError = computed(() => {
    const code = this.formCode();
    if (!code) return null;
    if (!/^[A-Z][A-Z0-9_]+$/.test(code)) return 'admin.roles.validation.codePattern';
    return null;
  });

  readonly isFormValid = computed(() => {
    const code = this.formCode().trim();
    const name = this.formName().trim();
    return code.length > 0 && name.length > 0 && !this.formCodeError();
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.resetForm();
    }
  }

  resetForm(): void {
    if (this.role) {
      this.formCode.set(this.role.code);
      this.formName.set(this.role.name);
      this.formDescription.set(this.role.description || '');
    } else {
      this.formCode.set('');
      this.formName.set('');
      this.formDescription.set('');
    }
  }

  onSubmit(): void {
    if (!this.isFormValid() || this.isSubmitting) return;

    if (this.role) {
      const payload: UpdateRolePayload = {
        code: this.formCode().trim().toUpperCase(),
        name: this.formName().trim(),
        description: this.formDescription().trim() || null
      };
      this.submitForm.emit(payload);
    } else {
      const payload: CreateRolePayload = {
        code: this.formCode().trim().toUpperCase(),
        name: this.formName().trim(),
        description: this.formDescription().trim() || null,
        permissions: []
      };
      this.submitForm.emit(payload);
    }
  }
}


