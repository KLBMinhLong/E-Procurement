import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'ep-lang-switcher',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './ep-lang-switcher.component.html',
  styleUrl: './ep-lang-switcher.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpLangSwitcherComponent {
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly currentLang = signal(this.translateService.currentLang || this.translateService.getFallbackLang() || 'vi');

  constructor() {
    this.translateService.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.currentLang.set(event.lang));
  }

  useLanguage(lang: 'vi' | 'en'): void {
    this.translateService.use(lang);
  }
}
