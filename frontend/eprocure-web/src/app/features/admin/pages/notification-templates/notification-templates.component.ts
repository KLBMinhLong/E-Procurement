import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { EpBadgeComponent } from '../../../../shared/components/ep-badge/ep-badge.component';
import { EpBreadcrumbComponent } from '../../../../shared/components/ep-breadcrumb/ep-breadcrumb.component';
import { EpButtonComponent } from '../../../../shared/components/ep-button/ep-button.component';
import { EpEmptyStateComponent } from '../../../../shared/components/ep-empty-state/ep-empty-state.component';
import { EpIconComponent } from '../../../../shared/components/ep-icon/ep-icon.component';
import { EpSkeletonComponent } from '../../../../shared/components/ep-skeleton/ep-skeleton.component';
import { EpStatCardComponent } from '../../../../shared/components/ep-stat-card/ep-stat-card.component';
import {
  NotificationTemplate,
  NotificationTemplateChannel,
  NotificationTemplatePreview
} from '../../models/admin.model';
import { AdminNotificationTemplateService } from '../../services/admin-notification-template.service';

type ActiveFilter = 'all' | 'active' | 'inactive';

@Component({
  selector: 'ep-admin-notification-templates',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    TranslatePipe,
    EpBadgeComponent,
    EpBreadcrumbComponent,
    EpButtonComponent,
    EpEmptyStateComponent,
    EpIconComponent,
    EpSkeletonComponent,
    EpStatCardComponent
  ],
  templateUrl: './notification-templates.component.html',
  styleUrl: './notification-templates.component.scss'
})
export class NotificationTemplatesComponent implements OnInit {
  private readonly templateService = inject(AdminNotificationTemplateService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly templates = signal<NotificationTemplate[]>([]);
  readonly selectedTemplate = signal<NotificationTemplate | null>(null);
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isPreviewing = signal(false);
  readonly searchQuery = signal('');
  readonly channelFilter = signal<NotificationTemplateChannel | 'all'>('all');
  readonly languageFilter = signal('all');
  readonly activeFilter = signal<ActiveFilter>('all');
  readonly draftSubject = signal('');
  readonly draftBody = signal('');
  readonly draftActive = signal(true);
  readonly sampleJson = signal(this.defaultSampleJson('EMAIL_SEND'));
  readonly preview = signal<NotificationTemplatePreview | null>(null);

  readonly channelOptions: Array<NotificationTemplateChannel | 'all'> = ['all', 'EMAIL', 'IN_APP', 'PUSH'];
  readonly activeOptions: ActiveFilter[] = ['all', 'active', 'inactive'];

  readonly languages = computed(() => {
    const values = new Set(this.templates().map(template => template.language));
    return Array.from(values).sort((a, b) => a.localeCompare(b));
  });

  readonly eventTypes = computed(() => {
    const values = new Set(this.templates().map(template => template.eventType));
    return Array.from(values).sort((a, b) => a.localeCompare(b));
  });

  readonly filteredTemplates = computed(() => {
    const query = this.normalizeQuery(this.searchQuery());
    const channel = this.channelFilter();
    const language = this.languageFilter();
    const active = this.activeFilter();

    return this.templates().filter(template => {
      if (channel !== 'all' && template.channel !== channel) {
        return false;
      }
      if (language !== 'all' && template.language !== language) {
        return false;
      }
      if (active === 'active' && !template.isActive) {
        return false;
      }
      if (active === 'inactive' && template.isActive) {
        return false;
      }
      if (!query) {
        return true;
      }
      return [
        template.code,
        template.eventType,
        template.channel,
        template.language,
        template.subjectTemplate,
        template.bodyTemplate
      ].filter(Boolean).join(' ').toLowerCase().includes(query);
    });
  });

  readonly totalTemplates = computed(() => this.templates().length);
  readonly activeTemplates = computed(() => this.templates().filter(template => template.isActive).length);
  readonly emailTemplates = computed(() => this.templates().filter(template => template.channel === 'EMAIL').length);
  readonly visibleTemplates = computed(() => this.filteredTemplates().length);

  readonly hasDirtyDraft = computed(() => {
    const template = this.selectedTemplate();
    if (!template) {
      return false;
    }
    return this.draftSubject() !== (template.subjectTemplate ?? '')
      || this.draftBody() !== template.bodyTemplate
      || this.draftActive() !== template.isActive;
  });

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.isLoading.set(true);
    this.templateService.listTemplates()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: response => {
          const templates = response.data ?? [];
          this.templates.set(templates);
          const currentCode = this.selectedTemplate()?.code;
          const nextSelected = templates.find(template => template.code === currentCode) ?? templates[0] ?? null;
          this.selectTemplate(nextSelected);
        },
        error: () => this.toastService.errorKey('admin.templates.toast.loadFailed')
      });
  }

  selectTemplate(template: NotificationTemplate | null): void {
    this.selectedTemplate.set(template);
    this.preview.set(null);
    this.draftSubject.set(template?.subjectTemplate ?? '');
    this.draftBody.set(template?.bodyTemplate ?? '');
    this.draftActive.set(template?.isActive ?? true);
    this.sampleJson.set(this.defaultSampleJson(template?.eventType ?? 'EMAIL_SEND'));
  }

  onSearchChange(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  saveTemplate(): void {
    const template = this.selectedTemplate();
    if (!template || this.isSaving()) {
      return;
    }
    if (!this.draftBody().trim()) {
      this.toastService.errorKey('admin.templates.toast.bodyRequired');
      return;
    }

    this.isSaving.set(true);
    this.templateService.updateTemplate(template.code, {
      subjectTemplate: this.draftSubject().trim() || null,
      bodyTemplate: this.draftBody().trim(),
      isActive: this.draftActive()
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isSaving.set(false))
    ).subscribe({
      next: response => {
        const updated = response.data;
        this.templates.update(items => items.map(item => item.code === updated.code ? updated : item));
        this.selectTemplate(updated);
        this.toastService.successKey('admin.templates.toast.updateSuccess');
      },
      error: () => this.toastService.errorKey('admin.templates.toast.updateFailed')
    });
  }

  previewTemplate(): void {
    const template = this.selectedTemplate();
    if (!template || this.isPreviewing()) {
      return;
    }

    const sampleData = this.parseSampleData();
    if (!sampleData) {
      return;
    }

    this.isPreviewing.set(true);
    this.templateService.previewTemplate(template.code, sampleData)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isPreviewing.set(false))
      )
      .subscribe({
        next: response => this.preview.set(response.data),
        error: () => this.toastService.errorKey('admin.templates.toast.previewFailed')
      });
  }

  resetDraft(): void {
    this.selectTemplate(this.selectedTemplate());
  }

  setChannelFilter(event: Event): void {
    this.channelFilter.set((event.target as HTMLSelectElement).value as NotificationTemplateChannel | 'all');
  }

  setLanguageFilter(event: Event): void {
    this.languageFilter.set((event.target as HTMLSelectElement).value);
  }

  setActiveFilter(event: Event): void {
    this.activeFilter.set((event.target as HTMLSelectElement).value as ActiveFilter);
  }

  setDraftSubject(event: Event): void {
    this.draftSubject.set((event.target as HTMLInputElement).value);
  }

  setDraftBody(event: Event): void {
    this.draftBody.set((event.target as HTMLTextAreaElement).value);
  }

  setDraftActive(event: Event): void {
    this.draftActive.set((event.target as HTMLInputElement).checked);
  }

  setSampleJson(event: Event): void {
    this.sampleJson.set((event.target as HTMLTextAreaElement).value);
  }

  trackByCode(_: number, template: NotificationTemplate): string {
    return template.code;
  }

  private parseSampleData(): Record<string, unknown> | null {
    try {
      const parsed = JSON.parse(this.sampleJson()) as unknown;
      if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
        this.toastService.errorKey('admin.templates.toast.invalidJson');
        return null;
      }
      return parsed as Record<string, unknown>;
    } catch {
      this.toastService.errorKey('admin.templates.toast.invalidJson');
      return null;
    }
  }

  private defaultSampleJson(eventType: string): string {
    const sample: Record<string, unknown> = {
      subject: 'Thông báo hệ thống',
      body: 'Nội dung thông báo mẫu',
      prNumber: 'PR-2026-06-00001',
      glAccountCode: '6002',
      resetUrl: 'https://app.eprocure.local/reset'
    };
    if (eventType === 'PASSWORD_RESET') {
      return JSON.stringify({ resetUrl: sample['resetUrl'] }, null, 2);
    }
    return JSON.stringify(sample, null, 2);
  }

  private normalizeQuery(value: string): string {
    return value.trim().toLowerCase();
  }
}
