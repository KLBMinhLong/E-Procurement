import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'ep-avatar',
  standalone: true,
  templateUrl: './ep-avatar.component.html',
  styleUrl: './ep-avatar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpAvatarComponent {
  readonly name = input.required<string>();
  readonly imageUrl = input<string | null>(null);

  readonly initials = computed(() => {
    const parts = this.name().trim().split(/\s+/);
    return parts.slice(0, 2).map((part) => part[0]?.toUpperCase()).join('');
  });
}
